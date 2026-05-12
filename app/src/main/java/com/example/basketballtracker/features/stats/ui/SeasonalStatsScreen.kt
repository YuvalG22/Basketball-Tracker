package com.example.basketballtracker.features.stats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChipDefaults.filterChipColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.basketballtracker.features.history.state.GamesHistoryViewModel
import com.example.basketballtracker.features.history.ui.GamesHistoryScreen
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import com.example.basketballtracker.features.stats.state.SeasonStatsViewModel
import com.example.basketballtracker.features.stats.state.StatsDisplayMode
import java.nio.file.WatchEvent

@Composable
fun SeasonStatsScreen(
    viewModel: SeasonStatsViewModel
) {
    val seasonStats by viewModel.seasonStats.collectAsStateWithLifecycle()
    var displayMode by remember { mutableStateOf(StatsDisplayMode.PER_GAME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "SEASON BOX SCORE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                StatsModeDropdown(displayMode) { displayMode = it }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                stickyHeader { TableHeader() }
                itemsIndexed(seasonStats) { index, player ->
                    PlayerSeasonStatsRow(player, displayMode, index)
//                    HorizontalDivider(
//                        modifier = Modifier.fillMaxWidth(),
//                        thickness = 1.dp,
//                        color = Color.White.copy(alpha = 0.08f)
//                    )
                }
            }
        }
    }
}

@Composable
fun PlayerSeasonStatsRow(
    player: PlayerSeasonStats,
    mode: StatsDisplayMode,
    index: Int
) {
    val backgroundColor = if (index % 2 == 0) {
        Color.Transparent
    } else {
        Color.LightGray.copy(alpha = 0.05f)
    }
    val textColor = Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(0.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${player.playerNumber}",
                modifier = Modifier.width(32.dp),
                color = textColor.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = player.playerName,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        StatCell(player.gp.toString(), 0.2f, TextAlign.Center, textColor)
        StatCell(formatStat(player.pts, player.gp, mode), 0.2f, TextAlign.Center, textColor)
        StatCell(formatStat(player.rebTotal, player.gp, mode), 0.2f, TextAlign.Center, textColor)
        StatCell(formatStat(player.ast, player.gp, mode), 0.2f, TextAlign.Center, textColor)
        StatCell(formatStat(player.stl, player.gp, mode), 0.2f, TextAlign.Center, textColor)
        StatCell(formatStat(player.blk, player.gp, mode), 0.2f, TextAlign.Center, textColor)
        StatCell(
            formatShootingStat(player.fgm, player.fga, player.gp, mode),
            0.2f,
            TextAlign.Center,
            textColor
        )
        StatCell(
            formatShootingStat(player.threem, player.threea, player.gp, mode),
            0.2f,
            TextAlign.Center,
            textColor
        )
        StatCell(
            formatShootingStat(player.ftm, player.fta, player.gp, mode),
            0.2f,
            TextAlign.Center,
            textColor
        )
        StatCell(formatStat(player.tov, player.gp, mode), 0.2f, TextAlign.Center, textColor)
        StatCell(formatStat(player.pf, player.gp, mode), 0.2f, TextAlign.Center, textColor)
    }
}

@Composable
fun RowScope.StatCell(stat: String, w: Float, alignment: TextAlign? = null, color: Color) {
    Text(
        text = stat,
        modifier = Modifier.weight(w),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = alignment,
        color = color
    )
}

@Composable
fun TableHeader() {
    val textColor = Color.White.copy(alpha = 0.5f)
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = Color.White.copy(alpha = 0.08f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(0.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                modifier = Modifier.width(32.dp),
                color = textColor.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "PLAYER",
                color = textColor.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        StatCell("GAMES", 0.2f, TextAlign.Center, textColor)
        StatCell("PTS", 0.2f, TextAlign.Center, textColor)
        StatCell("REB", 0.2f, TextAlign.Center, textColor)
        StatCell("AST", 0.2f, TextAlign.Center, textColor)
        StatCell("STL", 0.2f, TextAlign.Center, textColor)
        StatCell("BLK", 0.2f, TextAlign.Center, textColor)
        StatCell("FG%", 0.2f, TextAlign.Center, textColor)
        StatCell("3P%", 0.2f, TextAlign.Center, textColor)
        StatCell("FT%", 0.2f, TextAlign.Center, textColor)
        StatCell("TOV", 0.2f, TextAlign.Center, textColor)
        StatCell("PF", 0.2f, TextAlign.Center, textColor)
    }
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = Color.White.copy(alpha = 0.08f)
    )
}

@Composable
fun StatsModeDropdown(
    selectedMode: StatsDisplayMode,
    onModeSelected: (StatsDisplayMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (selectedMode == StatsDisplayMode.TOTAL) "Total" else "Per Game",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                StatsDisplayMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (mode == StatsDisplayMode.TOTAL) "Total" else "Average",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        },
                        leadingIcon = {
                            if (selectedMode == mode) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

fun formatStat(value: Int, gamesPlayed: Int, mode: StatsDisplayMode): String {
    return if (mode == StatsDisplayMode.TOTAL || gamesPlayed <= 1) {
        value.toString()
    } else {
        "%.1f".format(value.toFloat() / gamesPlayed)
    }
}

fun formatShootingStat(
    made: Int,
    attempted: Int,
    gamesPlayed: Int,
    mode: StatsDisplayMode
): String {
    val pct = (made.toFloat() / attempted * 100).toInt()
    return if (mode == StatsDisplayMode.TOTAL || gamesPlayed <= 1) {
        "$pct% ($made/$attempted)"
    } else {
        "%d%%".format(
            pct
        )
    }
}