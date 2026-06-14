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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basketballtracker.R
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.core.ui.components.CustomFilterChip
import com.example.basketballtracker.features.history.ui.ResultBadge
import com.example.basketballtracker.features.history.ui.ResultScoreBadge
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.formatMinutes
import com.example.basketballtracker.features.players.data.PlayerAverages
import com.example.basketballtracker.features.players.data.PlayerGameStats
import com.example.basketballtracker.features.players.data.buildPlayerGameStats
import com.example.basketballtracker.features.summary.ui.SummaryTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            ) {
                SummaryTopBar(
                    title = state.player?.name ?: "Player Stats",
                    subTitle = "View player stats and game history",
                    onBack = onBack
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
        }
    }

    selectedGame?.let { game ->
        GameStatsBottomSheet(
            playerId = playerId,
            playerName = state.player?.name ?: "",
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
//                Box(
//                    modifier = Modifier
//                        .size(64.dp)
//                        .clip(CircleShape)
//                        .background(Color(0xFF2D2A2A)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = player.number.toString(),
//                        color = Color(0xFF2ECC71),
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                }

                Column {
                    Text(
                        text = "Season Averages",
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
    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    val resultText = if (game.game.teamScore > game.game.opponentScore) "W" else "L"

    val stats = if (isPhone) {
        listOf(
            "PTS" to game.points,
            "REB" to game.rebounds,
            "AST" to game.assists
        )
    } else {
        listOf(
            "MIN" to game.secondsPlayed,
            "PTS" to game.points,
            "REB" to game.rebounds,
            "AST" to game.assists,
            "STL" to game.steals,
            "BLK" to game.blocks,
            "TO" to game.turnovers
        )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GameInfoSection(
                game = game,
                resultText = resultText,
                isPhone = isPhone
            )

            StatsRow(stats = stats)
        }
    }
}

@Composable
private fun GameInfoSection(
    game: PlayerGameStats,
    resultText: String,
    isPhone: Boolean
) {
    val date = remember(game.game.gameDateEpoch) {
        SimpleDateFormat("dd/MM", Locale.ENGLISH)
            .format(Date(game.game.gameDateEpoch))
    }

    if (isPhone) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.width(90.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = date,
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "vs",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = game.game.opponentName,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ResultScoreBadge(
                resultText,
                game.game.teamScore,
                game.game.opponentScore
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.width(200.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = date,
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "vs",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = game.game.opponentName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }

            ResultScoreBadge(
                resultText,
                game.game.teamScore,
                game.game.opponentScore
            )
        }
    }
}

@Composable
private fun StatsRow(
    stats: List<Pair<String, Int>>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEachIndexed { index, stat ->
            MiniStatCard(stat.first, stat.second)

            if (index != stats.lastIndex) {
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    thickness = 1.dp,
                    color = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun MiniStatCard(
    label: String,
    value: Int,
) {
    Column(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStatsBottomSheet(
    playerId: Long,
    playerName: String,
    game: PlayerGameStats,
    onClose: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf<Int?>(null) }

    val filteredGame = remember(game, selectedPeriod) {
        buildPlayerGameStats(
            playerId = playerId,
            game = game.game,
            events = game.events,
            selectedPeriod = selectedPeriod
        )
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        sheetMaxWidth = 400.dp,
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
                text = playerName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "vs ${game.game.opponentName}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val date = SimpleDateFormat("dd/MM", Locale.ENGLISH)
                    .format(Date(game.game.gameDateEpoch))
                Text(
                    text = date,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Card(
                shape = RoundedCornerShape(999.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CustomFilterChip(
                        text = "All",
                        active = selectedPeriod == null,
                        onClick = { selectedPeriod = null }
                    )

                    (1..4).forEach { period ->
                        CustomFilterChip(
                            text = "Q$period",
                            active = selectedPeriod == period,
                            onClick = { selectedPeriod = period }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ShotChartCard(
                shots = filteredGame.shots
            )
            Spacer(Modifier.height(20.dp))
            SheetStatsRows(filteredGame)
        }
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
            modifier = Modifier
                .padding(0.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    CustomFilterChip(
//                        text = "All",
//                        active = filter == "all",
//                        onClick = { filter = "all" }
//                    )
//
//                    CustomFilterChip(
//                        text = "Makes",
//                        active = filter == "makes",
//                        onClick = { filter = "makes" }
//                    )
//
//                    CustomFilterChip(
//                        text = "Misses",
//                        active = filter == "misses",
//                        onClick = { filter = "misses" }
//                    )
//                }
//            }
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
    val isPhone = LocalConfiguration.current.screenWidthDp < 600
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
            painter = painterResource(R.drawable.court_15x14_original_parquet),
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
                val center = Offset(
                    x = (shot.shotX?.div(15f))?.times(size.width) ?: 1f,
                    y = (shot.shotY?.div(14f))?.times(size.height) ?: 1f
                )
                val x = shot.shotX ?: return@forEach
                val y = shot.shotY ?: return@forEach

                val isMade =
                    shot.type == EventType.TWO_MADE.name ||
                            shot.type == EventType.THREE_MADE.name

                val size = if (isPhone) 15f else 10f
                val strokeWidth = if (isPhone) 10f else 6f
                if (isMade) {
                    drawCircle(
                        color = Color(0xFF4CAF50),
                        radius = size,
                        center = Offset(
                            x = (x / 15f) * courtW,
                            y = (y / 14f) * courtH
                        ),
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    drawLine(
                        color = Color.Red,
                        start = Offset(center.x - size, center.y - size),
                        end = Offset(center.x + size, center.y + size),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color.Red,
                        start = Offset(center.x - size, center.y + size),
                        end = Offset(center.x + size, center.y - size),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}

@Composable
fun SheetStatsRows(
    stats: PlayerGameStats
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        StatRow("Minutes played", formatMinutes(stats.secondsPlayed).toInt())
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.White.copy(alpha = 0.15f)
        )
        StatRow("Points", stats.points)
        ShootingStatRow(
            "Free throws",
            stats.ftMade,
            stats.ftAttempts
        )
        ShootingStatRow(
            "2 pointers",
            stats.twoMade,
            stats.twoAttempts
        )
        ShootingStatRow(
            "3 pointers",
            stats.threeMade,
            stats.threeAttempts
        )
        ShootingStatRow(
            "Field goals",
            stats.fgMade,
            stats.fgAttempts
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.White.copy(alpha = 0.15f)
        )
        StatRow("Rebounds", stats.rebounds)
        StatRow("Defensive rebounds", stats.rebDef)
        StatRow("Offensive rebounds", stats.rebOff)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.White.copy(alpha = 0.15f)
        )
        StatRow("Assists", stats.assists)
        StatRow("Turnovers", stats.turnovers)
        StatRow("Steals", stats.steals)
        StatRow("Blocks", stats.blocks)
        StatRow("Personal fouls", stats.pf)
    }
}

@Composable
fun StatRow(
    title: String,
    value: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Light
        )
        Text(
            text = value.toString(),
            color = Color.White,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
fun ShootingStatRow(
    title: String,
    made: Int,
    attempts: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "$made/$attempts (${(made.toFloat() / attempts.toFloat() * 100).toInt()}%)",
            color = Color.White,
            fontWeight = FontWeight.Light
        )
    }
}