package com.example.basketballtracker.features.players.ui

import com.example.basketballtracker.ui.theme.SportsColors
import com.example.basketballtracker.features.core.ui.components.StatStrip
import com.example.basketballtracker.features.core.ui.components.StatPairs
import com.example.basketballtracker.features.core.ui.components.ShootingTable
import com.example.basketballtracker.features.core.ui.components.ShootingFigures
import com.example.basketballtracker.features.core.ui.components.SectionTitle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalDensity
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
                    title = "PLAYER PROFILE",
                    subTitle = state.player?.name ?: "Stats and game history",
                    onBack = onBack
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
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
                    item { SectionTitle("GAME LOG") }
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
            photoUri = state.player?.photoUri,
            game = game,
            onClose = { selectedGame = null }
        )
    }
}

@Composable
fun PlayerHeader(player: PlayerEntity, averages: PlayerAverages, gamesCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SportsColors.Surface),
        border = BorderStroke(1.dp, SportsColors.Line)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayerPhoto(player.photoUri, player.name, modifier = Modifier.size(80.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("AFEKA  /  #${player.number}", style = MaterialTheme.typography.labelSmall,
                        color = SportsColors.Accent, fontWeight = FontWeight.Bold)
                    Text(player.name, style = MaterialTheme.typography.headlineSmall,
                        color = SportsColors.Text, fontWeight = FontWeight.Black)
                    Text("$gamesCount games played", style = MaterialTheme.typography.bodySmall,
                        color = SportsColors.Muted)
                }
            }
            Spacer(Modifier.height(24.dp))
            SectionTitle("SEASON AVERAGES")
            Spacer(Modifier.height(12.dp))
            StatStrip(listOf("PPG" to String.format("%.1f", averages.ppg),
                "RPG" to String.format("%.1f", averages.rpg),
                "APG" to String.format("%.1f", averages.apg)))
            HorizontalDivider(color = SportsColors.Line)
            StatStrip(listOf("SPG" to String.format("%.1f", averages.spg),
                "BPG" to String.format("%.1f", averages.bpg),
                "TPG" to String.format("%.1f", averages.tpg)))
        }
    }
}

@Composable
fun PlayerGameCard(game: PlayerGameStats, onClick: () -> Unit) {
    val isPhone = LocalConfiguration.current.screenWidthDp < 600
    val date = remember(game.game.gameDateEpoch) {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(game.game.gameDateEpoch))
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SportsColors.Surface),
        border = BorderStroke(1.dp, SportsColors.Line)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("vs ${game.game.opponentName}", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium, color = SportsColors.Text)
                    Text(date, style = MaterialTheme.typography.bodySmall, color = SportsColors.Muted)
                }
                ResultScoreBadge(
                    if (game.game.teamScore > game.game.opponentScore) "W" else "L",
                    game.game.teamScore, game.game.opponentScore)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = SportsColors.Line)
            val stats = listOf("PTS" to game.points.toString(), "REB" to game.rebounds.toString(),
                "AST" to game.assists.toString())
            StatStrip(if (isPhone) stats else listOf("MIN" to formatMinutes(game.secondsPlayed)) +
                stats + listOf("STL" to game.steals.toString(), "BLK" to game.blocks.toString()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStatsBottomSheet(
    playerId: Long,
    playerName: String,
    photoUri: String?,
    game: PlayerGameStats,
    onClose: () -> Unit
) {
    var selectedPeriod by remember(playerId, game.game.id) { mutableStateOf<Int?>(null) }

    val filteredGame = remember(game, selectedPeriod) {
        buildPlayerGameStats(
            playerId = playerId,
            game = game.game,
            events = game.events,
            selectedPeriod = selectedPeriod
        )
    }
    val density = LocalDensity.current
    // Require a deliberate pull or a strong fling instead of dismissing on small scrolls.
    val positionalThreshold = { with(density) { 160.dp.toPx() } }
    val velocityThreshold = { with(density) { 1800.dp.toPx() } }
    val sheetState = rememberSaveable(
        density,
        saver = SheetState.Saver(
            skipPartiallyExpanded = true,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            confirmValueChange = { true },
            skipHiddenState = false
        )
    ) {
        SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold
        )
    }
    ModalBottomSheet(
        // Resolve the inset inside the sheet's dialog window, not the parent screen.
        modifier = Modifier.statusBarsPadding(),
        sheetMaxWidth = 400.dp,
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = SportsColors.Surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SportsColors.Text.copy(alpha = 0.25f)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerPhoto(photoUri, playerName, modifier = Modifier.size(64.dp))
                Text(
                    text = playerName,
                    modifier = Modifier.weight(1f),
                    color = SportsColors.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "vs ${game.game.opponentName}",
                    color = SportsColors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val date = SimpleDateFormat("dd/MM", Locale.ENGLISH)
                    .format(Date(game.game.gameDateEpoch))
                Text(
                    text = date,
                    color = SportsColors.Muted,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Card(
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CustomFilterChip(
                        text = "All",
                        active = selectedPeriod == null,
                        onClick = { selectedPeriod = null }
                    )

                    (1..maxOf(game.game.quartersCount, game.events.maxOfOrNull { it.period } ?: 1)).forEach { period ->
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
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = SportsColors.Raised
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
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("○ Made", color = SportsColors.Green, style = MaterialTheme.typography.labelMedium)
                Text("× Missed", color = SportsColors.Red, style = MaterialTheme.typography.labelMedium)
            }
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
            .background(SportsColors.Raised),
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
                .border(1.dp, SportsColors.Text.copy(alpha = 0.15f))
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
                        color = SportsColors.Green,
                        radius = size,
                        center = Offset(
                            x = (x / 15f) * courtW,
                            y = (y / 14f) * courtH
                        ),
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    drawLine(
                        color = SportsColors.Red,
                        start = Offset(center.x - size, center.y - size),
                        end = Offset(center.x + size, center.y + size),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = SportsColors.Red,
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
fun SheetStatsRows(stats: PlayerGameStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("PERFORMANCE")
        StatStrip(listOf("POINTS" to stats.points.toString(), "REBOUNDS" to stats.rebounds.toString(),
            "ASSISTS" to stats.assists.toString()))
        ShootingTable(listOf(
            ShootingFigures("Field goals", stats.fgMade, stats.fgAttempts),
            ShootingFigures("2-pointers", stats.twoMade, stats.twoAttempts),
            ShootingFigures("3-pointers", stats.threeMade, stats.threeAttempts),
            ShootingFigures("Free throws", stats.ftMade, stats.ftAttempts)
        ))
        StatPairs(listOf(
            "Minutes" to formatMinutes(stats.secondsPlayed), "Fouls" to stats.pf.toString(),
            "Off. rebounds" to stats.rebOff.toString(), "Def. rebounds" to stats.rebDef.toString(),
            "Steals" to stats.steals.toString(), "Blocks" to stats.blocks.toString(),
            "Turnovers" to stats.turnovers.toString()
        ))
    }
}
