package com.example.basketballtracker.features.summary.ui

import androidx.compose.foundation.layout.fillMaxHeight

import com.example.basketballtracker.ui.theme.SportsColors
import com.example.basketballtracker.features.core.ui.components.StatStrip
import com.example.basketballtracker.features.core.ui.components.StatPairs
import com.example.basketballtracker.features.core.ui.components.ShootingFigures
import com.example.basketballtracker.features.core.ui.components.ShootingTable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.basketballtracker.features.players.ui.GameStatsBottomSheet
import com.example.basketballtracker.features.players.data.buildPlayerGameStats
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.basketballtracker.features.core.ui.components.SectionTitle
import kotlin.collections.forEachIndexed
import kotlin.math.roundToInt

private val SummaryAccent = SportsColors.Accent
private val BoxScoreRowHeight = 56.dp
private val BoxScoreHeaderHeight = 40.dp

@Composable
fun GameSummaryScreen(
    viewModel: GameSummaryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GameSummaryContent(state, onBack)
}

@Composable
internal fun GameSummaryContent(state: GameSummaryUiState, onBack: () -> Unit) {
    var selectedPlayerId by rememberSaveable(state.game?.id) { mutableStateOf<Long?>(null) }
    val selectedPlayer = state.rows.firstOrNull { it.playerId == selectedPlayerId }
    val selectedGame = remember(selectedPlayerId, state.game, state.events) {
        val game = state.game
        val playerId = selectedPlayerId
        if (game != null && playerId != null) {
            buildPlayerGameStats(playerId, game, state.events)
        } else null
    }
    if (selectedPlayer != null && selectedGame != null) {
        GameStatsBottomSheet(
            playerId = selectedPlayer.playerId,
            playerName = selectedPlayer.playerName,
            photoUri = selectedPlayer.photoUri,
            game = selectedGame,
            onClose = { selectedPlayerId = null }
        )
    }

    if (state.isLoading) {
        LoadingSummary(onBack)
        return
    }
    val teamTotals = state.teamTotals ?: return

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
                    title = "GAME SUMMARY",
                    subTitle = "Box score and team statistics",
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
//                item {
//                    SummaryTopBar(
//                        title = "GAME SUMMARY",
//                        subTitle = "Box score, team totals and game leaders",
//                        onBack = onBack
//                    )
//                }

                    item {
                        GameResultCard(
                            opponentName = state.opponentName,
                            roundNumber = state.roundNumber,
                            dateText = state.dateText,
                            teamScore = state.teamScore,
                            opponentScore = state.opponentScore,
                            isWin = state.isWin,
                            quarterScores = state.quarterScores
                        )
                    }

                    item {
                        com.example.basketballtracker.features.core.ui.components.TeamOverviewWithShotMap(state.events) {
                            TeamSummaryCard(teamTotals)
                        }
                    }

                    item {
                        BoxScoreCard(
                            rows = state.rows,
                            teamTotals = teamTotals,
                            onPlayerClick = { selectedPlayerId = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryTopBar(
    title: String,
    subTitle: String,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = SportsColors.Text)
            }
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black, color = SportsColors.Text,
                    letterSpacing = 0.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(subTitle, style = MaterialTheme.typography.bodySmall, color = SportsColors.Muted)
            }
        }
        HorizontalDivider(color = SportsColors.Line)
    }
}

@Composable
internal fun GameResultCard(
    opponentName: String,
    roundNumber: Int,
    dateText: String,
    teamScore: Int,
    opponentScore: Int,
    isWin: Boolean,
    quarterScores: List<QuarterScore>
) {
    DashboardCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResultBadge(if (isWin) "WIN" else "LOSS")

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "ROUND $roundNumber • $dateText",
                    color = SportsColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreSide(
                    name = "AFEKA",
                    score = teamScore,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "VS",
                    color = SportsColors.Muted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                ScoreSide(
                    name = opponentName,
                    score = opponentScore,
                    modifier = Modifier.weight(1f)
                )
            }
            QuarterScoreTable(
                opponentName = opponentName,
                quarterScores = quarterScores
            )
        }
    }
}

@Composable
private fun ScoreSide(
    name: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(4.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            color = SportsColors.Muted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = score.toString(),
            color = SummaryAccent,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
internal fun TeamSummaryCard(t: TeamTotals) {
    DashboardCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle("TEAM STATS")
            StatStrip(listOf("POINTS" to t.pts.toString(), "REBOUNDS" to t.rebTotal.toString(),
                "ASSISTS" to t.ast.toString()))
            ShootingTable(listOf(
                ShootingFigures("Field goals", t.fgm, t.fga),
                ShootingFigures("3-pointers", t.threem, t.threea),
                ShootingFigures("Free throws", t.ftm, t.fta)
            ))
            StatPairs(listOf(
                "Off. rebounds" to t.rebOff.toString(), "Def. rebounds" to t.rebDef.toString(),
                "Steals" to t.stl.toString(), "Blocks" to t.blk.toString(),
                "Turnovers" to t.tov.toString(), "Fouls" to t.pf.toString()
            ))
        }
    }
}

fun pct(made: Int, att: Int): Int {
    if (att == 0) return 0
    return ((made * 100.0) / att).roundToInt()
}

@Composable
internal fun BoxScoreCard(
    rows: List<GameSummaryPlayerRowUi>,
    teamTotals: TeamTotals,
    onPlayerClick: (Long) -> Unit
) {

    val horizontalScroll = rememberScrollState()

    var sortColumn by rememberSaveable { mutableStateOf<String?>(null) }
    var ascending by rememberSaveable { mutableStateOf(false) }
    val onSort: (String) -> Unit = { column ->
        val initialAscending = column == "PLAYER"
        when {
            sortColumn != column -> {
                sortColumn = column
                ascending = initialAscending
            }
            ascending == initialAscending -> ascending = !ascending
            else -> {
                sortColumn = null
                ascending = false
            }
        }
    }
    val sortedRows = remember(rows, sortColumn, ascending) { sortBoxScoreRows(rows, sortColumn, ascending) }
    val starters = if (sortColumn == null) sortedRows.filter { it.isStarter } else sortedRows
    val bench = if (sortColumn == null) sortedRows.filter { !it.isStarter } else emptyList()

    DashboardCard {

        Column {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                SectionTitle("BOX SCORE")
                Spacer(Modifier.height(6.dp))
                Text("Tap a column to sort, again to reverse, a third time to reset. Tap a player for game stats · Swipe for more columns", color = SportsColors.Muted,
                    style = MaterialTheme.typography.bodySmall)
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {

                val fixedPlayerWidth = 170.dp
                val minStatsWidth = 900.dp

                val statsWidth =
                    (maxWidth - fixedPlayerWidth)
                        .coerceAtLeast(minStatsWidth)

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    FixedPlayersColumn(
                        starters = starters,
                        bench = bench,
                        onPlayerClick = onPlayerClick,
                        playerHeader = { PlayerHeader(sortColumn, ascending) { onSort("PLAYER") } }
                    )

                    Column(
                        modifier = Modifier
                            .horizontalScroll(horizontalScroll)
                            .width(statsWidth)
                    ) {

                        StatsHeader(sortColumn, ascending, onSort)

                        starters.forEach { row ->

                            StatsRow(row, onClick = { onPlayerClick(row.playerId) })

                            DividerLine()
                        }

                        if (starters.isNotEmpty() && bench.isNotEmpty()) {
                            BenchDivider(false)
                        }

                        bench.forEach { row ->

                            StatsRow(row, onClick = { onPlayerClick(row.playerId) })

                            DividerLine()
                        }

                        TeamStatsTotalRow(teamTotals)
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedPlayersColumn(
    starters: List<GameSummaryPlayerRowUi>,
    bench: List<GameSummaryPlayerRowUi>,
    onPlayerClick: (Long) -> Unit,
    playerHeader: @Composable () -> Unit
) {

    Column(
        modifier = Modifier.width(170.dp).background(SportsColors.Surface)
    ) {

        playerHeader()

        starters.forEach { row ->

            PlayerCell(row, onClick = { onPlayerClick(row.playerId) })

            DividerLine()
        }

        if (starters.isNotEmpty() && bench.isNotEmpty()) {
            BenchDivider(true)
        }

        bench.forEach { row ->

            PlayerCell(row, onClick = { onPlayerClick(row.playerId) })

            DividerLine()
        }

        TeamPlayerTotalCell()
    }
}

@Composable
private fun PlayerHeader(sortColumn: String?, ascending: Boolean, onSort: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BoxScoreHeaderHeight)
            .clickable(role = Role.Button, onClickLabel = "Sort by player", onClick = onSort)
            .background(SportsColors.Raised)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        SortHeaderLabel("PLAYER", sortColumn == "PLAYER", ascending)
    }
}

@Composable
private fun PlayerCell(
    row: GameSummaryPlayerRowUi,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(BoxScoreRowHeight)
            .clickable(role = Role.Button, onClickLabel = "View ${row.playerName}'s game stats", onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = row.playerName,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Text(
            text = "#${row.playerNumber}",
            color = SummaryAccent,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TeamPlayerTotalCell() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BoxScoreRowHeight)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "TOTAL",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun StatsHeader(sortColumn: String?, ascending: Boolean, onSort: (String) -> Unit) {
    Row(
        modifier = Modifier
            .height(BoxScoreHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("MIN", "PTS", "REB", "AST", "DREB", "OREB", "STL", "BLK", "FG", "3PT", "FT", "TO", "PF", "+/-").forEach { column ->
            val selected = sortColumn == column
            HeaderCell(column,
                if (column in listOf("FG", "3PT", "FT")) 1.5f else 1f,
                selected = selected, ascending = ascending, onClick = { onSort(column) })
        }
    }
}

@Composable
private fun StatsRow(
    row: GameSummaryPlayerRowUi,
    onClick: () -> Unit
) {
    val pmText =
        if (row.plusMinus > 0)
            "+${row.plusMinus}"
        else
            "${row.plusMinus}"
    Row(
        modifier = Modifier
            .height(BoxScoreRowHeight)
            .clickable(role = Role.Button, onClickLabel = "View ${row.playerName}'s game stats", onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(row.min)
        StatCell("${row.pts}", accent = true)
        StatCell("${row.reb}")
        StatCell("${row.ast}")
        StatCell("${row.rebDef}")
        StatCell("${row.rebOff}")
        StatCell("${row.stl}")
        StatCell("${row.blk}")
        StatCell("${row.fgm}/${row.fga}", weight = 1.5f)
        StatCell("${row.threem}/${row.threea}", weight = 1.5f)
        StatCell("${row.ftm}/${row.fta}", weight = 1.5f)
        StatCell("${row.tov}")
        StatCell("${row.pf}")
        StatCell(pmText, accent = true)
    }
}

@Composable
private fun TeamStatsTotalRow(
    t: TeamTotals
) {
    Row(
        modifier = Modifier
            .height(BoxScoreRowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell("")
        StatCell("${t.pts}", accent = true)
        StatCell("${t.rebTotal}")
        StatCell("${t.ast}")
        StatCell("${t.rebDef}")
        StatCell("${t.rebOff}")
        StatCell("${t.stl}")
        StatCell("${t.blk}")
        StatCell("${t.fgm}/${t.fga}", weight = 1.5f)
        StatCell("${t.threem}/${t.threea}", weight = 1.5f)
        StatCell("${t.ftm}/${t.fta}", weight = 1.5f)
        StatCell("${t.tov}")
        StatCell("${t.pf}")
        StatCell("")
    }
}

@Composable
private fun DividerLine() {

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    )
}

@Composable
private fun RowScope.StatCell(
    text: String,
    accent: Boolean = false,
    weight: Float? = null
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight ?: 1f),
        textAlign = TextAlign.Center,
        color = if (accent) SummaryAccent else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (accent) FontWeight.Black else FontWeight.SemiBold
    )
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float = 1f, selected: Boolean, ascending: Boolean, onClick: () -> Unit) {
    Box(Modifier.weight(weight).fillMaxHeight().clickable(role = Role.Button,
        onClickLabel = "Sort by $text", onClick = onClick), contentAlignment = Alignment.Center) {
        SortHeaderLabel(text, selected, ascending)
    }
}

@Composable
private fun SortHeaderLabel(text: String, selected: Boolean, ascending: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text, color = if (selected) SummaryAccent else SportsColors.Muted,
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        if (selected) {
            Icon(
                imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = if (ascending) "Sorted ascending" else "Sorted descending",
                modifier = Modifier.size(14.dp),
                tint = SummaryAccent
            )
        }
    }
}

@Composable
private fun DashboardCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            SportsColors.Line
        )
    ) {
        content()
    }
}

@Composable
private fun ResultBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (text == "WIN") SummaryAccent else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun BenchDivider(isTextVisible: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (isTextVisible) "BENCH" else "",
            color = SportsColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LoadingSummary(
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = SummaryAccent
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Loading game summary...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun ShootingDonutsRow(
    teamTotals: TeamTotals
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShootingDonutCard(
            title = "FG%",
            subTitle = "Field Goals",
            color = SportsColors.Blue,
            made = teamTotals.fgm,
            attempted = teamTotals.fga,
            modifier = Modifier.weight(1f)
        )

        ShootingDonutCard(
            title = "3PT%",
            subTitle = "Three Points",
            color = SummaryAccent,
            made = teamTotals.threem,
            attempted = teamTotals.threea,
            modifier = Modifier.weight(1f)
        )

        ShootingDonutCard(
            title = "FT%",
            subTitle = "Free Throws",
            color = Color(0xFFFFC107),
            made = teamTotals.ftm,
            attempted = teamTotals.fta,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ShootingDonutCard(
    title: String,
    subTitle: String,
    color: Color,
    made: Int,
    attempted: Int,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme.surfaceVariant
    val pct = if (attempted == 0) 0f
    else made.toFloat() / attempted.toFloat()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(110.dp)
                ) {
                    drawArc(
                        color = c,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(
                            width = 18.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = pct * 360f,
                        useCenter = false,
                        style = Stroke(
                            width = 18.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SportsColors.Text,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "$made/$attempted",
                        style = MaterialTheme.typography.bodySmall,
                        color = SportsColors.Muted
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SportsColors.Muted,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = SportsColors.Muted,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun QuarterScoreTable(
    opponentName: String,
    quarterScores: List<QuarterScore>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                RoundedCornerShape(4.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SCORE BY QUARTER",
            color = SportsColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        QuarterRow(
            label = "",
            values = quarterScores.map { "Q${it.period}" },
            total = "TOT",
            isHeader = true
        )

        QuarterRow(
            label = "AFEKA",
            values = quarterScores.map { it.teamScore.toString() },
            total = quarterScores.sumOf { it.teamScore }.toString(),
            quarterScores = quarterScores,
            isNumberRow = true,
            isTeamRow = true
        )

        QuarterRow(
            label = opponentName,
            values = quarterScores.map { it.opponentScore.toString() },
            total = quarterScores.sumOf { it.opponentScore }.toString(),
            quarterScores = quarterScores,
            isNumberRow = true,
            isTeamRow = false
        )
    }
}

@Composable
private fun QuarterRow(
    label: String,
    values: List<String>,
    total: String,
    quarterScores: List<QuarterScore> = emptyList(),
    isHeader: Boolean = false,
    isNumberRow: Boolean = false,
    isTeamRow: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1.6f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isHeader) 0.45f else 0.75f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        values.forEachIndexed { index, value ->
            val current = value.toIntOrNull() ?: 0

            val other = if (quarterScores.isNotEmpty()) {
                if (isTeamRow) {
                    quarterScores[index].opponentScore
                } else {
                    quarterScores[index].teamScore
                }
            } else {
                0
            }

            val isQuarterWinner = isNumberRow && current > other

            Text(
                text = value,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = if (isNumberRow) {
                    if (isQuarterWinner) SummaryAccent else SportsColors.Muted
                } else {
                    SportsColors.Muted
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isQuarterWinner) FontWeight.Black else FontWeight.Medium
            )
        }

        Text(
            text = total,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = if (isNumberRow) SummaryAccent else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.45f
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black
        )
    }
}
