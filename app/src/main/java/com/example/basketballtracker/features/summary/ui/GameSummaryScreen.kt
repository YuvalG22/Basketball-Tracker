package com.example.basketballtracker.features.summary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private val SummaryAccent = Color(0xFF2ECC71)
private val BoxScoreRowHeight = 56.dp
private val BoxScoreHeaderHeight = 40.dp

@Composable
fun GameSummaryScreen(
    viewModel: GameSummaryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val teamTotals = state.teamTotals ?: return

    if (state.isLoading) {
        LoadingSummary(onBack)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SummaryTopBar(
                    title = "GAME SUMMARY",
                    subTitle = "Box score, team totals and game leaders",
                    onBack = onBack
                )
            }

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
                TeamSummaryCard(teamTotals)
            }

            item {
                BoxScoreCard(
                    rows = state.rows,
                    teamTotals = teamTotals
                )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun GameResultCard(
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
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
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
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
private fun TeamSummaryCard(t: TeamTotals) {
    DashboardCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle("TEAM TOTALS")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigStatBox("PTS", t.pts.toString(), Modifier.weight(1f))
                BigStatBox("REB", t.rebTotal.toString(), Modifier.weight(1f))
                BigStatBox("AST", t.ast.toString(), Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStatBox("STL", t.stl.toString(), Modifier.weight(1f))
                SmallStatBox("BLK", t.blk.toString(), Modifier.weight(1f))
                SmallStatBox("TOV", t.tov.toString(), Modifier.weight(1f))
                SmallStatBox("PF", t.pf.toString(), Modifier.weight(1f))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShootingBox("FG", "${t.fgm}/${t.fga}", "${pct(t.fgm, t.fga)}%", Modifier.weight(1f))
                ShootingBox(
                    "3PT",
                    "${t.threem}/${t.threea}",
                    "${pct(t.threem, t.threea)}%",
                    Modifier.weight(1f)
                )
                ShootingBox("FT", "${t.ftm}/${t.fta}", "${pct(t.ftm, t.fta)}%", Modifier.weight(1f))
            }
        }
    }
}

fun pct(made: Int, att: Int): Int {
    if (att == 0) return 0
    return ((made * 100.0) / att).roundToInt()
}

@Composable
private fun BoxScoreCard(
    rows: List<GameSummaryPlayerRowUi>,
    teamTotals: TeamTotals
) {

    val horizontalScroll = rememberScrollState()

    val starters = remember(rows) {
        rows.filter { it.isStarter }
    }

    val bench = remember(rows) {
        rows.filter { !it.isStarter }
    }

    DashboardCard {

        Column {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                SectionTitle("BOX SCORE")
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
                        bench = bench
                    )

                    Column(
                        modifier = Modifier
                            .horizontalScroll(horizontalScroll)
                            .width(statsWidth)
                    ) {

                        StatsHeader()

                        starters.forEach { row ->

                            StatsRow(row)

                            DividerLine()
                        }

                        if (starters.isNotEmpty() && bench.isNotEmpty()) {
                            BenchDivider(false)
                        }

                        bench.forEach { row ->

                            StatsRow(row)

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
    bench: List<GameSummaryPlayerRowUi>
) {

    Column(
        modifier = Modifier.width(170.dp)
    ) {

        PlayerHeader()

        starters.forEach { row ->

            PlayerCell(row)

            DividerLine()
        }

        if (starters.isNotEmpty() && bench.isNotEmpty()) {
            BenchDivider(true)
        }

        bench.forEach { row ->

            PlayerCell(row)

            DividerLine()
        }

        TeamPlayerTotalCell()
    }
}

@Composable
private fun PlayerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BoxScoreHeaderHeight)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "PLAYER",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayerCell(
    row: GameSummaryPlayerRowUi
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(BoxScoreRowHeight)
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
private fun StatsHeader() {
    Row(
        modifier = Modifier
            .height(BoxScoreHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("MIN")
        HeaderCell("PTS")
        HeaderCell("REB")
        HeaderCell("AST")
        HeaderCell("DREB")
        HeaderCell("OREB")
        HeaderCell("STL")
        HeaderCell("BLK")
        HeaderCell("FG", 1.5f)
        HeaderCell("3PT", 1.5f)
        HeaderCell("FT", 1.5f)
        HeaderCell("TO")
        HeaderCell("PF")
        HeaderCell("+/-")
    }
}

@Composable
private fun StatsRow(
    row: GameSummaryPlayerRowUi
) {
    val pmText =
        if (row.plusMinus > 0)
            "+${row.plusMinus}"
        else
            "${row.plusMinus}"
    Row(
        modifier = Modifier
            .height(BoxScoreRowHeight),
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
private fun RowScope.HeaderCell(
    text: String,
    weight: Float? = null
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight ?: 1f),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DashboardCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
private fun ResultBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                RoundedCornerShape(50)
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
private fun BigStatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(100.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            color = SummaryAccent,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SmallStatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            color = SummaryAccent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ShootingBox(
    title: String,
    madeAtt: String,
    pct: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = pct,
            color = SummaryAccent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )

        Text(
            text = madeAtt,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            style = MaterialTheme.typography.bodySmall
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
            color = Color(0xFF0288D1),
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
        shape = RoundedCornerShape(18.dp),
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
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "$made/$attempted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SCORE BY QUARTER",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
                    SummaryAccent.copy(alpha = if (isQuarterWinner) 1f else 0.55f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
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