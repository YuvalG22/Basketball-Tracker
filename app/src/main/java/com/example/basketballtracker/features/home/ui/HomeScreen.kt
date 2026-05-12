package com.example.basketballtracker.features.home.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.example.basketballtracker.ui.theme.inter
import com.example.basketballtracker.utils.formatDate

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewGame: () -> Unit,
    onContinue: (Long) -> Unit,
    onPlayers: () -> Unit,
    onPlayersStats: () -> Unit,
    onHistory: () -> Unit,
) {
    val lastGame by viewModel.lastGame.collectAsState()

    val lastGameId = lastGame?.id
    val gameDateEpoch = lastGame?.gameDateEpoch ?: System.currentTimeMillis()
    val gameDate = formatDate(gameDateEpoch, "MMM d")
    val lastGameOpponentName = lastGame?.opponentName ?: "Unknown"
    val lastGameIsHomeGame = lastGame?.isHomeGame ?: false
    val lastGameTeamScore = lastGame?.teamScore
    val lastGameOppScore = lastGame?.opponentScore

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .padding(32.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🏀 Basketball Tracker", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3AB47A)),
                shape = RoundedCornerShape(8.dp),
                onClick = { viewModel.manualSync() },
                enabled = !viewModel.isSyncing
            ) {
                if (viewModel.isSyncing) {
                    // אנימציית טעינה קטנה במקום האייקון בזמן סנכרון
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
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3AB47A)),
                shape = RoundedCornerShape(8.dp),
                onClick = onNewGame,
            ) { Text(
                "NEW GAME",
                fontFamily = inter,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
            ) }
            Log.d("HomeScreen", "Rendering HomeScreen with lastGameId: $lastGameId")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContinue(lastGameId ?: return@clickable) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "LAST GAME • $gameDate",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF5F5F5F)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = if (lastGameIsHomeGame) "AFEKA" else lastGameOpponentName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        LastGameScore(
                            isHomeGame = lastGameIsHomeGame,
                            teamScore = lastGameTeamScore ?: 0,
                            opponentScore = lastGameOppScore ?: 0
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = if (lastGameIsHomeGame) lastGameOpponentName else "AFEKA",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MenuCard(
                    onClick = { onPlayers() },
                    icon = "👥",
                    title = "Roster",
                    secondary = "Manage your players"
                )
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
            }
        }
    }
}

@Composable
fun LastGameScore(
    isHomeGame: Boolean,
    teamScore: Int,
    opponentScore: Int
) {
    val isWin = teamScore > opponentScore
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isHomeGame) "$teamScore" else "$opponentScore",
            style = MaterialTheme.typography.displayMedium,
            color = if (isWin && isHomeGame) Color.White else Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Black,
            fontFamily = inter
        )
        Text(
            " - ",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            if (isHomeGame) "$opponentScore" else "$teamScore",
            style = MaterialTheme.typography.displayMedium,
            color = if (isWin && isHomeGame) Color.White.copy(alpha = 0.5f) else Color.White,
            fontWeight = FontWeight.Black,
            fontFamily = inter,
        )
    }
}

@Composable
fun RowScope.MenuCard(
    onClick: () -> Unit,
    icon: String,
    title: String,
    secondary: String
) {
    Card(
        modifier = Modifier
            .weight(1f)
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