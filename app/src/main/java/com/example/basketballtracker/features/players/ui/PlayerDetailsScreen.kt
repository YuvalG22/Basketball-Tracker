package com.example.basketballtracker.features.players.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basketballtracker.R
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.core.ui.components.CustomFilterChip
import com.example.basketballtracker.features.history.ui.ResultBadge
import com.example.basketballtracker.features.history.ui.ResultScoreBadge
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.players.data.PlayerAverages
import com.example.basketballtracker.features.players.data.PlayerGameStats
import com.example.basketballtracker.features.summary.ui.SummaryTopBar

@Composable
fun PlayerDetailsScreen(
    playerId: Long,
    viewModel: PlayerDetailsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedGame by remember { mutableStateOf<PlayerGameStats?>(null) }

    LaunchedEffect(playerId) {
        viewModel.loadPlayer(playerId)
    }

    if (state.isLoading) {
        Text("Loading...")
        return
    }
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryTopBar(
                    title = "Player Stats",
                    subTitle = "View player stats and game history",
                    onBack = onBack
                )
            }

            item {
                state.player?.let {
                    PlayerHeader(
                        player = it,
                        averages = state.averages,
                        gamesCount = state.games.size
                    )
                }
            }

            item { Text("Games", style = MaterialTheme.typography.titleLarge) }

            item {
                state.games.forEach { game ->
                    PlayerGameCard(
                        game = game,
                        onClick = { selectedGame = game }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    selectedGame?.let { game ->
        GameStatsBottomSheet(
            game = game,
            onClose = { selectedGame = null }
        )
    }
}

@Composable
fun PlayerHeader(
    player: PlayerEntity,
    averages: PlayerAverages,
    gamesCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2D2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.number.toString(),
                        color = Color(0xFF2ECC71),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$gamesCount games played",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("PPG", averages.ppg, Modifier.weight(1f))
                StatCard("RPG", averages.rpg, Modifier.weight(1f))
                StatCard("APG", averages.apg, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("SPG", averages.spg, Modifier.weight(1f))
                StatCard("BPG", averages.bpg, Modifier.weight(1f))
                StatCard("TPG", averages.tpg, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2A2A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.1f", value),
                color = Color(0xFF2ECC71),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PlayerGameCard(
    game: PlayerGameStats,
    onClick: () -> Unit
) {
    val isHome = game.game.isHomeGame
    val isWin = game.game.teamScore > game.game.opponentScore

    val resultText = when {
        isWin -> "WIN"
        else -> "LOSS"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResultScoreBadge(resultText, game.game.teamScore, game.game.opponentScore)
                        Text(
                            text = "vs ${game.game.opponentName}",
                            color = Color.White,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = game.points.toString(),
                        color = Color(0xFF2ECC71),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "PTS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStatCard("REB", game.rebounds, Modifier.weight(1f))
                MiniStatCard("AST", game.assists, Modifier.weight(1f))
                MiniStatCard("STL", game.steals, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MiniStatCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2A2A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStatsBottomSheet(
    game: PlayerGameStats,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF1F1D1D),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.White.copy(alpha = 0.25f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 32.dp
                )
        ) {
            Text(
                text = "vs ${game.game.opponentName}",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Round ${game.game.roundNumber}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStatCard("PTS", game.points, Modifier.weight(1f))
                MiniStatCard("REB", game.rebounds, Modifier.weight(1f))
                MiniStatCard("AST", game.assists, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStatCard("STL", game.steals, Modifier.weight(1f))
                MiniStatCard("BLK", game.blocks, Modifier.weight(1f))
                MiniStatCard("TOV", game.turnovers, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            ShootingSection(game)

            Spacer(Modifier.height(20.dp))

            ShotChartCard(
                shots = game.shots
            )
        }
    }
}

@Composable
fun ShootingSection(game: PlayerGameStats) {
    val twoAttempts = game.twoMade + game.twoMiss
    val threeAttempts = game.threeMade + game.threeMiss
    val ftAttempts = game.ftMade + game.ftMiss

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2A2A)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Shooting",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            ShotRow("2PT", game.twoMade, twoAttempts)
            ShotRow("3PT", game.threeMade, threeAttempts)
            ShotRow("FT", game.ftMade, ftAttempts)
        }
    }
}

@Composable
fun ShotRow(
    label: String,
    made: Int,
    attempts: Int
) {
    val percent = if (attempts == 0) 0.0 else made * 100.0 / attempts

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f)
        )

        Text(
            text = "$made/$attempts · ${String.format("%.1f", percent)}%",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ShotChartCard(
    shots: List<EventEntity>
) {
    var filter by remember {
        mutableStateOf("all")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2A2A)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shot Chart",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomFilterChip(
                        text = "All",
                        active = filter == "all",
                        onClick = { filter = "all" }
                    )

                    CustomFilterChip(
                        text = "Makes",
                        active = filter == "makes",
                        onClick = { filter = "makes" }
                    )

                    CustomFilterChip(
                        text = "Misses",
                        active = filter == "misses",
                        onClick = { filter = "misses" }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            ShotChart(
                shots = shots,
                filter = filter
            )
        }
    }
}

@Composable
fun ShotChart(
    shots: List<EventEntity>,
    filter: String
) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .aspectRatio(15f / 14f)
            .background(Color(0xFF262626)),
        contentAlignment = Alignment.Center
    ) {
        val filteredShots = shots.filter { shot ->
            val isMade =
                shot.type == EventType.TWO_MADE.name ||
                        shot.type == EventType.THREE_MADE.name

            when (filter) {
                "makes" -> isMade
                "misses" -> !isMade
                else -> true
            }
        }

        Image(
            painter = painterResource(R.drawable.half_court),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(15f / 14f)
                .border(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            val courtW = size.width
            val courtH = size.height

            filteredShots.forEach { shot ->
                val x = shot.shotX ?: return@forEach
                val y = shot.shotY ?: return@forEach

                val isMade =
                    shot.type == EventType.TWO_MADE.name ||
                            shot.type == EventType.THREE_MADE.name

                drawCircle(
                    color = if (isMade) Color(0xFF2ECC71) else Color.Red,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        x = (x / 15f) * courtW,
                        y = (y / 14f) * courtH
                    )
                )

                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        x = (x / 15f) * courtW,
                        y = (y / 14f) * courtH
                    ),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}