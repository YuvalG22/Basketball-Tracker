package com.example.basketballtracker.features.livegame.ui.panels

import android.R.attr.alpha
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontFamily.Companion.Monospace
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.GameClock
import com.example.basketballtracker.features.livegame.domain.ShotMeta
import com.example.basketballtracker.features.livegame.ui.components.ActionButton
import com.example.basketballtracker.features.livegame.ui.components.FoulDots
import com.example.basketballtracker.ui.theme.inter
import com.example.basketballtracker.utils.formatClock
import com.example.basketballtracker.utils.periodLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Locale.getDefault

@Composable
fun ScoreBoardPanel(
    gameDateEpoch: Long,
    roundNumber: Int,
    teamScore: Int,
    opponentName: String,
    opponentScore: Int,
    isHomeGame: Boolean,
    clock: GameClock,
    onToggleClock: () -> Unit,
    onNextQuarter: () -> Unit,
    onEvent: (EventType, ShotMeta?) -> Unit,
    enabled: Boolean,
    isEnded: Boolean,
    onEndGame: () -> Unit,
    homeFouls: Int,
    awayFouls: Int,
    modifier: Modifier
) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(0.dp),
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
                modifier = Modifier.weight(2f),
                contentAlignment = Alignment.Center
            ) {
                ScoreBoard(
                    clock = clock,
                    teamScore = teamScore,
                    opponentName = opponentName,
                    opponentScore = opponentScore,
                    isHomeGame = isHomeGame,
                    homeFouls = homeFouls,
                    awayFouls = awayFouls,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "OPP\nSCORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier
                            .width(130.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            OppScoreButton("FT", EventType.OPP_FT_MADE, onEvent, enabled)
                            OppScoreButton("2PT", EventType.OPP_TWO_MADE, onEvent, enabled)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            OppScoreButton("3PT", EventType.OPP_THREE_MADE, onEvent, enabled)
                            OppScoreButton("PF", EventType.OPP_PF, onEvent, enabled)
                        }
                    }
                    VerticalDivider(
                        modifier = Modifier
                            .height(48.dp)
                            .padding(end = 8.dp, start = 8.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        val dateText = remember(gameDateEpoch) {
                            if (gameDateEpoch == 0L) "" else
                                SimpleDateFormat("E, MMM d, yyyy", Locale.ENGLISH)
                                    .format(Date(gameDateEpoch))
                        }
                        Text(
                            "Round $roundNumber",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            "$dateText",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    var showEndDialog by remember { mutableStateOf(false) }
                    Box() {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Scoreboard menu"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 8.dp,
                            shadowElevation = 12.dp,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("End Game", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    showEndDialog = true
                                },
                            )

                        }
                    }
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
    }
}

@Composable
fun LeftAnimatedScore(score: Int) {
    AnimatedContent(
        targetState = score,
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
        }
    ) { target ->
        Text(
            //modifier = Modifier.width(100.dp),
            text = target.toString(),
            style = MaterialTheme.typography.displayLarge,
            //fontFamily = Monospace,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun RightAnimatedScore(score: Int) {
    AnimatedContent(
        targetState = score,
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
        }
    ) { target ->
        Text(
            //modifier = Modifier.width(100.dp),
            text = target.toString(),
            style = MaterialTheme.typography.displayLarge,
            //fontFamily = Monospace,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun ScoreBoard(
    clock: GameClock,
    teamScore: Int,
    opponentName: String,
    opponentScore: Int,
    isHomeGame: Boolean,
    homeFouls: Int,
    awayFouls: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VerticalDivider(
            modifier = Modifier.height(48.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
        LeftTeamScore(
            if (isHomeGame) "AFEKA" else opponentName.uppercase(getDefault()),
            if (isHomeGame) teamScore else opponentScore,
            fouls = if (isHomeGame) homeFouls else awayFouls,
        )
        ScoreBoardClock(clock)
        RightTeamScore(
            if (isHomeGame) opponentName.uppercase(getDefault()) else "AFEKA",
            if (isHomeGame) opponentScore else teamScore,
            fouls = if (isHomeGame) awayFouls else homeFouls,
        )
        VerticalDivider(
            modifier = Modifier.height(48.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ScoreBoardClock(clock: GameClock) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            formatClock(clock.secRemaining),
            modifier = Modifier.width(85.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            color = if (!clock.isRunning && clock.secRemaining < 600) MaterialTheme.colorScheme.error
            else if (clock.isRunning) Color(0xFF3AB47A) else Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            periodLabel(clock.period),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

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
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(
            onClick = onNextQuarter,
            enabled = !clock.isRunning,
            border = BorderStroke(
                width = 2.dp,
                color = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                "Next Period",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun RowScope.RightTeamScore(name: String, score: Int, fouls: Int) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RightAnimatedScore(score)
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                "AWAY",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = inter,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = inter,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            FoulDots(fouls = fouls)
        }
    }
}

@Composable
fun RowScope.LeftTeamScore(name: String, score: Int, fouls: Int) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                "HOME",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = inter,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = inter,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            FoulDots(
                fouls = fouls,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        LeftAnimatedScore(score)
    }
}

@Composable
fun RowScope.OppScoreButton(
    label: String,
    type: EventType,
    onEvent: (EventType, ShotMeta?) -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = { onEvent(type, null) },
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .padding(0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}