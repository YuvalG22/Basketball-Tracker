package com.example.basketballtracker.features.newgame.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.dao.RosterDao
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.players.data.PlayersRepository
import kotlinx.coroutines.launch

@Composable
fun NewGameScreen(
    defaultQuarterLengthSec: Int,
    gamesRepo: GamesRepository,
    playersRepo: PlayersRepository,
    rosterDao: RosterDao,
    onStart: (createdGameId: Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    var opponent by rememberSaveable { mutableStateOf("") }
    var quarterLen by rememberSaveable { mutableStateOf(defaultQuarterLengthSec) }

    val players by playersRepo.observePlayers().collectAsState(initial = emptyList())
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    val canStart = selectedIds.size >= 5

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New Game", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = opponent,
                onValueChange = { opponent = it },
                label = { Text("Opponent") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(480 to "8m", 600 to "10m", 720 to "12m").forEach { (sec, label) ->
                    FilterChip(
                        selected = quarterLen == sec,
                        onClick = { quarterLen = sec },
                        label = { Text(label) }
                    )
                }
            }

            Text(
                "Select roster (${selectedIds.size})",
                style = MaterialTheme.typography.titleMedium
            )

            Card {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(players) { p ->
                        val checked = p.id in selectedIds

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (checked) selectedIds - p.id else selectedIds + p.id
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedIds = if (isChecked) selectedIds + p.id else selectedIds - p.id
                                }
                            )
                        }
                    }
                }
            }

            Button(
                enabled = canStart,
                onClick = {
                    val opp = opponent.trim().ifEmpty { "Unknown" }
                    val ids = selectedIds.toList()

                    scope.launch {
                        val gameId = gamesRepo.createGame(opp, quarterLen)

                        rosterDao.insertAll(ids.map { pid ->
                            RosterEntity(gameId = gameId, playerId = pid)
                        })

                        onStart(gameId)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text(if (canStart) "Start" else "Select at least 5 players")
            }
        }
    }
}
