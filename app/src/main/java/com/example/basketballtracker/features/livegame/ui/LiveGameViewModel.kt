package com.example.basketballtracker.features.livegame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.core.data.db.SyncManager
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMadeShot(
    val type: EventType,
    val playerId: Long,
    val shotMeta: ShotMeta?
)

data class LiveUiState(
    val gameId: Long,
    val players: List<PlayerEntity>,
    val opponentName: String = "",
    val isHomeGame: Boolean = false,
    val roundNumber: Int = 0,
    val gameDateEpoch: Long = 0L,
    val plusMinusById: Map<Long, Int> = emptyMap(),
    val selectedPlayerId: Long? = null,
    val clock: GameClock,
    val events: List<LiveEvent> = emptyList(),
    val secondsPlayedById: Map<Long, Int> = emptyMap(),
    val isEnded: Boolean = false
)

class LiveGameViewModel(
    private val repo: LiveGameRepository,
    private val gamesRepo: GamesRepository,
    private val syncManager: SyncManager,
    gameId: Long,
    players: List<PlayerEntity>,
    private val quarterLengthSec: Int = 600
) : ViewModel() {

    private val _pendingMadeShot = MutableStateFlow<PendingMadeShot?>(null)
    val pendingMadeShot: StateFlow<PendingMadeShot?> = _pendingMadeShot.asStateFlow()

    private var lastLiveSyncAt = 0L

    private val _base = MutableStateFlow(
        LiveUiState(
            gameId = gameId,
            players = players,
            clock = GameClock(
                period = 1,
                secRemaining = quarterLengthSec,
                isRunning = false
            )
        )
    )

    val ui: StateFlow<LiveUiState> =
        combine(
            _base,
            repo.observeLiveEvents(gameId),
            gamesRepo.observeGame(gameId)
        ) { base, events, game ->

            val seconds = computeSecondsPlayedByPlayer(
                events = events,
                quarterLengthSec = quarterLengthSec,
                currentPeriod = base.clock.period,
                currentClockSecRemaining = base.clock.secRemaining
            )

            base.copy(
                events = events,
                secondsPlayedById = seconds,
                plusMinusById = computePlusMinusByPlayer(events),
                opponentName = game?.opponentName ?: base.opponentName,
                roundNumber = game?.roundNumber ?: base.roundNumber,
                gameDateEpoch = game?.gameDateEpoch ?: base.gameDateEpoch,
                isHomeGame = game?.isHomeGame ?: base.isHomeGame
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            _base.value
        )

    init {
        restoreLiveGameState()

        viewModelScope.launch {
            gamesRepo.updateGameStatus(_base.value.gameId, GameStatus.LIVE)
            syncManager.syncPending()
        }

        startClockLoop()
    }

    private fun restoreLiveGameState() {
        viewModelScope.launch {
            val game = gamesRepo.getById(_base.value.gameId) ?: return@launch

            val now = System.currentTimeMillis()

            val elapsedSeconds =
                if (game.isClockRunning && game.lastClockStartedAt != null) {
                    ((now - game.lastClockStartedAt) / 1000).toInt()
                } else {
                    0
                }

            val restoredSeconds =
                (game.clockSecRemaining - elapsedSeconds).coerceAtLeast(0)

            _base.update {
                it.copy(
                    opponentName = game.opponentName,
                    roundNumber = game.roundNumber,
                    gameDateEpoch = game.gameDateEpoch,
                    isHomeGame = game.isHomeGame,
                    clock = GameClock(
                        period = game.currentPeriod,
                        secRemaining = restoredSeconds,
                        isRunning = game.isClockRunning && restoredSeconds > 0
                    )
                )
            }

            persistClock()
        }
    }

    private fun startClockLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000)

                var shouldEndQuarter = false
                var shouldPersist = false

                _base.update { s ->
                    val c = s.clock

                    if (!c.isRunning) return@update s

                    shouldPersist = true

                    if (c.secRemaining <= 1) {
                        shouldEndQuarter = true

                        return@update s.copy(
                            clock = c.copy(
                                isRunning = false,
                                secRemaining = 0
                            )
                        )
                    }

                    s.copy(
                        clock = c.copy(
                            secRemaining = c.secRemaining - 1
                        )
                    )
                }

                if (shouldPersist) {
                    persistClock()
                }

                if (shouldEndQuarter) {
                    endQuarterAuto()
                }
            }
        }
    }

    private fun persistClock() {
        val snap = _base.value
        val clock = snap.clock

        viewModelScope.launch {
            gamesRepo.updateLiveState(
                gameId = snap.gameId,
                period = clock.period,
                secRemaining = clock.secRemaining,
                isRunning = clock.isRunning,
                lastStartedAt =
                    if (clock.isRunning) System.currentTimeMillis()
                    else null
            )
        }

        syncLiveStateThrottled()
    }

    fun selectPlayer(id: Long) {
        _base.update { it.copy(selectedPlayerId = id) }
    }

    fun toggleClock() {
        _base.update {
            it.copy(
                clock = it.clock.copy(
                    isRunning = !it.clock.isRunning
                )
            )
        }

        persistClock()
    }

    fun resetQuarter() {
        _base.update {
            it.copy(
                clock = it.clock.copy(
                    secRemaining = quarterLengthSec,
                    isRunning = false
                )
            )
        }

        persistClock()
    }

    fun nextQuarter() {
        addEvent(
            type = EventType.PERIOD_END,
            noPlayer = true,
            shotMeta = null
        )

        _base.update { s ->
            val next = s.clock.period + 1

            s.copy(
                clock = GameClock(
                    period = next,
                    secRemaining = quarterLengthSec,
                    isRunning = false
                )
            )
        }

        persistClock()

        addEvent(
            type = EventType.PERIOD_START,
            noPlayer = true,
            shotMeta = null
        )
    }

    fun addEvent(
        type: EventType,
        playerIdOverride: Long? = null,
        noPlayer: Boolean = false,
        shotMeta: ShotMeta? = null,
    ) {
        val snap = _base.value

        val pid: Long? = when {
            type.isOpponentEvent() -> null
            noPlayer -> null
            playerIdOverride != null -> playerIdOverride
            else -> snap.selectedPlayerId
        }

        if (!type.isOpponentEvent() && !noPlayer && type.requiresPlayer() && pid == null) {
            return
        }

        if ((type == EventType.TWO_MADE || type == EventType.THREE_MADE) && pid != null) {
            _pendingMadeShot.value = PendingMadeShot(
                type = type,
                playerId = pid,
                shotMeta = shotMeta
            )
            return
        }

        val currentEvents = ui.value.events
        val teamNow = computeTeamScore(currentEvents)
        val oppNow = computeOppScore(currentEvents)

        val (teamAtEvent, oppAtEvent) = when (type) {
            EventType.FT_MADE -> teamNow + 1 to oppNow
            EventType.OPP_TWO_MADE -> teamNow to oppNow + 2
            EventType.OPP_THREE_MADE -> teamNow to oppNow + 3
            EventType.OPP_FT_MADE -> teamNow to oppNow + 1
            else -> teamNow to oppNow
        }

        val shouldAttachScore = type.isScoreEvent() || type.isOpponentEvent()

        viewModelScope.launch {
            repo.addEvent(
                gameId = snap.gameId,
                playerId = pid,
                type = type,
                period = snap.clock.period,
                clockSecRemaining = snap.clock.secRemaining,
                teamScoreAtEvent = if (shouldAttachScore) teamAtEvent else null,
                opponentScoreAtEvent = if (shouldAttachScore) oppAtEvent else null,
                shotMeta = shotMeta,
                assistedByPlayerId = null
            )
        }
    }

    fun confirmMadeShot(assistedByPlayerId: Long?) {
        val pending = _pendingMadeShot.value ?: return
        val snap = _base.value

        val currentEvents = ui.value.events
        val teamNow = computeTeamScore(currentEvents)
        val oppNow = computeOppScore(currentEvents)

        val (teamAtEvent, oppAtEvent) = when (pending.type) {
            EventType.TWO_MADE -> teamNow + 2 to oppNow
            EventType.THREE_MADE -> teamNow + 3 to oppNow
            else -> teamNow to oppNow
        }

        viewModelScope.launch {
            repo.addEvent(
                gameId = snap.gameId,
                playerId = pending.playerId,
                type = pending.type,
                period = snap.clock.period,
                clockSecRemaining = snap.clock.secRemaining,
                teamScoreAtEvent = teamAtEvent,
                opponentScoreAtEvent = oppAtEvent,
                shotMeta = pending.shotMeta,
                assistedByPlayerId = assistedByPlayerId
            )

            if (assistedByPlayerId != null) {
                repo.addEvent(
                    gameId = snap.gameId,
                    playerId = assistedByPlayerId,
                    type = EventType.AST,
                    period = snap.clock.period,
                    clockSecRemaining = snap.clock.secRemaining,
                    shotMeta = null
                )
            }
        }

        _pendingMadeShot.value = null
    }

    fun dismissAssistPicker() {
        _pendingMadeShot.value = null
    }

    fun subIn(playerId: Long) {
        addEvent(EventType.SUB_IN, playerIdOverride = playerId, shotMeta = null)
    }

    fun subOut(playerId: Long) {
        addEvent(EventType.SUB_OUT, playerIdOverride = playerId, shotMeta = null)
    }

    fun undoLast() {
        val gameId = _base.value.gameId

        viewModelScope.launch {
            repo.undoLastReturning(gameId) ?: return@launch
        }
    }

    fun endGame() {
        _base.update {
            it.copy(
                isEnded = true,
                clock = it.clock.copy(isRunning = false)
            )
        }

        val gameId = _base.value.gameId
        val events = ui.value.events

        val team = computeTeamScore(events)
        val opp = computeOppScore(events)

        viewModelScope.launch {
            gamesRepo.finishGame(
                gameId = gameId,
                teamScore = team,
                opponentScore = opp
            )
            syncManager.syncPending()
        }
    }

    private fun addEventAt(
        type: EventType,
        playerId: Long?,
        period: Int,
        clockSecRemaining: Int,
        shotMeta: ShotMeta?
    ) {
        val snap = _base.value

        viewModelScope.launch {
            repo.addEvent(
                gameId = snap.gameId,
                playerId = playerId,
                type = type,
                period = period,
                clockSecRemaining = clockSecRemaining,
                shotMeta = shotMeta,
                assistedByPlayerId = null
            )
        }
    }

    private fun endQuarterAuto() {
        val snap = _base.value
        val events = ui.value.events
        val onCourt = computeOnCourtIds(events)
        val period = snap.clock.period

        onCourt.forEach { pid ->
            addEventAt(
                type = EventType.SUB_OUT,
                playerId = pid,
                period = period,
                clockSecRemaining = 0,
                shotMeta = null
            )
        }

        persistClock()
    }

    private fun syncLiveStateThrottled() {
        val now = System.currentTimeMillis()

        if (now - lastLiveSyncAt < 5_000) return

        lastLiveSyncAt = now

        viewModelScope.launch {
            syncManager.syncPending()
        }
    }
}