package com.example.basketballtracker.features.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basketballtracker.ui.theme.SportsColors
import java.util.Locale

/** Large tabular figures in one continuous strip, without nested cards. */
@Composable
fun StatStrip(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        stats.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(SportsColors.Line))
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black, color = SportsColors.Text)
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = SportsColors.Muted, letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

/** Two aligned label/value columns for supporting statistics. */
@Composable
fun StatPairs(stats: List<Pair<String, String>>) {
    Column {
        stats.chunked(2).forEach { pair ->
            HorizontalDivider(color = SportsColors.Line)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                pair.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall, color = SportsColors.Muted)
                        Spacer(Modifier.width(6.dp))
                        Text(value, fontWeight = FontWeight.Bold, color = SportsColors.Text,
                            style = MaterialTheme.typography.titleSmall)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

data class ShootingFigures(val label: String, val made: Int, val attempted: Int)

@Composable
fun ShootingTable(rows: List<ShootingFigures>) {
    Column {
        Row(Modifier.fillMaxWidth().background(SportsColors.Raised).padding(10.dp)) {
            Text("SHOOTING", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                color = SportsColors.Muted)
            Text("M / A", Modifier.width(72.dp), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall, color = SportsColors.Muted)
            Text("PCT", Modifier.width(66.dp), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall, color = SportsColors.Muted)
        }
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(row.label, Modifier.weight(1f), color = SportsColors.Text,
                    style = MaterialTheme.typography.bodyMedium)
                Text("${row.made} / ${row.attempted}", Modifier.width(72.dp),
                    textAlign = TextAlign.End, color = SportsColors.Muted,
                    style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (row.attempted == 0) "—" else
                        String.format(Locale.US, "%.1f%%", row.made * 100.0 / row.attempted),
                    Modifier.width(66.dp), textAlign = TextAlign.End,
                    color = SportsColors.Accent, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            HorizontalDivider(color = SportsColors.Line)
        }
    }
}
