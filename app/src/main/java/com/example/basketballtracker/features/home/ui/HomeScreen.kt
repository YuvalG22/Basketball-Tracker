package com.example.basketballtracker.features.home.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.features.livegame.domain.computeOppScore
import com.example.basketballtracker.features.livegame.domain.computeTeamScore
import com.example.basketballtracker.ui.theme.inter
import com.example.basketballtracker.utils.formatClock
import com.example.basketballtracker.utils.formatDate

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewGame: () -> Unit,
    onContinue: (Long) -> Unit,
    onPlayers: () -> Unit,
    onPlayersStats: () -> Unit,
    onHistory: () -> Unit,
    onGameSummary: (Long) -> Unit
) {
    val lastGame by viewModel.lastGame.collectAsState()
    val lastGameEvents by viewModel.lastGameEvents.collectAsState()

    val teamScore = remember(lastGame, lastGameEvents) {
        if (lastGame?.status == GameStatus.LIVE) {
            computeTeamScore(lastGameEvents)
        } else {
            lastGame?.teamScore ?: 0
        }
    }

    val opponentScore = remember(lastGame, lastGameEvents) {
        if (lastGame?.status == GameStatus.LIVE) {
            computeOppScore(lastGameEvents)
        } else {
            lastGame?.opponentScore ?: 0
        }
    }

    val lastGameId = lastGame?.id
    val gameDateEpoch = lastGame?.gameDateEpoch ?: System.currentTimeMillis()
    val gameDate = formatDate(gameDateEpoch, "MMM d")
    val lastGameOpponentName = lastGame?.opponentName ?: "Unknown"
    val lastGameIsHomeGame = lastGame?.isHomeGame ?: false

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .padding(32.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🏀 Basketball Tracker", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3AB47A)),
                    shape = RoundedCornerShape(8.dp),
                    onClick = onNewGame,
                ) {
                    Text(
                        "NEW GAME",
                        fontFamily = inter,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3AB47A)),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { viewModel.manualSync() },
                    enabled = !viewModel.isSyncing
                ) {
                    if (viewModel.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Sync Now"
                        )
                    }
                }
            }
            Log.d("HomeScreen", "Rendering HomeScreen with lastGameId: $lastGameId")
            lastGame?.let { game ->
                LastGameCard(
                    opponentName = lastGameOpponentName,
                    isHomeGame = lastGameIsHomeGame,
                    teamScore = teamScore,
                    opponentScore = opponentScore,
                    status = game.status,
                    gameDate = gameDate,
                    isPlayoff = game.isPlayoff,
                    stage = game.playoffStage,
                    gameNumber = game.playoffGameNumber ?: game.roundNumber,
                    period = game.currentPeriod,
                    secRemaining = game.clockSecRemaining,
                    onClick = {
                        when (game.status) {
                            GameStatus.LIVE -> onContinue(game.id)
                            GameStatus.FINISHED -> onGameSummary(game.id)
                            else -> Unit
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {

                val isWideScreen = maxWidth > 700.dp

                if (isWideScreen) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        MenuCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onPlayers() },
                            icon = "👥",
                            title = "Roster",
                            secondary = "Manage your players"
                        )

                        MenuCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onPlayersStats() },
                            icon = "📊",
                            title = "Stats",
                            secondary = "Season averages and team leaders"
                        )

                        MenuCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onHistory() },
                            icon = "📅",
                            title = "History",
                            secondary = "View past games and scores"
                        )
                    }

                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MenuCard(
                            onClick = { onPlayersStats() },
                            icon = "📊",
                            title = "Stats",
                            secondary = "Season averages, leaders, and individual game logs"
                        )

                        MenuCard(
                            onClick = { onHistory() },
                            icon = "📅",
                            title = "History",
                            secondary = "View past games and scores"
                        )

                        MenuCard(
                            onClick = { onPlayers() },
                            icon = "👥",
                            title = "Roster",
                            secondary = "Manage your players"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LastGameScore(
    isHomeGame: Boolean,
    teamScore: Int,
    opponentScore: Int,
    isLive: Boolean = false,
    secRemaining: Int = 0,
    period: Int = 0,
) {
    val homeName = "AFEKA"
    val awayName = "OPP"

    val leftScore = if (isHomeGame) teamScore else opponentScore
    val rightScore = if (isHomeGame) opponentScore else teamScore

    val isTie = teamScore == opponentScore
    val teamWon = teamScore > opponentScore

    val leftIsWinner =
        !isLive && !isTie && ((isHomeGame && teamWon) || (!isHomeGame && !teamWon))

    val rightIsWinner =
        !isLive && !isTie && !leftIsWinner
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isLive) "Q$period • ${formatClock(secRemaining)}" else "",
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScoreBox(
                score = leftScore,
                isWinner = leftIsWinner,
                isLive = isLive
            )

            Text(
                text = "-",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White.copy(alpha = 0.35f),
                fontWeight = FontWeight.Bold
            )

            ScoreBox(
                score = rightScore,
                isWinner = rightIsWinner,
                isLive = isLive
            )
        }
    }
}

@Composable
private fun ScoreBox(
    score: Int,
    isWinner: Boolean,
    isLive: Boolean
) {
    val background =
        when {
            isLive -> Color(0xFF15261E)
            isWinner -> Color(0xFF2ECC71).copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.06f)
        }

    val border =
        when {
            isLive -> Color(0xFF2ECC71).copy(alpha = 0.55f)
            isWinner -> Color(0xFF2ECC71).copy(alpha = 0.75f)
            else -> Color.White.copy(alpha = 0.12f)
        }

    val textColor =
        when {
            isLive -> Color.White
            isWinner -> Color.White
            else -> Color.White.copy(alpha = 0.45f)
        }

    Box(
        modifier = Modifier
            .widthIn(min = 82.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = border,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = textColor,
            fontWeight = FontWeight.Black,
            fontFamily = inter
        )
    }
}

@Composable
fun MenuCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: String,
    title: String,
    secondary: String
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                icon,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                secondary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun LastGameCard(
    opponentName: String,
    isHomeGame: Boolean,
    teamScore: Int,
    opponentScore: Int,
    status: String,
    gameDate: String,
    isPlayoff: Boolean,
    stage: String?,
    gameNumber: Int,
    period: Int,
    secRemaining: Int,
    onClick: () -> Unit
) {
    val isLive = status == GameStatus.LIVE
    val isFinished = status == GameStatus.FINISHED

    val gameLabel =
        if (isPlayoff) {
            "${stageFormated(stage ?: "")} • Game $gameNumber"
        } else {
            "Round"
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF181818)
        ),
        border = BorderStroke(
            1.dp,
            if (isLive) Color(0xFF2ECC71).copy(alpha = 0.35f)
            else Color.White.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameStatusBadge(
                    isLive = isLive,
                    gameDate = gameDate,
                    info = gameLabel
                )

                Text(
                    text = if (isLive) "Tap to continue" else "Tap for summary",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }

            Text(
                text = if (isLive) {
                    "Q$period • ${formatClock(secRemaining)}"
                } else {
                    "FINAL SCORE"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (isLive) Color(0xFF2ECC71) else Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.Bold,
                fontFamily = inter
            )

            ScoreboardRows(
                opponentName = opponentName,
                isHomeGame = isHomeGame,
                teamScore = teamScore,
                opponentScore = opponentScore,
                isLive = isLive,
                isFinished = isFinished
            )
        }
    }
}

@Composable
private fun GameStatusBadge(
    isLive: Boolean,
    gameDate: String,
    info: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isLive) Color(0xFF2ECC71).copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.07f)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2ECC71))
            )
        }

        Text(
            text = if (isLive) "LIVE • $info" else "LAST GAME • $info",
            style = MaterialTheme.typography.labelLarge,
            color = if (isLive) Color(0xFF2ECC71) else Color.White.copy(alpha = 0.65f),
            fontWeight = FontWeight.Bold,
            fontFamily = inter
        )
    }
}

@Composable
private fun ScoreboardRows(
    opponentName: String,
    isHomeGame: Boolean,
    teamScore: Int,
    opponentScore: Int,
    isLive: Boolean,
    isFinished: Boolean
) {
    val firstTeamName = if (isHomeGame) "AFEKA" else opponentName
    val secondTeamName = if (isHomeGame) opponentName else "AFEKA"

    val firstScore = if (isHomeGame) teamScore else opponentScore
    val secondScore = if (isHomeGame) opponentScore else teamScore

    val firstIsAfeka = isHomeGame
    val secondIsAfeka = !isHomeGame

    val teamWon = teamScore > opponentScore
    val isTie = teamScore == opponentScore

    ScoreboardTeamRow(
        name = firstTeamName,
        score = firstScore,
        isAfeka = firstIsAfeka,
        isWinner = isFinished && !isTie && ((firstIsAfeka && teamWon) || (!firstIsAfeka && !teamWon)),
        isLive = isLive
    )

    ScoreboardTeamRow(
        name = secondTeamName,
        score = secondScore,
        isAfeka = secondIsAfeka,
        isWinner = isFinished && !isTie && ((secondIsAfeka && teamWon) || (!secondIsAfeka && !teamWon)),
        isLive = isLive
    )
}

@Composable
private fun ScoreboardTeamRow(
    name: String,
    score: Int,
    isAfeka: Boolean,
    isWinner: Boolean,
    isLive: Boolean
) {
    val alpha = when {
        isLive -> 1f
        isWinner -> 1f
        else -> 0.48f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isWinner) Color(0xFF2ECC71).copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                width = 1.dp,
                color = if (isWinner) Color(0xFF2ECC71).copy(alpha = 0.35f)
                else Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                fontFamily = inter
            )

            if (isAfeka) {
                Text(
                    text = "Your team",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2ECC71).copy(alpha = 0.85f),
                    fontFamily = inter
                )
            }
        }

        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White.copy(alpha = alpha),
            fontWeight = FontWeight.Black,
            fontFamily = inter
        )
    }
}

fun stageFormated(stage: String): String {
    return when (stage) {
        "FINAL" -> "Final"
        "SEMI_FINAL" -> "Semi Final"
        "QUARTER_FINAL" -> "Quarter Final"
        else -> "Round"
    }
}