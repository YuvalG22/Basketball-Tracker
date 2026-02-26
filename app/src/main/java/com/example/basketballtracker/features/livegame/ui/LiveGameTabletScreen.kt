package com.example.basketballtracker.features.livegame.ui

import com.example.basketballtracker.R
import android.annotation.SuppressLint
import android.inputmethodservice.Keyboard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class EventFilter { All, Score }

/**
 * Composes the live game tablet screen by subscribing to the view model UI and rendering the
 * scoreboard, players panel, play-by-play controls, and action buttons.
 *
 * Subscribes to vm.ui and computes derived state (scores, per-period team fouls, player box
 * stats, on-court/bench lists, and action enablement) which is passed to child panels.
 *
 * @param onEndGameNavigate Callback invoked to navigate away when the user confirms ending the game.
 */
@Composable
fun LiveGameTabletScreen(
    vm: LiveGameViewModel,
    onEndGameNavigate: () -> Unit
) {
    val s by vm.ui.collectAsState()
    val box = remember(s.events) { computeBoxByPlayer(s.events) }
    val teamScore = remember(s.events) { computeTeamScore(s.events) }
    val opponentScore = remember(s.events) { computeOppScore(s.events) }
    val playersById = remember(s.players) { s.players.associateBy { it.id } }

    val teamFoulsThisQ = remember(s.events, s.clock.period, s.gameId) {
        computeTeamFoulsThisPeriod(
            events = s.events,
            gameId = s.gameId,
            period = s.clock.period
        )
    }

    val selectedPf = remember(s.selectedPlayerId, box) {
        val id = s.selectedPlayerId ?: return@remember 0
        box[id]?.pf ?: 0
    }

    val actionsEnabled = s.selectedPlayerId != null && !s.isEnded && selectedPf < 5

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScoreBoardPanel(
                gameDateEpoch = s.gameDateEpoch,
                roundNumber = s.roundNumber,
                clock = s.clock,
                teamScore = teamScore,
                opponentName = s.opponentName,
                opponentScore = opponentScore,
                onToggleClock = vm::toggleClock,
                onNextQuarter = vm::nextQuarter,
                teamFouls = teamFoulsThisQ,
                modifier = Modifier
                    .weight(0.1f)
                    .fillMaxWidth()
            )
            Row(
                modifier = Modifier.weight(0.9f)
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
                        .weight(0.30f)
                        .fillMaxHeight()
                )

                Spacer(Modifier.width(6.dp))

                GameControlPanel(
                    opponentName = s.opponentName,
                    events = s.events,
                    playersById = playersById,
                    isEnded = s.isEnded,
                    onEndGame = onEndGameNavigate,
                    onUndo = vm::undoLast,
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                )

                Spacer(Modifier.width(6.dp))

                ActionsPanel(
                    enabled = actionsEnabled,
                    onEvent = vm::addEvent,
                    onUndo = vm::undoLast,
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Displays the "On Court" and "Bench" player panels with selection, substitution controls, and a player details dialog.
 *
 * Shows a scrollable list of on-court players and a horizontal list of bench players, each with selection and action buttons.
 * Long-pressing a player opens a details dialog showing that player's stats. Sub-in availability is computed from the
 * current number of on-court players.
 *
 * @param onCourtPlayers List of players currently on the court.
 * @param benchPlayers List of players on the bench.
 * @param selectedId Currently selected player id, or null when none is selected.
 * @param isEnded Whether the game has ended; used to disable certain bench actions.
 * @param box Mapping from player id to their accumulated box score (may be null for missing entries).
 * @param plusMinusById Mapping from player id to their plus/minus value; missing entries imply 0.
 * @param secondsPlayedById Mapping from player id to seconds played; used for sorting and display.
 * @param onSelect Callback invoked with a player id when a player is selected.
 * @param onSubIn Callback invoked with a bench player id to request substituting that player in.
 * @param onSubOut Callback invoked with an on-court player id to request substituting that player out.
 * @param modifier Modifier applied to the root container.
 */
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
                        //selectedId = selectedId,
                        onSelect = onSelect,
                        onSubOut = onSubOut,
                        onOpenSheet = { id -> sheetPlayerId = id }
                    )
                }
            }
            SectionDivider()
            Text("Bench", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val sortedBench = benchPlayers.sortedBy { secondsPlayedById[it.id] }.reversed()
                items(sortedBench, key = { it.id }) { p ->
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

/**
 * Displays a card of action controls for recording team and opponent events.
 *
 * The panel presents made/missed shot buttons, rebound and miscellaneous action buttons,
 * a personal foul button, and opponent scoring buttons. Button availability is governed
 * by the `enabled` flag.
 *
 * @param enabled When `true`, all action controls are interactive; when `false`, controls are disabled and a helper hint is shown.
 * @param onEvent Callback invoked with the selected `EventType` when any action button is pressed.
 * @param onUndo Callback intended to trigger an undo of the last action.
 * @param modifier Modifier applied to this panel.
 */
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MadeShotButton("2PT ✓", EventType.TWO_MADE, onEvent, enabled)
                    MadeShotButton("3PT ✓", EventType.THREE_MADE, onEvent, enabled)
                    MadeShotButton("FT ✓", EventType.FT_MADE, onEvent, enabled)
                    MadeShotButton("REB D", EventType.REB_DEF, onEvent, enabled)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MissedShotButton("2PT ✗", EventType.TWO_MISS, onEvent, enabled)
                    MissedShotButton("3PT ✗", EventType.THREE_MISS, onEvent, enabled)
                    MissedShotButton("FT ✗", EventType.FT_MISS, onEvent, enabled)
                    MissedShotButton("REB O", EventType.REB_OFF, onEvent, enabled)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton("AST", EventType.AST, onEvent, enabled)
                ActionButton("STL", EventType.STL, onEvent, enabled)
                ActionButton("BLK", EventType.BLK, onEvent, enabled)
                ActionButton("TOV", EventType.TOV, onEvent, enabled)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onEvent(EventType.PF) },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp)
            ) { Text("Personal Foul") }
            Spacer(Modifier.height(8.dp))
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

/**
 * Renders the play-by-play control panel with event filtering, undo, and end-game controls.
 *
 * Displays a filter for event types, an undo button, a scrollable list of filtered events,
 * and an end-game button which opens a confirmation dialog when pressed.
 *
 * @param opponentName The display name of the opposing team shown in play-by-play entries.
 * @param events The list of live events to display and filter.
 * @param playersById Map from player ID to PlayerEntity used to resolve player names in the list.
 * @param isEnded When true, disables the end-game action.
 * @param onEndGame Callback invoked when the user confirms ending the game.
 * @param onUndo Callback invoked when the user presses the undo button.
 * @param modifier Modifier applied to the panel container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
private fun GameControlPanel(
    opponentName: String,
    events: List<LiveEvent>,
    playersById: Map<Long, PlayerEntity>,
    isEnded: Boolean,
    onEndGame: () -> Unit,
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
            Text("Play By Play", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            var filter by rememberSaveable { mutableStateOf(EventFilter.All) }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayByPlayFilter(
                    selected = filter,
                    onSelected = { filter = it }
                )
                TextButton(
                    onClick = onUndo,
                ) { Text("UNDO", color = MaterialTheme.colorScheme.error) }
            }
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

/**
 * Renders the scoreboard panel with clock controls, team scores, fouls, and game metadata.
 *
 * Displays clock controls on the left, a centered score board showing team and opponent scores
 * and fouls, and game date plus round number on the right. The card background color
 * changes to indicate running (green) or low/paused time (red) states.
 *
 * @param gameDateEpoch Epoch milliseconds for the game date; if 0 the date text is empty.
 * @param roundNumber The current round number to display.
 * @param teamScore The home/team score to show.
 * @param opponentName The opponent team name to show.
 * @param opponentScore The opponent team's score to show.
 * @param clock Current game clock state used by the clock controls and display.
 * @param onToggleClock Callback invoked to toggle the clock running/paused state.
 * @param onNextQuarter Callback invoked to advance to the next quarter.
 * @param teamFouls Number of team fouls to display.
 * @param modifier Modifier to apply to the panel root.
 */
@Composable
fun ScoreBoardPanel(
    gameDateEpoch: Long,
    roundNumber: Int,
    teamScore: Int,
    opponentName: String,
    opponentScore: Int,
    clock: GameClock,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    teamFouls: Int,
    modifier: Modifier
) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(
            if (!clock.isRunning && clock.secRemaining < 600) Color.Red.copy(
                alpha = 0.12f
            ) else if (clock.isRunning) Color.Green.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                ClockControls(
                    clock = clock,
                    onToggleClock = onToggleClock,
                    onNextQuarter = onNextQuarter
                )
            }
            Box(
                modifier = Modifier.weight(4f),
                contentAlignment = Alignment.Center
            ) {
                ScoreBoard(
                    clock = clock,
                    teamScore = teamScore,
                    opponentName = opponentName,
                    opponentScore = opponentScore,
                    fouls = teamFouls
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dateText = remember(gameDateEpoch) {
                        if (gameDateEpoch == 0L) "" else
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(gameDateEpoch))
                    }
                    Text(
                        "$dateText",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Round $roundNumber",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/**
 * Renders the two-team scoreboard row containing the home team (labeled "AFEKA"), the game clock, and the opponent.
 *
 * @param clock The game clock state to display in the center.
 * @param teamScore The home team score to display on the left.
 * @param opponentName The opponent team name to display on the right.
 * @param opponentScore The opponent team score to display on the right.
 * @param fouls The current team foul count for the period to display for both teams.
 */
@Composable
fun ScoreBoard(
    clock: GameClock,
    teamScore: Int,
    opponentName: String,
    opponentScore: Int,
    fouls: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeftTeamScore(
            "AFEKA",
            teamScore,
            fouls = fouls
        )
        VerticalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
        ScoreBoardClock(clock)
        VerticalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        RightTeamScore(
            opponentName,
            opponentScore,
            fouls = fouls
        )
    }
}

/**
 * Displays the formatted remaining game time and the current period in a horizontal layout.
 *
 * Shows the remaining time as "M:SS" using a monospace, bold display style on the left and the period as `Qn` on the right.
 *
 * @param clock The GameClock containing `secRemaining` (seconds left) and `period` (current quarter). 
 */
@Composable
fun ScoreBoardClock(clock: GameClock) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            formatClock(clock.secRemaining),
            modifier = Modifier.width(140.dp),
            style = MaterialTheme.typography.displayMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            "Q${clock.period}",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Renders play/pause and "Next Q" controls for the game clock.
 *
 * @param clock Current game clock state used to determine the play/pause icon and button enablement.
 * @param onToggleClock Invoked when the play/pause control is pressed.
 * @param onNextQuarter Invoked to advance the game to the next quarter; the UI disables this action while the clock is running.
 */
@Composable
fun ClockControls(
    clock: GameClock,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleClock,
            Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (!clock.isRunning) Icons.Default.PlayCircleOutline else Icons.Default.PauseCircleOutline,
                contentDescription = "playPause",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        OutlinedButton(
            onClick = onNextQuarter,
            enabled = !clock.isRunning,
        ) { Text("Next Q") }
    }
}

/**
 * Displays the right-side team score block showing the team name, numeric score, and foul indicators.
 *
 * @param name The team display name.
 * @param score The team's current score.
 * @param fouls The team's current foul count rendered as foul dots.
 */
@Composable
fun RightTeamScore(name: String, score: Int, fouls: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$score",
            modifier = Modifier.width(105.dp),
            style = MaterialTheme.typography.displayLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(40.dp))
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            FoulDots(fouls = fouls)
        }
    }
}

/**
 * Renders the left/home team display with the team name, a visual foul indicator, and the team score.
 *
 * @param name The team name to show.
 * @param score The team's current score displayed prominently.
 * @param fouls The number of team fouls to render as foul dots.
@Composable
fun LeftTeamScore(name: String, score: Int, fouls: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            FoulDots(
                fouls = fouls,
            )
        }
        Spacer(modifier = Modifier.width(40.dp))
        Text(
            "$score",
            modifier = Modifier.width(105.dp),
            style = MaterialTheme.typography.displayLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Renders a vertically scrollable play-by-play list of events and auto-scrolls to the latest event.
 *
 * Displays the provided events in a fixed-height list (up to eight rows of 60dp each) and keeps the view scrolled to the newest item when the events list grows.
 *
 * @param events The chronological list of live events to display.
 * @param playersById Mapping of player IDs to PlayerEntity used to resolve player names/details for each event.
 * @param opponentName The opponent team name used for rendering opponent-related events.
 */
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
    val itemHeight = 60.dp
    val visibleItems = 8
    LazyColumn(
        state = listState,
        modifier = Modifier.height(itemHeight * visibleItems)
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

/**
 * Renders a single play-by-play entry row for the given event.
 *
 * Displays left-side event/player details, a center time/period/score column, and right-side opponent details.
 * If the event is a period start or end marker, renders a period marker instead.
 *
 * @param opponentName Display name of the opposing team shown for opponent-side entries.
 * @param event The live event to render.
 * @param playersById Map of player IDs to PlayerEntity used to resolve and format the player's name for display. 
 */
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

    if (event.type == EventType.PERIOD_START || event.type == EventType.PERIOD_END) {
        PeriodMarker(event)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(60.dp),
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
                oppScore = event.opponentScoreAtEvent,
                isOppEvent = isOppEvent
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

/**
 * Displays the current period and clock time, and optionally a centered score line.
 *
 * @param period The current period (quarter) number to display as "Q{period}".
 * @param time A formatted clock string (e.g., "1:23") shown next to the period.
 * @param isScoreEvent If true, renders a score line showing `teamScore - oppScore` beneath the period/time.
 * @param teamScore The team's score to display when `isScoreEvent` is true; may be null if unavailable.
 * @param oppScore The opponent's score to display when `isScoreEvent` is true; may be null if unavailable.
 * @param isOppEvent When true, visually de-emphasizes the team side and emphasizes the opponent side on the score line.
 */
@Composable
private fun PlayByPlayCenter(
    period: Int,
    time: String,
    isScoreEvent: Boolean,
    teamScore: Int?,
    oppScore: Int?,
    isOppEvent: Boolean
) {
    val fadedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Q$period $time",
            style = MaterialTheme.typography.bodyMedium,
            color = fadedColor,
            textAlign = TextAlign.Center
        )
        if (isScoreEvent) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$teamScore",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOppEvent) fadedColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    " - ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = fadedColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    "$oppScore",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOppEvent) MaterialTheme.colorScheme.onSurface else fadedColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Renders a horizontal row of foul indicator dots.
 *
 * @param fouls Number of fouls to mark; displayed count is capped at 5.
 * @param maxFouls Total number of dots to render.
 * @param dotSize Size of each dot.
 * @param spacing Horizontal spacing between dots.
 * @param activeColor Color used for active (filled) foul dots.
 * @param inactiveColor Color used for inactive (empty) foul dots.
 */
@Composable
fun FoulDots(
    fouls: Int,
    maxFouls: Int = 5,
    dotSize: Dp = 8.dp,
    spacing: Dp = 4.dp,
    activeColor: Color = Color.Red,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.3f)
) {
    val displayFouls = fouls.coerceAtMost(5)
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxFouls) { index ->
            Box(
                modifier = Modifier
                    .size(height = 4.dp, width = 12.dp)
                    .clip(RectangleShape)
                    .background(
                        if (index < displayFouls) activeColor else inactiveColor
                    )
            )
        }
    }
}

/**
 * Displays a player's detailed statistics in a vertical sheet layout.
 *
 * Shows a "Player not found" message when `player` is null. When `player` is present, renders minutes played, points, free throws, 2-pointers, 3-pointers, field goals, rebounds (total/defensive/offensive), assists, steals, blocks, turnovers, personal fouls, and plus/minus.
 *
 * @param player The player to display; if null a "Player not found" message is shown.
 * @param box The player's box score; null values are treated as zeros and default formatted strings.
 * @param secondsPlayed Total seconds the player has been on court (formatted as minutes).
 * @param plusMinus The player's plus/minus value (prefixed with `+` when positive).
 * @param modifier Modifier to be applied to the root composable.
 */
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
        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        StatRow("Points", pts.toString())
        StatRow("Free throws", ft)
        StatRow("2 pointers", twofg)
        StatRow("3 pointers", tp)
        StatRow("Field goals", fg)

        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        StatRow("Rebounds", reb.toString())
        StatRow("Defensive rebounds", dreb.toString())
        StatRow("Offensive rebounds", oreb.toString())

        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

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

/**
 * Displays an outlined card for an on-court player showing number, name, minutes, points, rebounds, assists and fouls, with a button to substitute the player out.
 *
 * The card is selectable via tap (invokes `onSelect`), supports a long‑press to open a player details sheet (invokes `onOpenSheet` and triggers haptic feedback), and exposes an "OUT" button that invokes `onSubOut`.
 *
 * @param player The player to render.
 * @param playerBoxScore Aggregated stats for the player; fields used: `pts`, `rebTotal`, `ast`, `pf`. If null, stats display as zero.
 * @param secondsByPlayer Seconds played for the player; used to format minutes display. If null, treated as zero.
 * @param isSelected Whether this player is currently selected (affects card border styling).
 * @param onSelect Callback invoked with the player's id when the card is tapped.
 * @param onSubOut Callback invoked with the player's id when the "OUT" button is pressed.
 * @param onOpenSheet Callback invoked with the player's id when the card is long‑pressed to open the details sheet.
 */
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
            color = if (isSelected) MaterialTheme.colorScheme.outline
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
                    verticalAlignment = Alignment.CenterVertically
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
                    onClick = {
                        onSubOut(player.id)
                    },
                ) {
                    Text(
                        "OUT"
                    )
                }
            }
        }
    }
}

/**
 * Displays a bench player card showing the player's number and formatted name, with an "IN" button
 * and click/long-press interactions.
 *
 * @param player The player entity to render.
 * @param isSelected True when the player is currently selected; affects the card border.
 * @param canSubIn True when substituting this player in is allowed.
 * @param isEnded True when the game has ended; disables the "IN" button when true.
 * @param onSelect Invoked with the player's id when the card is clicked.
 * @param onSubIn Invoked with the player's id when the "IN" button is pressed.
 * @param onOpenSheet Invoked with the player's id when the card is long-pressed (opens details sheet).
 */
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
            .height(90.dp)
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
            color = if (isSelected) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            Modifier
                .width(120.dp)
                .fillMaxHeight()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            Text(
                "#${player.number}\n${formatPlayerName(player.name)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = { onSubIn(player.id) },
                enabled = canSubIn && !isEnded,
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("IN")
            }
        }
    }
}

/**
 * Button that reports a missed-shot event when pressed.
 *
 * @param label Text to display inside the button.
 * @param type The `EventType` that will be sent to the callback when the button is pressed.
 * @param onEvent Callback invoked with `type` when the button is clicked.
 * @param enabled Whether the button is enabled and can be interacted with.
 */
@Composable
fun MissedShotButton(
    label: String,
    type: EventType,
    onEvent: (EventType) -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = { onEvent(type) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(0.dp, 50.dp, 50.dp, 0.dp)
    ) { Text(label) }
}

/**
 * Primary action button for recording a made shot event.
 *
 * @param label The text shown inside the button.
 * @param type The EventType that will be passed to the callback when the button is pressed.
 * @param onEvent Callback invoked with `type` when the button is clicked.
 * @param enabled Controls whether the button is interactive.
 */
@Composable
fun MadeShotButton(label: String, type: EventType, onEvent: (EventType) -> Unit, enabled: Boolean) {
    Button(
        onClick = { onEvent(type) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50.dp, 0.dp, 0.dp, 50.dp)
    ) { Text(label) }
}

/**
 * Renders a full-width action button inside a Row that dispatches the given event when clicked.
 *
 * @param label Text to show on the button.
 * @param type EventType passed to [onEvent] when the button is clicked.
 * @param onEvent Callback invoked with [type] on click.
 * @param enabled Whether the button is interactive.
 */
@Composable
fun RowScope.ActionButton(
    label: String,
    type: EventType,
    onEvent: (EventType) -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = { onEvent(type) },
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .height(56.dp),
        shape = RoundedCornerShape(50.dp)
    ) { Text(text = label) }
}

/**
 * Counts personal fouls (PF) recorded for the specified game and period.
 *
 * @param events List of live events to examine.
 * @param gameId Identifier of the game to filter events by.
 * @param period Period (quarter) number to filter events by.
 * @return The number of PF events matching the given game and period.
 */
fun computeTeamFoulsThisPeriod(
    events: List<LiveEvent>,
    gameId: Long,
    period: Int
): Int {
    return events.count {
        it.gameId == gameId &&
                it.type == EventType.PF &&
                it.period == period
    }
}

/**
 * Renders a two-option segmented control to choose the play-by-play event filter.
 *
 * Shows "All" and "Score" segments and invokes the callback when the user selects a segment.
 *
 * @param selected The currently selected EventFilter.
 * @param onSelected Callback invoked with the newly selected EventFilter.
 */
@Composable
fun PlayByPlayFilter(
    selected: EventFilter,
    onSelected: (EventFilter) -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = selected == EventFilter.All,
            onClick = { onSelected(EventFilter.All) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                activeContentColor = Color.White,
                activeBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                inactiveBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("All")
        }

        SegmentedButton(
            selected = selected == EventFilter.Score,
            onClick = { onSelected(EventFilter.Score) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                activeContentColor = Color.White,
                activeBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                inactiveBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("Score")
        }
    }
}

/**
 * Displays a centered period marker row like "START Qn" or "END Qn" between horizontal dividers.
 *
 * @param event A LiveEvent with type PERIOD_START or PERIOD_END; its `period` value is shown in the marker.
 */
@Composable
private fun PeriodMarker(event: LiveEvent) {
    val text = when (event.type) {
        EventType.PERIOD_START -> "START Q${event.period}"
        EventType.PERIOD_END -> "END Q${event.period}"
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp)
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp)
    }
}