package com.example.basketballtracker.features.newgame.ui

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.dao.RosterDao
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.players.data.PlayersRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NewGameScreen(
    defaultQuarterLengthSec: Int,
    gamesRepo: GamesRepository,
    playersRepo: PlayersRepository,
    rosterDao: RosterDao,
    onStart: (createdGameId: Long) -> Unit
) {
    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val scope = rememberCoroutineScope()

    var opponent by rememberSaveable { mutableStateOf("") }
    var quarterLen by rememberSaveable { mutableIntStateOf(defaultQuarterLengthSec) }
    var roundText by rememberSaveable { mutableStateOf("") }

    val gameDateEpoch = remember { System.currentTimeMillis() }

    val players by playersRepo.observePlayers().collectAsState(initial = emptyList())
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    val canStart = selectedIds.size >= 5

    fun togglePlayer(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun startGame() {
        val round = roundText.toIntOrNull() ?: 0
        val opp = opponent.trim().ifEmpty { "Unknown" }
        val ids = selectedIds.toList()

        scope.launch {
            val gameId = gamesRepo.createGame(opp, round, gameDateEpoch, quarterLen)

            rosterDao.insertAll(
                ids.map { pid -> RosterEntity(gameId = gameId, playerId = pid) }
            )

            onStart(gameId)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (isWide) {
            NewGameWideLayout(
                opponent = opponent,
                onOpponent = { opponent = it },
                roundText = roundText,
                onRound = { roundText = it.filter(Char::isDigit).take(3) },
                players = players,
                selectedIds = selectedIds,
                onToggle = ::togglePlayer,
                canStart = canStart,
                onStartClick = ::startGame
            )
        } else {
            NewGameNarrowLayout(
                opponent = opponent,
                onOpponent = { opponent = it },
                roundText = roundText,
                onRound = { roundText = it.filter(Char::isDigit).take(3) },
                players = players,
                selectedIds = selectedIds,
                onToggle = ::togglePlayer,
                canStart = canStart,
                onStartClick = ::startGame
            )
        }

    }
}

@Composable
private fun NewGameNarrowLayout(
    opponent: String,
    onOpponent: (String) -> Unit,
    roundText: String,
    onRound: (String) -> Unit,
    players: List<PlayerEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    canStart: Boolean,
    onStartClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("New Game", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = opponent,
            onValueChange = onOpponent,
            label = { Text("Opponent") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = roundText,
            onValueChange = onRound,
            label = { Text("Round") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(players, key = { it.id }) { p ->
                    val checked = p.id in selectedIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(p.id) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.name, style = MaterialTheme.typography.bodyLarge)
                        Checkbox(checked = checked, onCheckedChange = { onToggle(p.id) })
                    }
                }
            }
        }

        Button(
            enabled = canStart,
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(if (canStart) "Start" else "Select at least 5 players")
        }
    }
}

@Composable
private fun NewGameWideLayout(
    opponent: String,
    onOpponent: (String) -> Unit,
    roundText: String,
    onRound: (String) -> Unit,
    players: List<PlayerEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    canStart: Boolean,
    onStartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("New Game", style = MaterialTheme.typography.headlineSmall)
            Column() {
                MyTextField(
                    text = opponent,
                    onValueChange = onOpponent,
                    label = "Opponent",
                    hint = "Opponent",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                MyTextField(
                    text = roundText,
                    onValueChange = onRound,
                    label = "Round",
                    hint = "Round",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(players, key = { it.id }) { p ->
                        val checked = p.id in selectedIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(p.id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Checkbox(checked = checked, onCheckedChange = { onToggle(p.id) })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                enabled = canStart,
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(if (canStart) "Start" else "Select at least 5 players")
            }
        }
    }
}

@Composable
fun MyTextField(
    text: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            placeholder = {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
