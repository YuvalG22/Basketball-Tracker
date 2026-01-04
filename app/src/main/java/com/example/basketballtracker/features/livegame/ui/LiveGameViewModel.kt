import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.livegame.ui.EventType
import com.example.basketballtracker.features.livegame.ui.GameClock
import com.example.basketballtracker.features.livegame.ui.LiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveUiState(
    val gameId: Long,
    val players: List<Pair<Long, String>>,
    val selectedPlayerId: Long? = null,
    val clock: GameClock,
    val events: List<LiveEvent> = emptyList()
)

class LiveGameViewModel(
    gameId: Long,
    players: List<Pair<Long, String>>,
    private val quarterLengthSec: Int = 600
) : ViewModel() {

    private val _ui = MutableStateFlow(
        LiveUiState(
            gameId = gameId,
            players = players,
            clock = GameClock(period = 1, secRemaining = quarterLengthSec)
        )
    )
    val ui: StateFlow<LiveUiState> = _ui

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _ui.update { s ->
                    val c = s.clock
                    if (!c.isRunning) return@update s
                    if (c.secRemaining <= 0) return@update s.copy(clock = c.copy(isRunning = false, secRemaining = 0))
                    s.copy(clock = c.copy(secRemaining = c.secRemaining - 1))
                }
            }
        }
    }

    fun selectPlayer(id: Long) = _ui.update { it.copy(selectedPlayerId = id) }

    fun toggleClock() = _ui.update { it.copy(clock = it.clock.copy(isRunning = !it.clock.isRunning)) }

    fun resetQuarter() = _ui.update {
        it.copy(clock = it.clock.copy(secRemaining = quarterLengthSec, isRunning = false))
    }

    fun nextQuarter() = _ui.update { s ->
        val next = (s.clock.period + 1).coerceAtMost(4)
        s.copy(clock = GameClock(period = next, secRemaining = quarterLengthSec, isRunning = false))
    }

    fun addEvent(type: EventType) = _ui.update { s ->
        val pid = s.selectedPlayerId
        val e = LiveEvent(
            id = System.nanoTime(),
            gameId = s.gameId,
            playerId = pid,
            type = type,
            period = s.clock.period,
            clockSecRemaining = s.clock.secRemaining,
            createdAt = System.currentTimeMillis()
        )
        s.copy(events = s.events + e)
    }

    fun undoLast() = _ui.update { s ->
        if (s.events.isEmpty()) s else s.copy(events = s.events.dropLast(1))
    }
}
