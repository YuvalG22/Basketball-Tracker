package com.example.basketballtracker.features.summary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.PlayerBox
import com.example.basketballtracker.features.livegame.domain.computeBoxByPlayer
import com.example.basketballtracker.features.livegame.domain.computePlusMinusByPlayer
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedByPlayer
import com.example.basketballtracker.features.livegame.domain.formatMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.forEachIndexed

private val SummaryAccent = Color(0xFF2ECC71)

@Composable
fun GameSummaryScreen(
    gameId: Long,
    db: AppDatabase,
    gamesRepo: GamesRepository,
    liveRepo: LiveGameRepository,
    onBack: () -> Unit
) {
    val events by liveRepo.observeLiveEvents(gameId).collectAsState(initial = emptyList())

    val gameInfo by produceState<GameInfo?>(initialValue = null, key1 = gameId) {
        value = withContext(Dispatchers.IO) {
            val g = gamesRepo.getById(gameId) ?: return@withContext null
            val ids = db.rosterDao().observeRosterPlayerIds(gameId).first()
            val players = if (ids.isEmpty()) emptyList() else db.playerDao().getPlayersByIds(ids)

            GameInfo(
                opponentName = g.opponentName,
                opponentScore = g.opponentScore,
                teamScore = g.teamScore,
                roundNumber = g.roundNumber,
                gameDateEpoch = g.gameDateEpoch,
                quarterLengthSec = g.quarterLengthSec,
                quartersCount = g.quartersCount,
                players = players
            )
        }
    }

    val info = gameInfo ?: run {
        LoadingSummary(onBack)
        return
    }

    val box = remember(events) { computeBoxByPlayer(events, 600, 4, 0) }

    val secondsPlayedById = remember(events, info.quarterLengthSec, info.quartersCount) {
        computeSecondsPlayedByPlayer(
            events = events,
            quarterLengthSec = info.quarterLengthSec,
            currentPeriod = info.quartersCount,
            currentClockSecRemaining = 0
        )
    }

    val pmById = remember(events) { computePlusMinusByPlayer(events) }

    val teamTotals = remember(box, secondsPlayedById) {
        buildTeamTotals(box.values.toList(), secondsPlayedById)
    }

    val quarterScores = remember(events, info.quartersCount) {
        buildQuarterScores(events, info.quartersCount)
    }

    val teamScore = info.teamScore
    val opponentScore = info.opponentScore
    val isWin = teamScore > opponentScore

    val dateText = remember(info.gameDateEpoch) {
        if (info.gameDateEpoch == 0L) ""
        else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(info.gameDateEpoch))
    }

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
                    "GAME SUMMARY",
                    "Box score, team totals and game leaders",
                    onBack
                )
            }

            item {
                GameResultCard(
                    opponentName = info.opponentName,
                    roundNumber = info.roundNumber,
                    dateText = dateText,
                    teamScore = teamScore,
                    opponentScore = opponentScore,
                    isWin = isWin,
                    quarterScores = quarterScores
                )
            }

            item {
                TeamSummaryCard(teamTotals)
            }

//            item {
//                GameLeadersCard(
//                    players = info.players,
//                    box = box,
//                    secondsPlayedById = secondsPlayedById,
//                    pmById = pmById
//                )
//            }

            item {
                BoxScoreCard(
                    players = info.players,
                    box = box,
                    secondsPlayedById = secondsPlayedById,
                    pmById = pmById,
                    teamTotals = teamTotals,
                    events = events
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

            ShootingDonutsRow(teamTotals = t)
        }
    }
}

@Composable
private fun GameLeadersCard(
    players: List<PlayerEntity>,
    box: Map<Long, PlayerBox>,
    secondsPlayedById: Map<Long, Int>,
    pmById: Map<Long, Int>
) {
    val leaders = remember(players, box, secondsPlayedById, pmById) {
        listOf(
            GameLeader(
                "Points",
                players.bestBy(box) { it.pts }?.let { it to "${box[it.id]?.pts ?: 0}" }),
            GameLeader(
                "Rebounds",
                players.bestBy(box) { it.rebTotal }?.let { it to "${box[it.id]?.rebTotal ?: 0}" }),
            GameLeader(
                "Assists",
                players.bestBy(box) { it.ast }?.let { it to "${box[it.id]?.ast ?: 0}" }),
            GameLeader(
                "Steals",
                players.bestBy(box) { it.stl }?.let { it to "${box[it.id]?.stl ?: 0}" }),
            GameLeader(
                "Blocks",
                players.bestBy(box) { it.blk }?.let { it to "${box[it.id]?.blk ?: 0}" }),
            GameLeader("Minutes", players.maxByOrNull { secondsPlayedById[it.id] ?: 0 }
                ?.let { it to formatMinutes(secondsPlayedById[it.id] ?: 0) }),
            GameLeader("+/-", players.maxByOrNull { pmById[it.id] ?: 0 }
                ?.let {
                    val pm = pmById[it.id] ?: 0
                    it to if (pm > 0) "+$pm" else "$pm"
                })
        )
    }

    DashboardCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("GAME LEADERS")

            leaders.forEach { leader ->
                val data = leader.playerAndValue ?: return@forEach
                LeaderMiniRow(
                    title = leader.title,
                    player = data.first,
                    value = data.second
                )
            }
        }
    }
}

private data class GameLeader(
    val title: String,
    val playerAndValue: Pair<PlayerEntity, String>?
)

@Composable
private fun LeaderMiniRow(
    title: String,
    player: PlayerEntity,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.width(92.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "#${player.number}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = value,
            color = SummaryAccent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun BoxScoreCard(
    players: List<PlayerEntity>,
    box: Map<Long, PlayerBox>,
    secondsPlayedById: Map<Long, Int>,
    pmById: Map<Long, Int>,
    teamTotals: TeamTotals,
    events: List<LiveEvent>
) {
    val horizontalScrollState = rememberScrollState()
    val starterIds = remember(events) { detectStartersByCreatedAt(events) }

    val starters = remember(players, starterIds) {
        players.filter { it.id in starterIds }
    }

    val bench = remember(players, starterIds) {
        players.filter { it.id !in starterIds }
    }
    val minStatsWidth = 900.dp
    DashboardCard {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val statsWidth = (maxWidth - 170.dp).coerceAtLeast(minStatsWidth)
            Column {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    SectionTitle("BOX SCORE")
                }

                TableHeader(
                    scrollState = horizontalScrollState,
                    statsWidth = statsWidth
                )

                Column {
                    starters.forEachIndexed { index, p ->
                        SummaryPlayerRow(
                            player = p,
                            box = box[p.id],
                            sec = secondsPlayedById[p.id] ?: 0,
                            pm = pmById[p.id] ?: 0,
                            scrollState = horizontalScrollState,
                            statsWidth = statsWidth
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
                    }

                    if (starters.isNotEmpty() && bench.isNotEmpty()) {
                        BenchDivider()
                    }

                    bench.forEach { p ->
                        SummaryPlayerRow(
                            player = p,
                            box = box[p.id],
                            sec = secondsPlayedById[p.id] ?: 0,
                            pm = pmById[p.id] ?: 0,
                            scrollState = horizontalScrollState,
                            statsWidth = statsWidth
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
                    }

                    TeamTotalRow(
                        teamTotals,
                        scrollState = horizontalScrollState,
                        statsWidth = statsWidth
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryPlayerRow(
    player: PlayerEntity,
    box: PlayerBox?,
    sec: Int,
    pm: Int,
    scrollState: ScrollState,
    statsWidth: Dp
) {
    val pmText = if (pm > 0) "+$pm" else "$pm"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        // FIXED PLAYER INFO
        Column(
            modifier = Modifier
                .width(170.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = player.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Text(
                text = "#${player.number}",
                color = SummaryAccent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(statsWidth),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(formatMinutes(sec))
            StatCell("${box?.pts ?: 0}", accent = true)
            StatCell("${box?.rebTotal ?: 0}")
            StatCell("${box?.ast ?: 0}")
            StatCell("${box?.rebDef ?: 0}")
            StatCell("${box?.rebOff ?: 0}")
            StatCell("${box?.stl ?: 0}")
            StatCell("${box?.blk ?: 0}")
            StatCell("${box?.fgm ?: 0}/${box?.fga ?: 0}", false, 1.5f)
            StatCell("${box?.threem ?: 0}/${box?.threea ?: 0}", false, 1.5f)
            StatCell("${box?.ftm ?: 0}/${box?.fta ?: 0}", false, 1.5f)
            StatCell("${box?.tov ?: 0}")
            StatCell("${box?.pf ?: 0}")
            StatCell(pmText, accent = true)
        }
    }
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
fun TableHeader(
    scrollState: ScrollState,
    statsWidth: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(170.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "PLAYER",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(statsWidth)
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
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .background(SummaryAccent, RoundedCornerShape(50))
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
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
private fun BenchDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "BENCH",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun buildTeamTotals(
    boxes: List<PlayerBox>,
    secondsPlayedById: Map<Long, Int>
): TeamTotals {
    val fgm = boxes.sumOf { it.fgm }
    val fga = boxes.sumOf { it.fga }
    val threem = boxes.sumOf { it.threem }
    val threea = boxes.sumOf { it.threea }
    val ftm = boxes.sumOf { it.ftm }
    val fta = boxes.sumOf { it.fta }

    return TeamTotals(
        totalSec = secondsPlayedById.values.sum(),
        pts = boxes.sumOf { it.pts },
        ast = boxes.sumOf { it.ast },
        rebTotal = boxes.sumOf { it.rebTotal },
        rebDef = boxes.sumOf { it.rebDef },
        rebOff = boxes.sumOf { it.rebOff },
        stl = boxes.sumOf { it.stl },
        blk = boxes.sumOf { it.blk },
        tov = boxes.sumOf { it.tov },
        pf = boxes.sumOf { it.pf },
        fgm = fgm,
        fga = fga,
        threem = threem,
        threea = threea,
        ftm = ftm,
        fta = fta
    )
}

private fun List<PlayerEntity>.bestBy(
    box: Map<Long, PlayerBox>,
    selector: (PlayerBox) -> Int
): PlayerEntity? {
    return maxByOrNull { player ->
        selector(box[player.id] ?: return@maxByOrNull 0)
    }
}

private fun pct(made: Int, attempted: Int): Int {
    if (attempted == 0) return 0
    return ((made.toDouble() / attempted.toDouble()) * 100).toInt()
}

private data class GameInfo(
    val opponentName: String,
    val opponentScore: Int,
    val teamScore: Int,
    val roundNumber: Int,
    val gameDateEpoch: Long,
    val quarterLengthSec: Int,
    val quartersCount: Int,
    val players: List<PlayerEntity>
)

private data class TeamTotals(
    val totalSec: Int,
    val pts: Int,
    val ast: Int,
    val rebTotal: Int,
    val rebDef: Int,
    val rebOff: Int,
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

private fun detectStartersByCreatedAt(
    events: List<LiveEvent>
): Set<Long> {

    val sorted = events
        .asSequence()
        .filter {
            it.playerId != null &&
                    (it.type == EventType.SUB_IN ||
                            it.type == EventType.SUB_OUT)
        }
        .sortedBy { it.createdAt }
        .toList()

    val onCourt = LinkedHashSet<Long>()

    for (e in sorted) {
        val pid = e.playerId ?: continue

        when (e.type) {

            EventType.SUB_IN -> {
                onCourt.add(pid)

                if (onCourt.size == 5) {
                    return onCourt.toSet()
                }
            }

            EventType.SUB_OUT -> {
                onCourt.remove(pid)
            }

            else -> Unit
        }
    }

    return emptySet()
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
private fun TeamTotalRow(
    t: TeamTotals,
    scrollState: ScrollState,
    statsWidth: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(170.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "TOTAL",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(statsWidth)
        ) {
            StatCell("")
            StatCell("${t.pts}", accent = true)
            StatCell("${t.rebTotal}")
            StatCell("${t.ast}")
            StatCell("${t.rebDef}")
            StatCell("${t.rebOff}")
            StatCell("${t.stl}")
            StatCell("${t.blk}")
            StatCell("${t.fgm}/${t.fga}", false, 1.5f)
            StatCell("${t.threem}/${t.threea}", false, 1.5f)
            StatCell("${t.ftm}/${t.fta}", false, 1.5f)
            StatCell("${t.tov}")
            StatCell("${t.pf}")
            StatCell("")
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

private data class QuarterScore(
    val period: Int,
    val teamScore: Int,
    val opponentScore: Int
)

private fun buildQuarterScores(
    events: List<LiveEvent>,
    quartersCount: Int
): List<QuarterScore> {
    return (1..quartersCount).map { period ->
        val periodEvents = events
            .filter { it.period == period }
            .sortedBy { it.createdAt }

        val teamPoints = periodEvents.sumOf { event ->
            when (event.type) {
                EventType.TWO_MADE -> 2
                EventType.THREE_MADE -> 3
                EventType.FT_MADE -> 1
                else -> 0
            }
        }

        val opponentPoints = periodEvents.sumOf { event ->
            when (event.type) {
                EventType.OPP_TWO_MADE -> 2
                EventType.OPP_THREE_MADE -> 3
                EventType.OPP_FT_MADE -> 1
                else -> 0
            }
        }
        QuarterScore(
            period = period,
            teamScore = teamPoints,
            opponentScore = opponentPoints
        )
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