package com.example.basketballtracker.features.livegame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LiveUiState(
    val gameId: Long,
    val players: List<PlayerEntity>,
    val opponentName: String = "",
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
    gameId: Long,
    players: List<PlayerEntity>,
    private val quarterLengthSec: Int = 600
) : ViewModel() {

    private val _base = MutableStateFlow(
        LiveUiState(
            gameId = gameId,
            players = players,
            clock = GameClock(period = 1, secRemaining = quarterLengthSec)
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
            val pm = computePlusMinusByPlayer(events)

            base.copy(
                events = events,
                secondsPlayedById = seconds,
                plusMinusById = pm,
                opponentName = game?.opponentName ?: base.opponentName,
                roundNumber = game?.roundNumber ?: base.roundNumber,
                gameDateEpoch = game?.gameDateEpoch ?: base.gameDateEpoch,
//                teamScore = game?.teamScore ?: base.teamScore,
//                opponentScore = game?.opponentScore ?: base.opponentScore
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _base.value)


    init {
        viewModelScope.launch {
            val game = gamesRepo.getById(_base.value.gameId) ?: return@launch
            _base.update {
                it.copy(
                    opponentName = game.opponentName,
                    roundNumber = game.roundNumber,
                    gameDateEpoch = game.createdAt
                )
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(1000)

                var shouldEndQuarter = false

                _base.update { s ->
                    val c = s.clock
                    if (!c.isRunning) return@update s

                    if (c.secRemaining <= 1) {
                        shouldEndQuarter = true
                        return@update s.copy(
                            clock = c.copy(
                                isRunning = false,
                                secRemaining = 0
                            )
                        )
                    }

                    s.copy(clock = c.copy(secRemaining = c.secRemaining - 1))
                }
                if (shouldEndQuarter) {
                    endQuarterAuto()
                }
            }
        }
    }


    fun selectPlayer(id: Long) = _base.update { it.copy(selectedPlayerId = id) }
    fun toggleClock() =
        _base.update { it.copy(clock = it.clock.copy(isRunning = !it.clock.isRunning)) }

    fun resetQuarter() = _base.update {
        it.copy(clock = it.clock.copy(secRemaining = quarterLengthSec, isRunning = false))
    }

    fun nextQuarter() = _base.update { s ->
        val next = (s.clock.period + 1).coerceAtMost(4)
        s.copy(clock = GameClock(period = next, secRemaining = quarterLengthSec, isRunning = false))
    }

    fun addEvent(type: EventType, playerIdOverride: Long? = null) {
        val snap = _base.value
        val pid = playerIdOverride ?: snap.selectedPlayerId

        val currentEvents = ui.value.events
        val teamNow = computeTeamScore(currentEvents)
        val oppNow = computeOppScore(currentEvents)

        val (teamAtEvent, oppAtEvent) = when (type) {
            EventType.TWO_MADE -> teamNow + 2 to oppNow
            EventType.THREE_MADE -> teamNow + 3 to oppNow
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
                opponentScoreAtEvent = if (shouldAttachScore) oppAtEvent else null
            )
        }
    }

    fun subIn(playerId: Long) = addEvent(EventType.SUB_IN, playerIdOverride = playerId)
    fun subOut(playerId: Long) = addEvent(EventType.SUB_OUT, playerIdOverride = playerId)


    fun undoLast() {
        val gameId = _base.value.gameId
        viewModelScope.launch {
            repo.undoLastReturning(gameId) ?: return@launch
        }
    }

    fun endGame() {
        _base.update { it.copy(isEnded = true, clock = it.clock.copy(isRunning = false)) }

        val gameId = _base.value.gameId
        val events = ui.value.events

        val team = computeTeamScore(events)
        val opp = computeOppScore(events)

        viewModelScope.launch {
            gamesRepo.updateGameResult(
                gameId = gameId,
                teamScore = team,
                opponentScore = opp
            )
        }
    }

    private fun addEventAt(
        type: EventType,
        playerId: Long?,
        period: Int,
        clockSecRemaining: Int
    ) {
        val snap = _base.value
        viewModelScope.launch {
            repo.addEvent(
                gameId = snap.gameId,
                playerId = playerId,
                type = type,
                period = period,
                clockSecRemaining = clockSecRemaining
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
                clockSecRemaining = 0
            )
        }
    }


}
