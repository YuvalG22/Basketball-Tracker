package com.example.basketballtracker.features.stats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import com.example.basketballtracker.features.stats.state.SeasonStatsViewModel
import com.example.basketballtracker.features.stats.state.StatsDisplayMode
import com.example.basketballtracker.features.summary.ui.SummaryTopBar

private val StatsAccent = Color(0xFF2ECC71)
private val CardDark = Color(0xFF101820)
private val CardBorder = Color.White.copy(alpha = 0.10f)

@Composable
fun SeasonStatsScreen(
    viewModel: SeasonStatsViewModel,
    onBack: () -> Unit
) {
    val seasonStats by viewModel.seasonStats.collectAsStateWithLifecycle()
    var displayMode by remember { mutableStateOf(StatsDisplayMode.PER_GAME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
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
                    title = "SEASON STATS",
                    subTitle = "Team overview & stats leaders",
                    onBack = onBack,
                )
            }

            item {
                StatsModeDropdown(displayMode) {
                    displayMode = it
                }
            }

            if (seasonStats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No season stats yet",
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            } else {
                item {
                    TeamOverviewCard(
                        stats = seasonStats,
                        mode = displayMode
                    )
                }

                item {
                    Text(
                        text = "CATEGORY LEADERS",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }

                items(buildLeaderCategories(seasonStats, displayMode)) { category ->
                    LeaderCategoryCard(category)
                }
            }
        }
    }
}

@Composable
fun TeamOverviewCard(
    stats: List<PlayerSeasonStats>,
    mode: StatsDisplayMode
) {
    val gp = stats.maxOfOrNull { it.gp } ?: 0

    val pts = stats.sumOf { it.pts }
    val reb = stats.sumOf { it.rebTotal }
    val ast = stats.sumOf { it.ast }
    val stl = stats.sumOf { it.stl }
    val blk = stats.sumOf { it.blk }
    val tov = stats.sumOf { it.tov }
    val pf = stats.sumOf { it.pf }

    val fgm = stats.sumOf { it.fgm }
    val fga = stats.sumOf { it.fga }
    val threem = stats.sumOf { it.threem }
    val threea = stats.sumOf { it.threea }
    val ftm = stats.sumOf { it.ftm }
    val fta = stats.sumOf { it.fta }

    StatDashboardCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("TEAM OVERVIEW")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BigTeamStat(
                    title = "GAMES",
                    value = gp.toString(),
                    modifier = Modifier.weight(1f)
                )

                BigTeamStat(
                    title = if (mode == StatsDisplayMode.PER_GAME) "PPG" else "POINTS",
                    value = formatTeamStat(pts, gp, mode),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallTeamStat("REB", formatTeamStat(reb, gp, mode), Modifier.weight(1f))
                SmallTeamStat("AST", formatTeamStat(ast, gp, mode), Modifier.weight(1f))
                SmallTeamStat("STL", formatTeamStat(stl, gp, mode), Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallTeamStat("BLK", formatTeamStat(blk, gp, mode), Modifier.weight(1f))
                SmallTeamStat("TOV", formatTeamStat(tov, gp, mode), Modifier.weight(1f))
                SmallTeamStat("PF", formatTeamStat(pf, gp, mode), Modifier.weight(1f))
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            SectionTitle("SHOOTING")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShootingStat("FG%", formatPercent(fgm, fga), "$fgm/$fga", Modifier.weight(1f))
                ShootingStat(
                    "3P%",
                    formatPercent(threem, threea),
                    "$threem/$threea",
                    Modifier.weight(1f)
                )
                ShootingStat("FT%", formatPercent(ftm, fta), "$ftm/$fta", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BigTeamStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .background(
                Color.White.copy(alpha = 0.035f),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = value,
                color = StatsAccent,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun SmallTeamStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.035f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = value,
            color = StatsAccent,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun ShootingStat(
    title: String,
    value: String,
    attempts: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.035f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            color = StatsAccent,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )

        Text(
            text = attempts,
            color = Color.White.copy(alpha = 0.45f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

data class LeaderCategory(
    val title: String,
    val subtitle: String,
    val leaders: List<LeaderItem>
)

data class LeaderItem(
    val rank: Int,
    val playerName: String,
    val playerNumber: Int,
    val value: String
)

@Composable
fun LeaderCategoryCard(
    category: LeaderCategory
) {
    StatDashboardCard {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = category.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = category.subtitle,
                        color = Color.White.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "TOP 3",
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            category.leaders.forEach { leader ->
                LeaderPlayerRow(leader)
            }
        }
    }
}

@Composable
fun LeaderPlayerRow(
    leader: LeaderItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leader.rank.toString(),
                color = StatsAccent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leader.playerName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "#${leader.playerNumber}",
                color = Color.White.copy(alpha = 0.42f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = leader.value,
            color = StatsAccent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun StatDashboardCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        content()
    }
}

@Composable
fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .background(StatsAccent, RoundedCornerShape(50))
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

fun buildLeaderCategories(
    stats: List<PlayerSeasonStats>,
    mode: StatsDisplayMode
): List<LeaderCategory> {
    return listOf(
        LeaderCategory(
            title = "POINTS",
            subtitle = if (mode == StatsDisplayMode.PER_GAME) "PPG" else "TOTAL",
            leaders = stats.top3By(
                selector = { valueByMode(it.pts, it.gp, mode) },
                value = { formatStat(it.pts, it.gp, mode) }
            )
        ),
        LeaderCategory(
            title = "REBOUNDS",
            subtitle = if (mode == StatsDisplayMode.PER_GAME) "RPG" else "TOTAL",
            leaders = stats.top3By(
                selector = { valueByMode(it.rebTotal, it.gp, mode) },
                value = { formatStat(it.rebTotal, it.gp, mode) }
            )
        ),
        LeaderCategory(
            title = "ASSISTS",
            subtitle = if (mode == StatsDisplayMode.PER_GAME) "APG" else "TOTAL",
            leaders = stats.top3By(
                selector = { valueByMode(it.ast, it.gp, mode) },
                value = { formatStat(it.ast, it.gp, mode) }
            )
        ),
        LeaderCategory(
            title = "STEALS",
            subtitle = if (mode == StatsDisplayMode.PER_GAME) "SPG" else "TOTAL",
            leaders = stats.top3By(
                selector = { valueByMode(it.stl, it.gp, mode) },
                value = { formatStat(it.stl, it.gp, mode) }
            )
        ),
        LeaderCategory(
            title = "BLOCKS",
            subtitle = if (mode == StatsDisplayMode.PER_GAME) "BPG" else "TOTAL",
            leaders = stats.top3By(
                selector = { valueByMode(it.blk, it.gp, mode) },
                value = { formatStat(it.blk, it.gp, mode) }
            )
        ),
        LeaderCategory(
            title = "TURNOVERS",
            subtitle = if (mode == StatsDisplayMode.PER_GAME) "TOPG" else "TOTAL",
            leaders = stats.top3By(
                selector = { valueByMode(it.tov, it.gp, mode) },
                value = { formatStat(it.tov, it.gp, mode) }
            )
        ),
        LeaderCategory(
            title = "FG%",
            subtitle = "MIN 1 FGA",
            leaders = stats
                .filter { it.fga > 0 }
                .top3By(
                    selector = { it.fgm.toFloat() / it.fga },
                    value = { formatPercent(it.fgm, it.fga) }
                )
        ),
        LeaderCategory(
            title = "3P%",
            subtitle = "MIN 1 3PA",
            leaders = stats
                .filter { it.threea > 0 }
                .top3By(
                    selector = { it.threem.toFloat() / it.threea },
                    value = { formatPercent(it.threem, it.threea) }
                )
        ),
        LeaderCategory(
            title = "FT%",
            subtitle = "MIN 1 FTA",
            leaders = stats
                .filter { it.fta > 0 }
                .top3By(
                    selector = { it.ftm.toFloat() / it.fta },
                    value = { formatPercent(it.ftm, it.fta) }
                )
        )
    )
}

fun List<PlayerSeasonStats>.top3By(
    selector: (PlayerSeasonStats) -> Float,
    value: (PlayerSeasonStats) -> String
): List<LeaderItem> {
    return sortedByDescending(selector)
        .take(3)
        .mapIndexed { index, player ->
            LeaderItem(
                rank = index + 1,
                playerName = player.playerName,
                playerNumber = player.playerNumber,
                value = value(player)
            )
        }
}

fun valueByMode(
    value: Int,
    gamesPlayed: Int,
    mode: StatsDisplayMode
): Float {
    return if (mode == StatsDisplayMode.TOTAL || gamesPlayed <= 0) {
        value.toFloat()
    } else {
        value.toFloat() / gamesPlayed
    }
}

fun formatTeamStat(
    value: Int,
    gamesPlayed: Int,
    mode: StatsDisplayMode
): String {
    return if (mode == StatsDisplayMode.TOTAL || gamesPlayed <= 0) {
        value.toString()
    } else {
        "%.1f".format(value.toFloat() / gamesPlayed)
    }
}

fun formatPercent(
    made: Int,
    attempted: Int
): String {
    if (attempted == 0) return "0%"
    return "${((made.toFloat() / attempted) * 100).toInt()}%"
}

fun formatStat(value: Int, gamesPlayed: Int, mode: StatsDisplayMode): String {
    return if (mode == StatsDisplayMode.TOTAL || gamesPlayed <= 1) {
        value.toString()
    } else {
        "%.1f".format(value.toFloat() / gamesPlayed)
    }
}

@Composable
fun StatsModeDropdown(
    selectedMode: StatsDisplayMode,
    onModeSelected: (StatsDisplayMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Text(
                text = if (selectedMode == StatsDisplayMode.TOTAL) "Total" else "Per Game",
                color = Color.White
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Per Game") },
                onClick = {
                    onModeSelected(StatsDisplayMode.PER_GAME)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text("Total") },
                onClick = {
                    onModeSelected(StatsDisplayMode.TOTAL)
                    expanded = false
                }
            )
        }
    }
}