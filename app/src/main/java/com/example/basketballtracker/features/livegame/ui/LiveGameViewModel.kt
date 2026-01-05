package com.example.basketballtracker.features.livegame.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.livegame.*
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.ui.EventType
import com.example.basketballtracker.features.livegame.ui.GameClock
import com.example.basketballtracker.features.livegame.ui.LiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LiveUiState(
    val gameId: Long,
    val players: List<Pair<Long, String>>,
    val selectedPlayerId: Long? = null,
    val clock: GameClock,
    val events: List<LiveEvent> = emptyList()
)

class LiveGameViewModel(
    private val repo: LiveGameRepository,
    gameId: Long,
    players: List<Pair<Long, String>>,
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

    fun addEvent(type: EventType) {
        val snap = _base.value
        viewModelScope.launch {
            repo.addEvent(
                gameId = snap.gameId,
                playerId = snap.selectedPlayerId,
                type = type,
                period = snap.clock.period,
                clockSecRemaining = snap.clock.secRemaining
            )
        }
    }

    fun undoLast() {
        val gameId = _base.value.gameId
        viewModelScope.launch { repo.undoLast(gameId) }
    }
}
