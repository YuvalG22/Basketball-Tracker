package com.example.basketballtracker.features.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.sqlite.throwSQLiteException
import com.example.basketballtracker.features.stats.state.StatsSort
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme

data class PlayerRowUi(
    val number: Int,
    val name: String,
    val games: String,
    val pts: String,
    val ast: String,
    val reb: String,
    val dreb: String,
    val oreb: String,
    val stl: String,
    val blk: String,
    val tov: String,
    val fg: String,
    val fgPct: String,
    val three: String,
    val threePct: String,
    val ft: String,
    val ftPct: String,
    val pf: String,
    val pm: String
)

data class SeasonTotals(
    val gp: Int,
    val pts: Int,
    val ast: Int,
    val reb: Int,
    val dreb: Int,
    val oreb: Int,
    val stl: Int,
    val blk: Int,
    val tov: Int,
    val pf: Int,
    val fgm: Int,
    val fga: Int,
    val threem: Int,
    val threea: Int,
    val ftm: Int,
    val fta: Int
)

@Composable
private fun RowScope.StatCell(text: String, cellWeight: Float = 1f, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.weight(cellWeight),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.SansSerif
        ),
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun RowScope.StatHeaderCell(
    text: String,
    cellWeight: Float = 1f,
    onSort: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(cellWeight)
            .clickable(onClick = onSort),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
fun TableHeaderSeasonal(
    onSort: (StatsSort) -> Unit,
    scrollState: ScrollState
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                "PLAYER",
                modifier = Modifier.width(200.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .width(1200.dp)
            ) {
                StatHeaderCell("GP", onSort = { onSort(StatsSort.GP) })
                StatHeaderCell("PTS", onSort = { onSort(StatsSort.PTS) })
                StatHeaderCell("AST", onSort = { onSort(StatsSort.AST) })
                StatHeaderCell("REB", onSort = { onSort(StatsSort.REB) })
                StatHeaderCell("DREB", onSort = { onSort(StatsSort.DREB) })
                StatHeaderCell("OREB", onSort = { onSort(StatsSort.OREB) })
                StatHeaderCell("STL", onSort = { onSort(StatsSort.STL) })
                StatHeaderCell("BLK", onSort = { onSort(StatsSort.BLK) })
                StatHeaderCell("TO", onSort = { onSort(StatsSort.TOV) })

                StatHeaderCell("FG", onSort = { onSort(StatsSort.FGM) })
                StatHeaderCell("FG%", onSort = { onSort(StatsSort.FG_PCT) })
                StatHeaderCell("3PT", onSort = { onSort(StatsSort.THREEM) })
                StatHeaderCell("3PT%", onSort = { onSort(StatsSort.THREE_PCT) })
                StatHeaderCell("FT", onSort = { onSort(StatsSort.FTM) })
                StatHeaderCell("FT%", onSort = { onSort(StatsSort.FT_PCT) })

                StatHeaderCell("PF", onSort = { onSort(StatsSort.PF) })
                StatHeaderCell("+/-", onSort = { onSort })
            }
        }
    }
    HorizontalDivider(
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
fun PlayerRowSeasonal(row: PlayerRowUi, index: Int, size: Int, scrollState: ScrollState) {
    val backgroundColor = when {
        index == size - 1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f) // Total row
        index % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    val isBold = index == size - 1
    Row(
        Modifier
            .height(50.dp)
            .background(backgroundColor)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.width(200.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.name,
                fontWeight = if (index == size - 1) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (index == size - 1) "" else "#${row.number}",
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            )
        }
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(1200.dp)
        ) {
            StatCell(row.games, bold = isBold)
            StatCell(row.pts, bold = isBold)
            StatCell(row.ast, bold = isBold)
            StatCell(row.reb, bold = isBold)
            StatCell(row.dreb, bold = isBold)
            StatCell(row.oreb, bold = isBold)
            StatCell(row.stl, bold = isBold)
            StatCell(row.blk, bold = isBold)
            StatCell(row.tov, bold = isBold)

            StatCell(row.fg, bold = isBold)
            StatCell("${row.fgPct}%", bold = isBold)
            StatCell(row.three, bold = isBold)
            StatCell("${row.threePct}%", bold = isBold)
            StatCell(row.ft, bold = isBold)
            StatCell("${row.ftPct}%", bold = isBold)

            StatCell(row.pf, bold = isBold)
            StatCell(row.pm, bold = isBold)
        }
    }
}