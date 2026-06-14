package com.example.basketballtracker.features.newgame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.dao.RosterDao
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.core.data.remote.RetrofitClient.rosterApi
import com.example.basketballtracker.core.data.remote.roster.RosterUploadDto
import com.example.basketballtracker.features.core.ui.components.CustomFilterChip
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.players.data.PlayersRepository
import com.example.basketballtracker.features.summary.ui.SummaryTopBar
import kotlinx.coroutines.launch

private val NewGameAccent = Color(0xFF2ECC71)

@Composable
fun NewGameScreen(
    defaultQuarterLengthSec: Int,
    gamesRepo: GamesRepository,
    playersRepo: PlayersRepository,
    rosterDao: RosterDao,
    playerDao: PlayerDao,
    gameDao: GameDao,
    onStart: (createdGameId: Long) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var opponent by rememberSaveable { mutableStateOf("") }
    var isHomeGame by rememberSaveable { mutableStateOf(false) }
    var quarterLen by rememberSaveable { mutableIntStateOf(defaultQuarterLengthSec) }
    var roundText by rememberSaveable { mutableStateOf("") }

    var isPlayoff by rememberSaveable { mutableStateOf(false) }
    var playoffStage by rememberSaveable { mutableStateOf("SEMI_FINAL") }
    var playoffGameNumber by rememberSaveable { mutableStateOf("1") }

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
            val gameId = gamesRepo.createGame(
                opp,
                isHomeGame,
                round,
                gameDateEpoch,
                quarterLen,
                isPlayoff = isPlayoff,
                playoffStage = if (isPlayoff) playoffStage else null,
                playoffGameNumber = if (isPlayoff) {
                    playoffGameNumber.toIntOrNull()
                } else {
                    null
                }
            )

            ids.forEach { pid ->
                val roster = RosterEntity(
                    gameId = gameId,
                    playerId = pid,
                    syncStatus = "PENDING"
                )

                rosterDao.insert(roster)

                try {
                    val game = gameDao.getById(roster.gameId)
                    val gameRemoteId = game?.remoteId ?: return@forEach

                    val player = playerDao.getPlayerById(roster.playerId)
                    val playerRemoteId = player?.remoteId ?: return@forEach

                    val response = rosterApi.uploadRoster(
                        RosterUploadDto(
                            gameId = roster.gameId,
                            playerId = roster.playerId,
                            gameRemoteId = gameRemoteId,
                            playerRemoteId = playerRemoteId
                        )
                    )

                    rosterDao.markSynced(
                        gameId = gameId,
                        playerId = pid,
                        remoteId = response.remoteId
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            onStart(gameId)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryTopBar(
                    title = "NEW GAME",
                    subTitle = "Set game details and choose at least 5 players",
                    onBack = onBack
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NewGameDetailsCard(
                        modifier = Modifier.weight(1f),
                        opponent = opponent,
                        onOpponent = { opponent = it },
                        roundText = roundText,
                        isPlayoff = isPlayoff,
                        onPlayoffChange = { isPlayoff = it },
                        playoffStage = playoffStage,
                        onPlayoffStage = { playoffStage = it },
                        playoffGameNumber = playoffGameNumber,
                        onPlayoffGameNumber = { playoffGameNumber = it },
                        onRound = { roundText = it.filter(Char::isDigit).take(3) },
                        isHomeGame = isHomeGame,
                        onHomeToggle = { isHomeGame = !isHomeGame }
                    )

                    RosterSelectCard(
                        modifier = Modifier.weight(1f),
                        players = players,
                        selectedIds = selectedIds,
                        onToggle = ::togglePlayer
                    )
                }

                Button(
                    enabled = canStart,
                    onClick = ::startGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NewGameAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = if (canStart) {
                            "START GAME"
                        } else {
                            "SELECT ${5 - selectedIds.size} MORE PLAYERS"
                        },
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun NewGameDetailsCard(
    modifier: Modifier = Modifier,
    opponent: String,
    onOpponent: (String) -> Unit,

    roundText: String,
    onRound: (String) -> Unit,

    isPlayoff: Boolean,
    onPlayoffChange: (Boolean) -> Unit,

    playoffStage: String,
    onPlayoffStage: (String) -> Unit,

    playoffGameNumber: String,
    onPlayoffGameNumber: (String) -> Unit,

    isHomeGame: Boolean,
    onHomeToggle: () -> Unit,
) {
    NewGameCard(modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle("GAME DETAILS")

            StyledTextField(
                value = opponent,
                onValueChange = onOpponent,
                label = "Opponent",
                placeholder = "Opponent team"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CustomFilterChip(
                    text = "Season",
                    active = !isPlayoff,
                    onClick = { onPlayoffChange(false) }
                )

                CustomFilterChip(
                    text = "Playoff",
                    active = isPlayoff,
                    onClick = { onPlayoffChange(true) }
                )
            }

            if (!isPlayoff) {
                StyledTextField(
                    value = roundText,
                    onValueChange = onRound,
                    label = "Round",
                    placeholder = "Round number"
                )
            } else {
                PlayoffStageSelector(
                    selectedStage = playoffStage,
                    onStageSelected = onPlayoffStage
                )

                StyledTextField(
                    value = playoffGameNumber,
                    onValueChange = onPlayoffGameNumber,
                    label = "Game number",
                    placeholder = "1"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onHomeToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Home game",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isHomeGame) "Playing at home" else "Away game",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = isHomeGame,
                    onCheckedChange = { onHomeToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NewGameAccent,
                        checkedTrackColor = NewGameAccent.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}

@Composable
private fun RosterSelectCard(
    modifier: Modifier = Modifier,
    players: List<PlayerEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    NewGameCard(modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle("SELECT ROSTER")

                Text(
                    text = "${selectedIds.size}/${players.size}",
                    color = NewGameAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No players available",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEach { player ->
                        PlayerSelectRow(
                            player = player,
                            checked = player.id in selectedIds,
                            onClick = { onToggle(player.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSelectRow(
    player: PlayerEntity,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (checked) {
                    NewGameAccent.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
                },
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${player.number}",
                color = NewGameAccent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = player.name,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        Checkbox(
            checked = checked,
            onCheckedChange = { onClick() },
            colors = CheckboxDefaults.colors(
                checkedColor = NewGameAccent
            )
        )
    }
}

@Composable
private fun NewGameCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .background(NewGameAccent, RoundedCornerShape(50))
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NewGameAccent,
                cursorColor = NewGameAccent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
            )
        )
    }
}

@Composable
private fun PlayoffStageSelector(
    selectedStage: String,
    onStageSelected: (String) -> Unit
) {
    val stages = listOf(
        "QUARTER_FINAL" to "Quarter Final",
        "SEMI_FINAL" to "Semi Final",
        "FINAL" to "Final"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Playoff stage",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stages.forEach { (value, label) ->
                val selected = selectedStage == value

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) NewGameAccent.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) NewGameAccent
                            else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onStageSelected(value) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) NewGameAccent
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}