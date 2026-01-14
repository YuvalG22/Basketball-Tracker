package com.example.basketballtracker.features.history.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.history.state.GamesHistoryViewModel

@Composable
fun GamesHistoryScreen(
    vm: GamesHistoryViewModel,
    onGameClick: (Long) -> Unit
) {
    val games by vm.games.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games, key = { it.id }) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGameClick(game.id) }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
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
                                modifier = Modifier.width(60.dp),
                                text = if (isWin) "W" else "L",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isWin) Color.Green else Color.Red
                            )
                            Text(
                                modifier = Modifier.width(100.dp),
                                textAlign = TextAlign.Center,
                                text = "${game.teamScore} - ${game.opponentScore}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
