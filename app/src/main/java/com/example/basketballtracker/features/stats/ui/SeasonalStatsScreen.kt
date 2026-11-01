package com.example.basketballtracker.features.stats.ui

import com.example.basketballtracker.ui.theme.SportsColors
import com.example.basketballtracker.features.core.ui.components.StatStrip
import com.example.basketballtracker.features.core.ui.components.StatPairs
import com.example.basketballtracker.features.core.ui.components.ShootingTable
import com.example.basketballtracker.features.core.ui.components.ShootingFigures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Scaffold
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
import com.example.basketballtracker.features.core.ui.components.SectionTitle
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import com.example.basketballtracker.features.players.ui.PlayerPhoto
import com.example.basketballtracker.features.stats.state.SeasonStatsViewModel
import com.example.basketballtracker.features.stats.state.StatsDisplayMode
import com.example.basketballtracker.features.summary.ui.SummaryTopBar

private val StatsAccent = SportsColors.Accent
private val CardDark = SportsColors.Surface
private val CardBorder = SportsColors.Text.copy(alpha = 0.10f)

@Composable
fun SeasonStatsScreen(
    viewModel: SeasonStatsViewModel,
    onBack: () -> Unit
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    var season by remember { mutableStateOf(com.example.basketballtracker.core.data.db.entities.GameSeason.CURRENT) }
    val seasonStats = remember(data, season) { data.stats(season) }
    val games = data.games.filter { it.season == season }
    val gameIds = games.map { it.id }.toSet()
    val shots = data.events.filter { it.gameId in gameIds }
    var displayMode by remember { mutableStateOf(StatsDisplayMode.PER_GAME) }

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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val leaderColumns = if (maxWidth >= 600.dp) 3 else 1
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SummaryTopBar(
                        title = "SEASON STATS",
                        subTitle = "Team overview & stats leaders",
                        onBack = onBack,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
    //                item {
    //                    SummaryTopBar(
    //                        title = "SEASON STATS",
    //                        subTitle = "Team overview & stats leaders",
    //                        onBack = onBack,
    //                    )
    //                }

                        item {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expanded = true }) { Text("Season $season") }
                                DropdownMenu(expanded, { expanded = false }) {
                                    data.seasons.forEach { value ->
                                        DropdownMenuItem(text = { Text(value) }, onClick = { season = value; expanded = false })
                                    }
                                }
                            }
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
                                        color = SportsColors.Muted
                                    )
                                }
                            }
                            item { com.example.basketballtracker.features.core.ui.components.TeamShotMap(shots) }
                        } else {
                            item {
                                com.example.basketballtracker.features.core.ui.components.TeamOverviewWithShotMap(shots) {
                                    TeamOverviewCard(
                                        stats = seasonStats,
                                        mode = displayMode,
                                        gameCount = games.size,
                                        teamPoints = data.teamPoints(season),
                                        recordedGameCount = data.recordedGameCount(season)
                                    )
                                }
                            }

                            item {
                                Text(
                                    text = "CATEGORY LEADERS",
                                    color = SportsColors.Text,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            items(buildLeaderCategories(seasonStats, displayMode).chunked(leaderColumns)) { categories ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    categories.forEach { category ->
                                        Box(Modifier.weight(1f)) {
                                            LeaderCategoryCard(category)
                                        }
                                    }
                                    repeat(leaderColumns - categories.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamOverviewCard(
    stats: List<PlayerSeasonStats>,
    mode: StatsDisplayMode,
    gameCount: Int = stats.maxOfOrNull { it.gp } ?: 0,
    teamPoints: Int = stats.sumOf { it.pts },
    recordedGameCount: Int = gameCount
) {
    val gp = gameCount

    val pts = teamPoints
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
            Text("$gp games · " + if (mode == StatsDisplayMode.PER_GAME) "Per-game averages" else "Season totals",
                color = SportsColors.Muted, style = MaterialTheme.typography.bodySmall)
            StatStrip(listOf(
                "POINTS" to formatTeamStat(pts, gp, mode),
                "REBOUNDS" to formatRecordedTeamStat(reb, recordedGameCount, mode),
                "ASSISTS" to formatRecordedTeamStat(ast, recordedGameCount, mode)
            ))
            if (recordedGameCount < gp) {
                Text(
                    "Points: all $gp games. Other stats: $recordedGameCount games with recorded stats; missing games excluded.",
                    color = SportsColors.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ShootingTable(listOf(
                ShootingFigures("Field goals", fgm, fga),
                ShootingFigures("3-pointers", threem, threea),
                ShootingFigures("Free throws", ftm, fta)
            ))
            StatPairs(listOf("Steals" to formatRecordedTeamStat(stl, recordedGameCount, mode),
                "Blocks" to formatRecordedTeamStat(blk, recordedGameCount, mode),
                "Turnovers" to formatRecordedTeamStat(tov, recordedGameCount, mode),
                "Fouls" to formatRecordedTeamStat(pf, recordedGameCount, mode)))
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
                SportsColors.Text.copy(alpha = 0.035f),
                RoundedCornerShape(4.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = title,
                color = SportsColors.Muted,
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
                SportsColors.Text.copy(alpha = 0.035f),
                RoundedCornerShape(4.dp)
            )
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = SportsColors.Text,
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
                SportsColors.Text.copy(alpha = 0.035f),
                RoundedCornerShape(4.dp)
            )
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = SportsColors.Text,
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
            color = SportsColors.Muted,
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
    val value: String,
    val photoUri: String? = null
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        color = SportsColors.Text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = category.subtitle,
                        color = SportsColors.Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "TOP 3",
                    color = SportsColors.Muted,
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
        PlayerPhoto(
            photoUri = leader.photoUri,
            playerName = leader.playerName,
            modifier = Modifier.size(32.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leader.playerName,
                color = SportsColors.Text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "#${leader.playerNumber}",
                color = SportsColors.Text.copy(alpha = 0.42f),
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
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        content()
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
                value = value(player),
                photoUri = player.photoUri
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

internal fun formatRecordedTeamStat(value: Int, games: Int, mode: StatsDisplayMode): String =
    if (games == 0) "—" else formatTeamStat(value, games, mode)

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
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, SportsColors.Text.copy(alpha = 0.12f))
        ) {
            Text(
                text = if (selectedMode == StatsDisplayMode.TOTAL) "Total" else "Per Game",
                color = SportsColors.Text
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = SportsColors.Muted
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
