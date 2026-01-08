package com.example.basketballtracker.features.livegame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import kotlin.math.max

@Composable
fun LiveGameTabletScreen(vm: LiveGameViewModel) {
    val s by vm.ui.collectAsState()
    val box = remember(s.events) { computeBoxByPlayer(s.events) }
    val playersById = remember(s.players) { s.players.associateBy { it.id } }
    val last = s.events.lastOrNull()

    val onCourtIds = remember(s.events) { computeOnCourtIds(s.events) }

    val onCourtPlayers = remember(s.players, onCourtIds) {
        s.players.filter { it.id in onCourtIds }
    }
    val benchPlayers = remember(s.players, onCourtIds) {
        s.players.filter { it.id !in onCourtIds }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            PlayersPanel(
                onCourtPlayers = onCourtPlayers,
                benchPlayers = benchPlayers,
                selectedId = s.selectedPlayerId,
                box = box,
                secondsPlayedById = s.secondsPlayedById,
                onSelect = vm::selectPlayer,
                onSubIn = vm::subIn,
                onSubOut = vm::subOut,
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
            )

            Spacer(Modifier.width(12.dp))

            ActionsPanel(
                enabled = s.selectedPlayerId != null,
                onEvent = vm::addEvent,
                onUndo = vm::undoLast,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
            )

            Spacer(Modifier.width(12.dp))

            GameControlPanel(
                opponentName = s.opponentName,
                roundNumber = s.roundNumber,
                gameDateEpoch = s.gameDateEpoch,
                clock = s.clock,
                lastEvent = last,
                playersById = playersById,
                onToggleClock = vm::toggleClock,
                onNextQuarter = vm::nextQuarter,
                onResetQuarter = vm::resetQuarter,
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun PlayersPanel(
    onCourtPlayers: List<PlayerEntity>,
    benchPlayers: List<PlayerEntity>,
    selectedId: Long?,
    box: Map<Long, PlayerBox>,
    secondsPlayedById: Map<Long, Int>,
    onSelect: (Long) -> Unit,
    onSubIn: (Long) -> Unit,
    onSubOut: (Long) -> Unit,
    modifier: Modifier
) {
    val canSubIn = onCourtPlayers.size < 5

    Card(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Text("On Court (${onCourtPlayers.size}/5)", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(onCourtPlayers, key = { it.id }) { p ->
                    val b = box[p.id]
                    val pts = b?.pts ?: 0
                    val reb = b?.rebTotal ?: 0
                    val ast = b?.ast ?: 0
                    val tov = b?.tov ?: 0
                    val fg = b?.let { "${it.fgm}/${it.fga} (${it.fgPct}%)" } ?: "0/0 (0%)"
                    val tp = b?.let { "${it.threem}/${it.threea} (${it.threePct}%)" } ?: "0/0 (0%)"
                    val ft = b?.let { "${it.ftm}/${it.fta} (${it.ftPct}%)" } ?: "0/0 (0%)"

                    val isSel = p.id == selectedId

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(p.id) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    "#${p.number}  ${p.name}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                val secPlayed = secondsPlayedById[p.id] ?: 0
                                val minText = formatMinutes(secPlayed)

                                Text("MIN $minText • PTS $pts • REB $reb • AST $ast")
                                Spacer(Modifier.height(2.dp))
                            }
                            OutlinedButton(onClick = { onSubOut(p.id) }) { Text("OUT") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Bench (${benchPlayers.size})", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(benchPlayers, key = { it.id }) { p ->
                    val isSel = p.id == selectedId

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(p.id) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${p.number} ${p.name}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = { onSubIn(p.id) },
                                enabled = canSubIn
                            ) { Text("IN") }
                        }
                    }
                }
            }

            if (!canSubIn) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Only 5 players can be on court",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text("Actions", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            @Composable
            fun Btn(label: String, type: EventType) {
                Button(
                    onClick = { onEvent(type) },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("PF (Foul)") }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
    opponentName: String,
    roundNumber: Int,
    gameDateEpoch: Long,
    clock: GameClock,
    lastEvent: LiveEvent?,
    playersById: Map<Long, PlayerEntity>,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    onResetQuarter: () -> Unit,
    modifier: Modifier
) {
    Card(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text("Game Info", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            val dateText = remember(gameDateEpoch) {
                if (gameDateEpoch == 0L) "" else
                    java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(gameDateEpoch))
            }
            Text("$dateText", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text("Round $roundNumber", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text("Opponent: $opponentName", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(
                Modifier.padding(vertical = 10.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Text("Q${clock.period}", style = MaterialTheme.typography.titleLarge)
            val mm = max(0, clock.secRemaining) / 60
            val ss = max(0, clock.secRemaining) % 60
            Text(String.format("%02d:%02d", mm, ss), style = MaterialTheme.typography.displaySmall)

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onToggleClock,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text(if (clock.isRunning) "Pause" else "Start") }

                OutlinedButton(
                    onClick = onNextQuarter,
                    enabled = !clock.isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("Next Q") }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onResetQuarter,
                enabled = !clock.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("Reset Quarter") }

            Spacer(Modifier.height(16.dp))
            Text("Last action", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))

            val lastText = if (lastEvent == null) {
                "—"
            } else {
                val name = lastEvent.playerId?.let { id -> playersById[id]?.name } ?: "Team"
                val time = String.format(
                    "%02d:%02d",
                    lastEvent.clockSecRemaining / 60,
                    lastEvent.clockSecRemaining % 60
                )
                "Q${lastEvent.period} $time — $name ${formatEvent(lastEvent.type)}"
            }
            Text(lastText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}