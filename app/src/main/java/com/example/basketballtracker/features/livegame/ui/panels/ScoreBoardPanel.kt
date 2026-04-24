package com.example.basketballtracker.features.livegame.ui.panels

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.livegame.domain.GameClock
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
                    isHomeGame = isHomeGame,
                    homeFouls = homeFouls,
                    awayFouls = awayFouls,
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
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
            text = target.toString(),
            modifier = Modifier.width(105.dp),
            style = MaterialTheme.typography.displayLarge,
            fontFamily = inter,
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
            text = target.toString(),
            modifier = Modifier.width(105.dp),
            style = MaterialTheme.typography.displayLarge,
            fontFamily = inter,
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeftTeamScore(
            if (isHomeGame) "AFEKA" else opponentName.uppercase(getDefault()),
            teamScore,
            fouls = homeFouls
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
            if (isHomeGame) opponentName.uppercase(getDefault()) else "AFEKA",
            opponentScore,
            fouls = awayFouls
        )
    }
}

@Composable
fun ScoreBoardClock(clock: GameClock) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            formatClock(clock.secRemaining),
            modifier = Modifier.width(140.dp),
            style = MaterialTheme.typography.displaySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            color = if (!clock.isRunning && clock.secRemaining < 600) MaterialTheme.colorScheme.error
            else if (clock.isRunning) Color(0xFF3AB47A) else Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            periodLabel(clock.period),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
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
        Spacer(modifier = Modifier.width(6.dp))
        OutlinedButton(
            onClick = onNextQuarter,
            enabled = !clock.isRunning,
            border = BorderStroke(
                width = 2.dp,
                color = Color.White
            )
        ) { Text("Next Q") }
    }
}

@Composable
fun RightTeamScore(name: String, score: Int, fouls: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RightAnimatedScore(score)
        Spacer(modifier = Modifier.width(40.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "AWAY",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
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

@Composable
fun LeftTeamScore(name: String, score: Int, fouls: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "HOME",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
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
        LeftAnimatedScore(score)
    }
}