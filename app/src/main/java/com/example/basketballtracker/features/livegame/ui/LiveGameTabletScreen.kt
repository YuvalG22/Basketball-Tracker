import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.livegame.ui.PlayerBox
import com.example.basketballtracker.features.livegame.ui.computeBoxByPlayer
import com.example.basketballtracker.features.livegame.ui.EventType
import com.example.basketballtracker.features.livegame.ui.GameClock
import com.example.basketballtracker.features.livegame.ui.LiveEvent
import com.example.basketballtracker.features.livegame.ui.formatEvent
import kotlin.math.max
import kotlin.math.sign

@Composable
fun LiveGameTabletScreen(vm: LiveGameViewModel) {
    val s by vm.ui.collectAsState()
    val box = remember(s.events) { computeBoxByPlayer(s.events) }
    val playersById = remember(s.players) { s.players.toMap() }
    val last = s.events.lastOrNull()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxSize().padding(12.dp).windowInsetsPadding(WindowInsets.systemBars)) {
            PlayersPanel(
                players = s.players,
                selectedId = s.selectedPlayerId,
                box = box,
                onSelect = vm::selectPlayer,
                modifier = Modifier.weight(0.35f).fillMaxHeight()
            )

            Spacer(Modifier.width(12.dp))

            ActionsPanel(
                enabled = s.selectedPlayerId != null,
                onEvent = vm::addEvent,
                onUndo = vm::undoLast,
                modifier = Modifier.weight(0.45f).fillMaxHeight()
            )

            Spacer(Modifier.width(12.dp))

            GameControlPanel(
                clock = s.clock,
                lastEvent = last,
                playersById = playersById,
                onToggleClock = vm::toggleClock,
                onNextQuarter = vm::nextQuarter,
                onResetQuarter = vm::resetQuarter,
                modifier = Modifier.weight(0.30f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun PlayersPanel(
    players: List<Pair<Long, String>>,
    selectedId: Long?,
    box: Map<Long, PlayerBox>,
    onSelect: (Long) -> Unit,
    modifier: Modifier
) {
    Card(modifier) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("Players Box Score", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(players) { (id, name) ->
                    val b = box[id]
                    val pts = b?.pts ?: 0
                    val reb = b?.rebTotal ?: 0
                    val ast = b?.ast ?: 0
                    val tov = b?.tov ?: 0

                    val isSel = id == selectedId
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(id) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isSel)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("PTS $pts • REB $reb • AST $ast • TO $tov")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionsPanel(
    enabled: Boolean,
    onEvent: (EventType) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier
) {
    Card(modifier) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("Actions", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            @Composable
            fun Btn(label: String, type: EventType) {
                Button(
                    onClick = { onEvent(type) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text(label) }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn("2PT ✓", EventType.TWO_MADE)
                    Btn("3PT ✓", EventType.THREE_MADE)
                    Btn("FT ✓", EventType.FT_MADE)
                    Btn("REB D", EventType.REB_DEF)
                    Btn("AST", EventType.AST)
                    Btn("STL", EventType.STL)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn("2PT ✗", EventType.TWO_MISS)
                    Btn("3PT ✗", EventType.THREE_MISS)
                    Btn("FT ✗", EventType.FT_MISS)
                    Btn("REB O", EventType.REB_OFF)
                    Btn("TOV", EventType.TOV)
                    Btn("BLK", EventType.BLK)
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onEvent(EventType.PF) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("PF (Foul)") }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("UNDO") }

            if (!enabled) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Select a player to enable actions",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GameControlPanel(
    clock: GameClock,
    lastEvent: LiveEvent?,
    playersById: Map<Long, String>,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    onResetQuarter: () -> Unit,
    modifier: Modifier
) {
    Card(modifier) {
        Column(Modifier.fillMaxSize()
            .padding(12.dp)) {
            Text("Game Info", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Text("Q${clock.period}", style = MaterialTheme.typography.titleLarge)
            val mm = max(0, clock.secRemaining) / 60
            val ss = max(0, clock.secRemaining) % 60
            Text(String.format("%02d:%02d", mm, ss), style = MaterialTheme.typography.displaySmall)

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onToggleClock,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(if (clock.isRunning) "Pause" else "Start") }

                OutlinedButton(
                    onClick = onNextQuarter,
                    enabled = !clock.isRunning,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("Next Q") }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onResetQuarter,
                enabled = !clock.isRunning,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Reset Quarter") }

            Spacer(Modifier.height(16.dp))
            Text("Last action", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))

            val lastText = if (lastEvent == null) {
                "—"
            } else {
                val name = lastEvent.playerId?.let { playersById[it] } ?: "Team"
                val time = String.format("%02d:%02d", lastEvent.clockSecRemaining / 60, lastEvent.clockSecRemaining % 60)
                "Q${lastEvent.period} $time — $name ${formatEvent(lastEvent.type)}"
            }
            Text(lastText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}