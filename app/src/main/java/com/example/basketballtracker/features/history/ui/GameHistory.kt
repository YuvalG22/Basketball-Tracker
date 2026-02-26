package com.example.basketballtracker.features.history.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.basketballtracker.features.history.state.GamesHistoryViewModel
import java.nio.file.WatchEvent

@Composable
fun GamesHistoryScreen(
    vm: GamesHistoryViewModel,
    onGameClick: (Long) -> Unit
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
                TextButton(onClick = { deleteGameId = null }) { Text("Cancel") }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Game History", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(32.dp))
                LazyColumn(
                    reverseLayout = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(games, key = { it.id }) { game ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGameClick(game.id) },
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    val date = remember(game.createdAt) {
                                        java.text.SimpleDateFormat("dd/MM/yyyy")
                                            .format(java.util.Date(game.createdAt))
                                    }

                                    Text(
                                        "$date • Round ${game.roundNumber}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "vs ${game.opponentName}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val isWin = game.teamScore > game.opponentScore
                                    Text(
                                        modifier = Modifier.width(100.dp),
                                        textAlign = TextAlign.Center,
                                        text = "${game.teamScore} - ${game.opponentScore}",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        modifier = Modifier.width(60.dp),
                                        text = if (isWin) "W" else "L",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = if (isWin) Color.Green else Color.Red
                                    )
                                    VerticalDivider(
                                        Modifier
                                            .height(40.dp)
                                            .padding(horizontal = 4.dp),
                                        thickness = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
                                    )
                                    IconButton(
                                        onClick = {
                                            deleteGameId = game.id
                                            deleteGameTitle =
                                                "vs ${game.opponentName} (Round ${game.roundNumber})"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete game"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
