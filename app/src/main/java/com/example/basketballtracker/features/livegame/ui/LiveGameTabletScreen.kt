package com.example.basketballtracker.features.livegame.ui

import com.example.basketballtracker.R
import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import kotlin.math.max

enum class EventFilter { All, Score }

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
                .padding(4.dp)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            PlayersPanel(
                onCourtPlayers = onCourtPlayers,
                benchPlayers = benchPlayers,
                selectedId = s.selectedPlayerId,
                isEnded = s.isEnded,
                box = box,
                plusMinusById = s.plusMinusById,
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
                    .weight(0.30f)
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
                events = s.events,
                playersById = playersById,
                isEnded = s.isEnded,
                onEndGame = onEndGameNavigate,
                onToggleClock = vm::toggleClock,
                onNextQuarter = vm::nextQuarter,
                onResetQuarter = vm::resetQuarter,
                onOpenSummary = onOpenSummary,
                modifier = Modifier
                    .weight(0.35f)
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
    plusMinusById: Map<Long, Int>,
    secondsPlayedById: Map<Long, Int>,
    onSelect: (Long) -> Unit,
    onSubIn: (Long) -> Unit,
    onSubOut: (Long) -> Unit,
    modifier: Modifier
) {
    val canSubIn = onCourtPlayers.size < 5

    var sheetPlayerId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (sheetPlayerId != null) {
        val pid = sheetPlayerId!!
        val p = (onCourtPlayers + benchPlayers).firstOrNull { it.id == pid }
        val b = box[pid]
        val pm = plusMinusById[pid] ?: 0
        val secPlayed = secondsPlayedById[pid] ?: 0

        AlertDialog(
            onDismissRequest = { sheetPlayerId = null },
            confirmButton = {
                TextButton(onClick = { sheetPlayerId = null }) {
                    Text("Close")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Text(
                    text = "#${p?.number} ${p?.name}",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Box(
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PlayerDetailsSheetContent(
                            player = p,
                            box = b,
                            plusMinus = pm,
                            secondsPlayed = secPlayed,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                "On Court",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(onCourtPlayers, key = { it.id }) { p ->
                    val isSelected = p.id == selectedId
                    OnCourtPlayerCard(
                        player = p,
                        playerBoxScore = box[p.id],
                        secondsByPlayer = secondsPlayedById[p.id],
                        isSelected = isSelected,
                        onSelect = onSelect,
                        onSubOut = onSubOut,
                        onOpenSheet = { id -> sheetPlayerId = id }
                    )
                }
            }
            SectionDivider()
            Text("Bench", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(benchPlayers, key = { it.id }) { p ->
                    val isSel = p.id == selectedId
                    BenchPlayerCard(
                        player = p,
                        isSelected = isSel,
                        canSubIn = canSubIn,
                        isEnded = isEnded,
                        onSelect = onSelect,
                        onSubIn = onSubIn,
                        onOpenSheet = { id -> sheetPlayerId = id }
                    )
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
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
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
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SectionDivider()
            Spacer(Modifier.weight(1f))
            Text("Opponent Actions")
            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.hgjhkj),
                contentDescription = "Clickable image example",
                modifier = Modifier
                    .padding(16.dp).fillMaxSize()
            )
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//                Button(
//                    onClick = { onEvent(EventType.OPP_TWO_MADE) },
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(56.dp)
//                ) { Text("2PT") }
//                Button(
//                    onClick = { onEvent(EventType.OPP_THREE_MADE) },
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(56.dp)
//                ) { Text("3PT") }
//                Button(
//                    onClick = { onEvent(EventType.OPP_FT_MADE) },
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(56.dp)
//                ) { Text("FT") }
//            }
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
    events: List<LiveEvent>,
    playersById: Map<Long, PlayerEntity>,
    isEnded: Boolean,
    onEndGame: () -> Unit,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    onResetQuarter: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Text("Game Info", style = MaterialTheme.typography.titleLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateText = remember(gameDateEpoch) {
                    if (gameDateEpoch == 0L) "" else
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(gameDateEpoch))
                }
                Text(
                    "$dateText • Round $roundNumber",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            SectionDivider()
            ScoreBoard(
                clock,
                teamScore,
                opponentName,
                opponentScore
            )
            Spacer(Modifier.height(8.dp))
            ClockControls(
                clock,
                onToggleClock,
                onNextQuarter,
                onResetQuarter
            )
            SectionDivider()
            Text("Game Events", style = MaterialTheme.typography.titleSmall)
            var filter by rememberSaveable { mutableStateOf(EventFilter.All) }
            var expanded by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(filter.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (!expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Options"
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    DropdownMenuItem(
                        text = { Text("All Events") },
                        onClick = {
                            filter = EventFilter.All
                            expanded = false
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DropdownMenuItem(
                        text = { Text("Score Events") },
                        onClick = {
                            filter = EventFilter.Score
                            expanded = false
                        }
                    )

                }
            }
            Spacer(Modifier.height(8.dp))
            val filteredEvents = remember(events, filter) {
                when (filter) {
                    EventFilter.All -> events
                    EventFilter.Score -> events.filter { it.type.isScoreEvent() }
                }
            }
            if (filteredEvents.isEmpty()) {
                Text("No events yet", style = MaterialTheme.typography.bodyLarge)
            } else {
                PlayByPlayList(
                    events = filteredEvents,
                    playersById = playersById,
                    opponentName = opponentName
                )
            }
            SectionDivider()
            Spacer(Modifier.weight(1f))
            var showEndDialog by remember { mutableStateOf(false) }
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
                                onEndGame()
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

@Composable
fun ScoreBoard(
    clock: GameClock,
    teamScore: Int,
    opponentName: String,
    opponentScore: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(3f), contentAlignment = Alignment.CenterStart
        ) {
            TeamScore(
                "AFEKA",
                teamScore
            )
        }
        Box(
            modifier = Modifier.weight(2f), contentAlignment = Alignment.Center
        ) {
            ScoreBoardClock(clock)
        }
        Box(
            modifier = Modifier.weight(3f), contentAlignment = Alignment.CenterEnd
        ) {
            TeamScore(
                opponentName,
                opponentScore
            )
        }
    }
}

@Composable
fun ScoreBoardClockSection(
    clock: GameClock,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    onResetQuarter: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ScoreBoardClock(clock)
        Spacer(Modifier.height(8.dp))
        ClockControls(
            clock = clock,
            onToggleClock = onToggleClock,
            onNextQuarter = onNextQuarter,
            onResetQuarter = onResetQuarter,
        )
    }
}

@Composable
fun ScoreBoardClock(clock: GameClock) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            formatClock(clock.secRemaining), style = MaterialTheme.typography.displaySmall
        )
        Text("Q${clock.period}", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ClockControls(
    clock: GameClock,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    onResetQuarter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onResetQuarter,
            enabled = !clock.isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("Reset Quarter") }
    }
}


@Composable
fun TeamScore(name: String, score: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$score",
            style = MaterialTheme.typography.displayMedium
        )
    }
}

@Composable
fun PlayByPlayList(
    events: List<LiveEvent>,
    playersById: Map<Long, PlayerEntity>,
    opponentName: String
) {
    val listState = rememberLazyListState()
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.scrollToItem(events.lastIndex)
        }
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.height(300.dp)
    ) {
        itemsIndexed(events) { index, e ->
            PlayByPlayItem(
                opponentName,
                e,
                playersById,
            )
            if (index < events.lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlayByPlayItem(
    opponentName: String,
    event: LiveEvent,
    playersById: Map<Long, PlayerEntity>,
) {
    val isOppEvent = event.type.isOpponentEvent()
    val isScoreEvent = event.type.isScoreEvent()
    val playerName = event.playerId?.let { id -> playersById[id]?.name }
    val formattedName = formatPlayerName(playerName)
    val time = formatClock(event.clockSecRemaining)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(50.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Box(Modifier.weight(2f), contentAlignment = Alignment.CenterStart) {
            PlayByPlaySide(
                visible = !isOppEvent,
                alignEnd = false,
                primary = formatEventPBP(event.type),
                secondary = formattedName,
                isScoreEvent = isScoreEvent,
                secondaryFaded = true
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PlayByPlayCenter(
                period = event.period,
                time = time,
                isScoreEvent = isScoreEvent,
                teamScore = event.teamScoreAtEvent,
                oppScore = event.opponentScoreAtEvent
            )
        }
        Box(Modifier.weight(2f), contentAlignment = Alignment.CenterEnd) {
            PlayByPlaySide(
                visible = isOppEvent,
                alignEnd = true,
                primary = opponentName,
                secondary = formatEventPBP(event.type),
                isScoreEvent = isScoreEvent,
                secondaryFaded = true
            )
        }
    }
}

@Composable
private fun PlayByPlaySide(
    visible: Boolean,
    alignEnd: Boolean,
    primary: String,
    secondary: String,
    isScoreEvent: Boolean,
    secondaryFaded: Boolean
) {
    if (!visible) return

    val weight = if (isScoreEvent) FontWeight.ExtraBold else FontWeight.Normal
    val fadedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alignEnd) {
            Text(
                primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = weight,
                color = if (secondaryFaded) fadedColor else LocalContentColor.current
            )
            Text(
                secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = weight
            )
        } else {
            Text(
                primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = weight
            )
            Text(
                secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = weight,
                color = if (secondaryFaded) fadedColor else LocalContentColor.current
            )
        }
    }
}

@Composable
private fun PlayByPlayCenter(
    period: Int,
    time: String,
    isScoreEvent: Boolean,
    teamScore: Int?,
    oppScore: Int?
) {
    val fadedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Q$period $time",
            style = MaterialTheme.typography.bodyLarge,
            color = fadedColor,
            textAlign = TextAlign.Center
        )
        if (isScoreEvent) {
            Text(
                "$teamScore-$oppScore",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = fadedColor,
                textAlign = TextAlign.Center
            )
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
    plusMinus: Int,
    modifier: Modifier = Modifier
) {
    val p = player
    if (p == null) {
        Text("Player not found", modifier = modifier)
        return
    }

    val pts = box?.pts ?: 0
    val reb = box?.rebTotal ?: 0
    val dreb = box?.rebDef ?: 0
    val oreb = box?.rebOff ?: 0
    val ast = box?.ast ?: 0
    val stl = box?.stl ?: 0
    val blk = box?.blk ?: 0
    val tov = box?.tov ?: 0
    val pf = box?.pf ?: 0

    val twofg = box?.let { "${it.twom}/${it.twoa} (${it.twoPct}%)" } ?: "0/0 (0%)"
    val fg = box?.let { "${it.fgm}/${it.fga} (${it.fgPct}%)" } ?: "0/0 (0%)"
    val tp = box?.let { "${it.threem}/${it.threea} (${it.threePct}%)" } ?: "0/0 (0%)"
    val ft = box?.let { "${it.ftm}/${it.fta} (${it.ftPct}%)" } ?: "0/0 (0%)"

    val plusMinusText = if (plusMinus > 0) "+$plusMinus" else plusMinus

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatRow("Minutes", formatMinutes(secondsPlayed))
        HorizontalDivider()

        StatRow("Points", pts.toString())
        StatRow("Free throws", ft)
        StatRow("2 pointers", twofg)
        StatRow("3 pointers", tp)
        StatRow("Field goals", fg)

        HorizontalDivider()

        StatRow("Rebounds", reb.toString())
        StatRow("Defensive rebounds", dreb.toString())
        StatRow("Offensive rebounds", oreb.toString())

        HorizontalDivider()

        StatRow("Assists", ast.toString())
        StatRow("Steals", stl.toString())
        StatRow("Blocks", blk.toString())
        StatRow("Turnovers", tov.toString())
        StatRow("Personal fouls", pf.toString())
        StatRow("+/-", plusMinusText.toString())
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontFamily = FontFamily.Monospace
        )
    }
}


fun formatPlayerName(fullName: String?): String {
    if (fullName.isNullOrBlank()) return ""

    val parts = fullName.trim().split(" ")
    if (parts.size < 2) return fullName

    val firstInitial = parts[0].first()
    val lastName = parts.last()

    return "$firstInitial. $lastName"
}

private fun formatClock(secRemaining: Int): String {
    val safe = max(0, secRemaining)
    val mm = safe / 60
    val ss = safe % 60
    return "%01d:%02d".format(mm, ss)
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        Modifier.padding(vertical = 8.dp),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
private fun OnCourtPlayerCard(
    player: PlayerEntity,
    playerBoxScore: PlayerBox?,
    secondsByPlayer: Int?,
    isSelected: Boolean,
    onSelect: (Long) -> Unit,
    onSubOut: (Long) -> Unit,
    onOpenSheet: (Long) -> Unit
) {
    val pts = playerBoxScore?.pts ?: 0
    val reb = playerBoxScore?.rebTotal ?: 0
    val ast = playerBoxScore?.ast ?: 0
    val pf = playerBoxScore?.pf ?: 0

    val hapticFeedback = LocalHapticFeedback.current

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onSelect(player.id) },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSheet(player.id)
                },

                ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
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
                Row(

                ) {
                    Text(
                        "#${player.number} ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(2.dp))
                val seconds = secondsByPlayer ?: 0
                val minText = formatMinutes(seconds)
                Text(
                    "MIN $minText  •  PTS $pts  •  REB $reb  •  AST $ast",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                FoulDots(fouls = pf)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = { onSubOut(player.id) },
                ) {
                    Text(
                        "OUT"
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchPlayerCard(
    player: PlayerEntity,
    isSelected: Boolean,
    canSubIn: Boolean,
    isEnded: Boolean,
    onSelect: (Long) -> Unit,
    onSubIn: (Long) -> Unit,
    onOpenSheet: (Long) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .combinedClickable(
                onClick = { onSelect(player.id) },
                onLongClick = {

                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSheet(player.id)
                }),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
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
                "#${player.number} ${player.name}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = { onSubIn(player.id) },
                enabled = canSubIn && !isEnded,
                contentPadding = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("IN")
            }
        }
    }
}