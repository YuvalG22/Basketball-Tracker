package com.example.basketballtracker.features.livegame.ui

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.privacysandbox.tools.core.model.Type
import androidx.room.util.TableInfo
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.math.max

@Composable
fun LiveGameTabletScreen(
    vm: LiveGameViewModel,
    onEndGameNavigate: () -> Unit,
    onOpenSummary: () -> Unit
) {
    val s by vm.ui.collectAsState()
    val box = remember(s.events) { computeBoxByPlayer(s.events) }
    val teamScore = remember(s.events) { computeTeamScore(s.events) }
    val opponentScore = remember(s.events) { computeOppScore(s.events) }
    val playersById = remember(s.players) { s.players.associateBy { it.id } }
    val last = s.events.takeLast(5)

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
                isEnded = s.isEnded,
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
                enabled = s.selectedPlayerId != null && !s.isEnded,
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
                teamScore = teamScore,
                opponentScore = opponentScore,
                clock = s.clock,
                lastEvents = last,
                playersById = playersById,
                isEnded = s.isEnded,
                onEndGame = onEndGameNavigate,
                onToggleClock = vm::toggleClock,
                onNextQuarter = vm::nextQuarter,
                onResetQuarter = vm::resetQuarter,
                onOpenSummary = onOpenSummary,
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayersPanel(
    onCourtPlayers: List<PlayerEntity>,
    benchPlayers: List<PlayerEntity>,
    selectedId: Long?,
    isEnded: Boolean,
    box: Map<Long, PlayerBox>,
    secondsPlayedById: Map<Long, Int>,
    onSelect: (Long) -> Unit,
    onSubIn: (Long) -> Unit,
    onSubOut: (Long) -> Unit,
    modifier: Modifier
) {
    val canSubIn = onCourtPlayers.size < 5

    // ---- BottomSheet state ----
    var sheetPlayerId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Render sheet if needed
    if (sheetPlayerId != null) {
        val pid = sheetPlayerId!!
        val p = (onCourtPlayers + benchPlayers).firstOrNull { it.id == pid }
        val b = box[pid]
        val secPlayed = secondsPlayedById[pid] ?: 0

        ModalBottomSheet(
            onDismissRequest = { sheetPlayerId = null },
            sheetState = sheetState
        ) {
            PlayerDetailsSheetContent(
                player = p,
                box = b,
                secondsPlayed = secPlayed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }

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
                    val pf = b?.pf ?: 0
                    val fg = b?.let { "${it.fgm}/${it.fga} (${it.fgPct}%)" } ?: "0/0 (0%)"
                    val tp = b?.let { "${it.threem}/${it.threea} (${it.threePct}%)" } ?: "0/0 (0%)"
                    val ft = b?.let { "${it.ftm}/${it.fta} (${it.ftPct}%)" } ?: "0/0 (0%)"

                    val isSel = p.id == selectedId

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(p.id) },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 4.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.padding(0.dp)) {
                                Text(
                                    "#${p.number}  ${p.name}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(2.dp))
                                val secPlayed = secondsPlayedById[p.id] ?: 0
                                val minText = formatMinutes(secPlayed)

                                Text("MIN $minText • PTS $pts • REB $reb • AST $ast")
                                Spacer(Modifier.height(4.dp))
                                FoulDots(fouls = pf)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedButton(onClick = { onSubOut(p.id) }, enabled = !isEnded) {
                                    Text(
                                        "OUT"
                                    )
                                }
//                                IconButton(
//                                    onClick = { sheetPlayerId = p.id }
//                                ) {
//                                    Icon(Icons.Default.Info, contentDescription = "Details")
//                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Bench (${benchPlayers.size})", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(benchPlayers, key = { it.id }) { p ->
                    val isSel = p.id == selectedId
                    val b = box[p.id]
//                    val pts = b?.pts ?: 0
//                    val reb = b?.rebTotal ?: 0
//                    val ast = b?.ast ?: 0
                    val secPlayed = secondsPlayedById[p.id] ?: 0
                    //val minText = formatMinutes(secPlayed)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clickable { onSelect(p.id) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                "#${p.number} ${p.name}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            FilledTonalButton(
                                onClick = { onSubIn(p.id) },
                                enabled = canSubIn && !isEnded,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("IN")
                            }
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
            Spacer(Modifier.height(10.dp))

            if (!enabled) {
                Text(
                    "Select a player to enable actions",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Spacer(Modifier.height(10.dp))
            Text("Opponent Actions")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onEvent(EventType.OPP_TWO_MADE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("2PT") }
                Button(
                    onClick = { onEvent(EventType.OPP_THREE_MADE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("3PT") }
                Button(
                    onClick = { onEvent(EventType.OPP_FT_MADE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("FT") }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun GameControlPanel(
    opponentName: String,
    roundNumber: Int,
    gameDateEpoch: Long,
    teamScore: Int,
    opponentScore: Int,
    clock: GameClock,
    lastEvents: List<LiveEvent>,
    playersById: Map<Long, PlayerEntity>,
    isEnded: Boolean,
    onEndGame: () -> Unit,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    onResetQuarter: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier
) {
    Card(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text("Game Info", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                val dateText = remember(gameDateEpoch) {
                    if (gameDateEpoch == 0L) "" else
                        java.text.SimpleDateFormat("dd/MM/yyyy")
                            .format(java.util.Date(gameDateEpoch))
                }
                Text("$dateText", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text("Round $roundNumber", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text("vs $opponentName", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "AFEKA",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "$teamScore",
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val mm = max(0, clock.secRemaining) / 60
                        val ss = max(0, clock.secRemaining) % 60
                        Text(
                            String.format("%02d:%02d", mm, ss),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text("Q${clock.period}", style = MaterialTheme.typography.titleLarge)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            opponentName.uppercase(getDefault()),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "$opponentScore",
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                }

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
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                Text("Last events", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                if (lastEvents.isEmpty()) {
                    Text("No events yet", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(items = lastEvents, key = { it.id }) { p ->
                            val isOppEvent = p.type.isOpponentEvent()
                            val name =
                                if (isOppEvent) opponentName else p.playerId?.let { id -> playersById[id]?.name }
                            val time = String.format(
                                "%02d:%02d",
                                p.clockSecRemaining / 60,
                                p.clockSecRemaining % 60
                            )
                            Text(
                                "Q${p.period} $time — $name ${formatEvent(p.type)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            var showEndDialog by remember { mutableStateOf(false) }
            Column() {
                OutlinedButton(
                    onClick = onOpenSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) { Text("Box Score") }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { showEndDialog = true },
                    enabled = !isEnded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                    ) { Text("End Game") }
                if (showEndDialog) {
                    AlertDialog(
                        onDismissRequest = { showEndDialog = false },
                        title = { Text("End game?") },
                        text = { Text("Are you sure you want to end the game? You won’t be able to continue recording events.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showEndDialog = false
                                    onEndGame() // או onEndGameNavigate()
                                }
                            ) { Text("End") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndDialog = false }) { Text("Cancel") }
                        }
                    )
                }

            }
        }
    }
}

@Composable
fun FoulDots(
    fouls: Int,
    dotSize: Dp = 8.dp,
    spacing: Dp = 4.dp,
    color: Color = Color.Red
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(fouls) { index ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun PlayerDetailsSheetContent(
    player: PlayerEntity?,
    box: PlayerBox?,
    secondsPlayed: Int,
    modifier: Modifier = Modifier
) {
    val p = player
    if (p == null) {
        Text("Player not found", modifier = modifier)
        return
    }

    val pts = box?.pts ?: 0
    val reb = box?.rebTotal ?: 0
    val ast = box?.ast ?: 0
    val stl = box?.stl ?: 0
    val blk = box?.blk ?: 0
    val tov = box?.tov ?: 0
    val pf = box?.pf ?: 0

    val fg = box?.let { "${it.fgm}/${it.fga} (${it.fgPct}%)" } ?: "0/0 (0%)"
    val tp = box?.let { "${it.threem}/${it.threea} (${it.threePct}%)" } ?: "0/0 (0%)"
    val ft = box?.let { "${it.ftm}/${it.fta} (${it.ftPct}%)" } ?: "0/0 (0%)"

    Column(modifier) {
        Text("#${p.number} ${p.name}", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(
            thickness = 2.dp
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val minText = formatMinutes(secondsPlayed)
                Text("MIN", style = MaterialTheme.typography.headlineMedium)
                Text(minText, style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PTS", style = MaterialTheme.typography.headlineMedium)
                Text("$pts", style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("REB", style = MaterialTheme.typography.headlineMedium)
                Text("$reb", style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AST", style = MaterialTheme.typography.headlineMedium)
                Text("$ast", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("STL", style = MaterialTheme.typography.headlineMedium)
                Text("$stl", style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("BLK", style = MaterialTheme.typography.headlineMedium)
                Text("$blk", style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TOV", style = MaterialTheme.typography.headlineMedium)
                Text("$tov", style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PF", style = MaterialTheme.typography.headlineMedium)
                Text("$pf", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            thickness = 2.dp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("FG", style = MaterialTheme.typography.headlineMedium)
                Text(fg, style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("3PT", style = MaterialTheme.typography.headlineMedium)
                Text(tp, style = MaterialTheme.typography.headlineMedium)
            }
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("FT", style = MaterialTheme.typography.headlineMedium)
                Text(ft, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}