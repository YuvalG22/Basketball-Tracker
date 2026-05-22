package com.example.basketballtracker.features.history.ui

import androidx.collection.mutableLongSetOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.features.history.state.GamesHistoryViewModel
import com.example.basketballtracker.features.summary.ui.SummaryTopBar
import java.nio.file.WatchEvent

private val HistoryAccent = Color(0xFF2ECC71)

@Composable
fun GamesHistoryScreen(
    vm: GamesHistoryViewModel,
    onGameClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val games by vm.games.collectAsState()

    var deleteGameId by remember { mutableStateOf<Long?>(null) }
    var deleteGameTitle by remember { mutableStateOf("") }

    if (deleteGameId != null) {
        AlertDialog(
            onDismissRequest = { deleteGameId = null },
            title = { Text("Delete game?") },
            text = { Text("Are you sure you want to delete $deleteGameTitle?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteGame(deleteGameId!!)
                        deleteGameId = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteGameId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SummaryTopBar(
                    "GAME HISTORY",
                    "Review previous games and box scores",
                    onBack
                )
            }

            if (games.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No games yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(games, key = { it.id }) { game ->
                    GameHistoryCard(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        onDeleteClick = {
                            deleteGameId = game.id
                            deleteGameTitle =
                                "vs ${game.opponentName} (Round ${game.roundNumber})"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GameHistoryCard(
    game: GameEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isHome = game.isHomeGame
    val isWin = game.teamScore > game.opponentScore

    val resultText = when {
        isWin -> "WIN"
        else -> "LOSS"
    }

    val date = remember(game.createdAt) {
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(game.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResultBadge(resultText)

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "ROUND ${game.roundNumber}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = date,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodySmall
                )

//                IconButton(onClick = onDeleteClick) {
//                    Icon(
//                        imageVector = Icons.Filled.Delete,
//                        contentDescription = "Delete game",
//                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//                    )
//                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamScoreBlock(
                    teamName = if (isHome) "Afeka" else game.opponentName,
                    score = if (isHome) game.teamScore else game.opponentScore,
                    isWinner = isWin && isHome || !isWin && !isHome,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "VS",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                TeamScoreBlock(
                    teamName = if (isHome) game.opponentName else "Afeka",
                    score = if (isHome) game.opponentScore else game.teamScore,
                    isWinner = !isWin && isHome || isWin && !isHome,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TeamScoreBlock(
    teamName: String,
    score: Int,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = teamName,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = score.toString(),
            color = if (isWinner) HistoryAccent else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black
        )

        Text(
            text = if (isWinner) "Winner" else "",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ResultBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (text == "WIN") HistoryAccent else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun ResultScoreBadge(text: String, teamScore: Int, opponentScore: Int) {
    Box(
        modifier = Modifier
            .background(
                if (text == "WIN") HistoryAccent else MaterialTheme.colorScheme.error,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$text $teamScore - $opponentScore",
            color = Color.Black,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun MiniGameInfo(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = value,
            color = HistoryAccent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black
        )
    }
}

fun marginText(teamScore: Int, opponentScore: Int): String {
    val diff = teamScore - opponentScore
    return when {
        diff > 0 -> "+$diff"
        diff < 0 -> diff.toString()
        else -> "0"
    }
}
