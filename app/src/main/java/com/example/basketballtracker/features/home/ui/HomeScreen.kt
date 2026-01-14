package com.example.basketballtracker.features.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.ui.theme.Surface
import fixQuarterBoundarySubs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    gamesRepo: GamesRepository,
    onNewGame: () -> Unit,
    onContinue: (Long) -> Unit,
    onPlayers: () -> Unit,
    onHistory: () -> Unit
) {
    val lastGameId by gamesRepo.observeLastGameId().collectAsState(initial = null)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Basketball Tracker", style = MaterialTheme.typography.displayLarge)

            Button(
                onClick = onNewGame,
                modifier = Modifier.height(64.dp)
            ) { Text("New Game", style = MaterialTheme.typography.titleLarge) }

            OutlinedButton(
                onClick = { lastGameId?.let(onContinue) },
                enabled = lastGameId != null,
                modifier = Modifier.height(64.dp)
            ) { Text("Continue last game", style = MaterialTheme.typography.titleMedium) }

            OutlinedButton(
                onClick = { onPlayers() },
                modifier = Modifier.height(64.dp),
            ) { Text("Manage Roster", style = MaterialTheme.typography.titleMedium) }
             OutlinedButton(
                 onClick = { onHistory() },
                 modifier = Modifier.height(64.dp),
             ){ Text("Games History", style = MaterialTheme.typography.titleMedium) }
        }
    }
}