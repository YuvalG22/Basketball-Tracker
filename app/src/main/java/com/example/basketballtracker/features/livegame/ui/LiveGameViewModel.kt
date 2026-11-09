package com.example.basketballtracker.features.livegame.ui

import com.example.basketballtracker.utils.englishTeamName
import com.google.gson.Gson
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.core.data.db.SyncManager
import com.example.basketballtracker.core.data.db.SyncPendingChanges
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.domain.*
import com.example.basketballtracker.core.data.db.entities.EventEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMadeShot(
    val type: EventType,
    val playerId: Long,
    val shotMeta: ShotMeta?,
    val period: Int,
    val clockSecRemaining: Int,
    val eligibleIds: Set<Long>
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
    val quarterLengthSec: Int = 600
) : ViewModel() {

    private val _pendingMadeShot = MutableStateFlow<PendingMadeShot?>(null)
    val pendingMadeShot: StateFlow<PendingMadeShot?> = _pendingMadeShot.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private fun commit(block: suspend () -> Unit) {
        if (_saving.value || _base.value.isEnded) return
        _saving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                block()
                // Observe the transaction before accepting another action.
                val latest = repo.getLiveEventsOnce(_base.value.gameId)
                ui.first { it.events.map { e -> e.id }.toSet() == latest.map { e -> e.id }.toSet() }
                viewModelScope.launch { uploadSafely() }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { _error.value = "Could not save action. Please try again." }
            finally { _saving.value = false }
        }
    }
    private fun event(type: EventType, player: Long?, period: Int, seconds: Int) = EventEntity(
        gameId = _base.value.gameId, playerId = player, type = type.name,
        period = period, clockSecRemaining = seconds, createdAt = System.currentTimeMillis())

    private suspend fun uploadSafely() {
        try { syncManager.syncPending() }
        catch (e: CancellationException) { throw e }
        catch (_: SyncPendingChanges) { /* Next scheduled pass includes these writes. */ }
        catch (e: Exception) { _error.value = e.message ?: "Saved locally. Cloud sync will retry." }
    }
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
                opponentName = game?.let { englishTeamName(it.opponentName) } ?: base.opponentName,
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
            if (gamesRepo.getById(_base.value.gameId)?.status != GameStatus.FINISHED)
                gamesRepo.updateGameStatus(_base.value.gameId, GameStatus.LIVE)
            uploadSafely()
        }

        observeRemoteGame()
        startClockLoop()
    }

    private fun restoreLiveGameState() {
        viewModelScope.launch {
            val game = gamesRepo.getById(_base.value.gameId) ?: return@launch

            val now = System.currentTimeMillis()

            val elapsedSeconds =
                if (game.isClockRunning && game.lastClockStartedAt != null) {
                    ((now - game.lastClockStartedAt) / 1000).coerceAtLeast(0).toInt()
                } else {
                    0
                }

            val restoredSeconds =
                (game.clockSecRemaining - elapsedSeconds).coerceAtLeast(0)

            _base.update {
                it.copy(
                    isEnded = game.status == GameStatus.FINISHED,
                    opponentName = englishTeamName(game.opponentName),
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

            if (game.status != GameStatus.FINISHED) {
                repo.getDraft(game.id)?.let { payload ->
                    _pendingMadeShot.value = Gson().fromJson(payload, PendingMadeShot::class.java)
                }
                persistClock()
            }
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
            syncLiveStateThrottled()
        }

    }

    fun updatePlayers(players: List<PlayerEntity>) {
        _base.update { it.copy(players = players) }
    }

    private fun observeRemoteGame() {
        viewModelScope.launch {
            var lastVersion = -1L
            gamesRepo.observeGame(_base.value.gameId).filterNotNull().collect { game ->
                if (game.isDeleted) {
                    _base.update { it.copy(isEnded = true, clock = it.clock.copy(isRunning = false)) }
                    _error.value = "This game was deleted on another device. Local actions are kept."
                } else if (game.syncStatus == "SYNCED" && game.serverVersion > lastVersion) {
                    lastVersion = game.serverVersion
                    val elapsed = if (game.isClockRunning && game.lastClockStartedAt != null)
                        ((System.currentTimeMillis() - game.lastClockStartedAt) / 1000).coerceAtLeast(0).toInt() else 0
                    val seconds = (game.clockSecRemaining - elapsed).coerceAtLeast(0)
                    _base.update { it.copy(isEnded = game.status == GameStatus.FINISHED,
                        clock = GameClock(game.currentPeriod, seconds, game.isClockRunning && seconds > 0)) }
                }
            }
        }
    }

    fun selectPlayer(id: Long) {
        _base.update { it.copy(selectedPlayerId = id) }
    }

    fun toggleClock() {
        if (_base.value.isEnded || _saving.value || _pendingMadeShot.value != null) return
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
        if (_pendingMadeShot.value != null || _base.value.clock.isRunning) return
        commit {
            val old = _base.value.clock
            repo.recordAction(listOf(event(EventType.PERIOD_END, null, old.period, old.secRemaining),
                event(EventType.PERIOD_START, null, old.period + 1, quarterLengthSec)))
            _base.update { it.copy(clock = GameClock(old.period + 1, quarterLengthSec, false)) }
            persistClock()
        }
    }

    fun addEvent(
        type: EventType,
        playerIdOverride: Long? = null,
        noPlayer: Boolean = false,
        shotMeta: ShotMeta? = null,
    ) {
        val snap = _base.value
        if (snap.isEnded || _saving.value || _pendingMadeShot.value != null) return

        val pid: Long? = when {
            type.isOpponentEvent() -> null
            noPlayer -> null
            playerIdOverride != null -> playerIdOverride
            else -> snap.selectedPlayerId
        }

        if (!type.isOpponentEvent() && !noPlayer && type.requiresPlayer() && pid == null) {
            return
        }

        if (type.requiresPlayer() && pid != null && ui.value.events.count { it.playerId == pid && it.type == EventType.PF } >= 5) return
        if ((type == EventType.TWO_MADE || type == EventType.THREE_MADE) && pid != null) {
            val draft = PendingMadeShot(
                type = type,
                playerId = pid,
                shotMeta = shotMeta,
                period = snap.clock.period,
                clockSecRemaining = snap.clock.secRemaining,
                eligibleIds = computeOnCourtIds(ui.value.events) - pid
            )
            _saving.value = true
            viewModelScope.launch {
                try {
                    repo.saveDraft(snap.gameId, Gson().toJson(draft))
                    _pendingMadeShot.value = draft
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { _error.value = "Could not save the shot. Please try again." }
                finally { _saving.value = false }
            }
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

        commit {
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
        if (assistedByPlayerId != null && assistedByPlayerId !in pending.eligibleIds) return
        val shot = event(pending.type, pending.playerId, pending.period, pending.clockSecRemaining).copy(
            assistedByPlayerId = assistedByPlayerId,
            teamScoreAtEvent = computeTeamScore(ui.value.events) + if (pending.type == EventType.THREE_MADE) 3 else 2,
            opponentScoreAtEvent = computeOppScore(ui.value.events),
            shotX = pending.shotMeta?.x, shotY = pending.shotMeta?.y, shotDistance = pending.shotMeta?.distance)
        commit {
            repo.recordAction(listOfNotNull(shot, assistedByPlayerId?.let {
                event(EventType.AST, it, pending.period, pending.clockSecRemaining)
            }))
            _pendingMadeShot.value = null
        }
    }

    fun dismissAssistPicker() {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try { repo.clearDraft(_base.value.gameId); _pendingMadeShot.value = null; _error.value = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = "Could not cancel the shot. Please try again." }
            finally { _saving.value = false }
        }
    }

    fun substitute(outgoing: Long?, incoming: Long?) {
        if (_pendingMadeShot.value != null) return
        commit {
            val events = repo.getLiveEventsOnce(_base.value.gameId)
            val court = computeOnCourtIds(events)
            require(outgoing == null || outgoing in court)
            require(incoming == null || (incoming !in court && _base.value.players.any { it.id == incoming } &&
                events.count { it.playerId == incoming && it.type == EventType.PF } < 5))
            require(court.size - (if (outgoing == null) 0 else 1) + (if (incoming == null) 0 else 1) <= 5)
            val clock = _base.value.clock
            repo.recordAction(listOfNotNull(
                outgoing?.let { event(EventType.SUB_OUT, it, clock.period, clock.secRemaining) },
                incoming?.let { event(EventType.SUB_IN, it, clock.period, clock.secRemaining) }))
            _base.update { it.copy(selectedPlayerId = incoming) }
        }
    }
    fun subIn(playerId: Long) = substitute(null, playerId)
    fun subOut(playerId: Long) = substitute(playerId, null)

    fun undoLast() {
        if (_pendingMadeShot.value != null) return
        commit { repo.undoLastReturning(_base.value.gameId) }
    }

    fun adjustClock(deltaSeconds: Int) {
        if (_base.value.isEnded || _saving.value || _pendingMadeShot.value != null) return
        _base.update { s ->
            val newSeconds = (s.clock.secRemaining + deltaSeconds)
                .coerceIn(0, quarterLengthSec)

            s.copy(
                clock = s.clock.copy(
                    secRemaining = newSeconds
                )
            )
        }

        persistClock()
    }

    fun adjustPeriod(delta: Int) {
        if (_base.value.isEnded || _saving.value || _pendingMadeShot.value != null) return
        _base.update { s ->
            val newPeriod = (s.clock.period + delta)
                .coerceIn(1, 10)

            s.copy(
                clock = s.clock.copy(
                    period = newPeriod
                )
            )
        }

        persistClock()
    }

    fun endGame(onSaved: () -> Unit = {}) {
        if (_base.value.isEnded || _saving.value || _pendingMadeShot.value != null) return
        _saving.value = true
        _base.update { it.copy(clock = it.clock.copy(isRunning = false)) }
        val snap = _base.value
        viewModelScope.launch {
            try {
                val events = repo.getLiveEventsOnce(snap.gameId)
                gamesRepo.finishGame(snap.gameId, computeTeamScore(events), computeOppScore(events),
                    snap.clock.period, snap.clock.secRemaining)
                _base.update { it.copy(isEnded = true) }
                onSaved()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = "Could not finish the game. Saved actions are kept. Try again." }
            finally { _saving.value = false }
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

        viewModelScope.launch {
            try {
                repo.recordAction(onCourt.map { event(EventType.SUB_OUT, it, period, 0) })
                uploadSafely()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = "Could not save period-end substitutions. Please review the game." }
        }

        persistClock()
    }

    private fun syncLiveStateThrottled() {
        val now = System.currentTimeMillis()

        if (now - lastLiveSyncAt < 5_000) return

        lastLiveSyncAt = now

        viewModelScope.launch {
            uploadSafely()
        }
    }
}
