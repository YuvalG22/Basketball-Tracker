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
    val selectedPlayerId: Long? = null,
    val clock: GameClock,
    val events: List<LiveEvent> = emptyList(),
    val secondsPlayedById: Map<Long, Int> = emptyMap()
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
        combine(_base, repo.observeLiveEvents(gameId)) { base, events ->
            base.copy(events = events)

            val seconds = computeSecondsPlayedByPlayer(
                events = events,
                quarterLengthSec = quarterLengthSec,
                currentPeriod = base.clock.period,
                currentClockSecRemaining = base.clock.secRemaining
            )

            base.copy(
                events = events,
                secondsPlayedById = seconds
            )

        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _base.value)

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _base.update { s ->
                    val c = s.clock
                    if (!c.isRunning) return@update s
                    if (c.secRemaining <= 0) return@update s.copy(clock = c.copy(isRunning = false, secRemaining = 0))
                    s.copy(clock = c.copy(secRemaining = c.secRemaining - 1))
                }
            }
        }

        viewModelScope.launch {
            val game = gamesRepo.getById(gameId) ?: return@launch
            _base.update { it.copy(opponentName = game.opponentName, roundNumber = game.roundNumber, gameDateEpoch = game.gameDateEpoch) }
        }
    }

    fun selectPlayer(id: Long) = _base.update { it.copy(selectedPlayerId = id) }
    fun toggleClock() = _base.update { it.copy(clock = it.clock.copy(isRunning = !it.clock.isRunning)) }

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

        viewModelScope.launch {
            repo.addEvent(
                gameId = snap.gameId,
                playerId = pid,
                type = type,
                period = snap.clock.period,
                clockSecRemaining = snap.clock.secRemaining
            )
        }
    }

    fun subIn(playerId: Long) = addEvent(EventType.SUB_IN, playerIdOverride = playerId)
    fun subOut(playerId: Long) = addEvent(EventType.SUB_OUT, playerIdOverride = playerId)


    fun undoLast() {
        val gameId = _base.value.gameId
        viewModelScope.launch { repo.undoLast(gameId) }
    }
}
