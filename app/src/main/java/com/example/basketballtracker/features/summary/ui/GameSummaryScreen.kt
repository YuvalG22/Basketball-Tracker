package com.example.basketballtracker.features.summary.ui

import android.R
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.ui.EventType
import com.example.basketballtracker.features.livegame.ui.LiveEvent
import com.example.basketballtracker.features.livegame.ui.PlayerBox
import com.example.basketballtracker.features.livegame.ui.computeBoxByPlayer
import com.example.basketballtracker.features.livegame.ui.computePlusMinusByPlayer
import com.example.basketballtracker.features.livegame.ui.computeSecondsPlayedByPlayer
import com.example.basketballtracker.features.livegame.ui.formatMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun GameSummaryScreen(
    gameId: Long,
    db: AppDatabase,
    gamesRepo: GamesRepository,
    liveRepo: LiveGameRepository,
    onBack: () -> Unit
) {
    // Events (Flow) – מתעדכן אוטומטית
    val events by liveRepo.observeLiveEvents(gameId).collectAsState(initial = emptyList())

    val context = LocalContext.current
    val view = LocalView.current
    var tableBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }


    // Game info + roster players (טעינה IO חד-פעמית)
    val gameInfo by produceState<GameInfo?>(initialValue = null, key1 = gameId) {
        value = withContext(Dispatchers.IO) {
            val g = gamesRepo.getById(gameId) ?: return@withContext null
            val ids = db.rosterDao().observeRosterPlayerIds(gameId).first()
            val players = if (ids.isEmpty()) emptyList() else db.playerDao().getPlayersByIds(ids)
            GameInfo(
                opponentName = g.opponentName,
                roundNumber = g.roundNumber,
                gameDateEpoch = g.gameDateEpoch,
                quarterLengthSec = g.quarterLengthSec,
                quartersCount = g.quartersCount,
                players = players
            )
        }
    }

    val info = gameInfo
    if (info == null) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Loading summary...", style = MaterialTheme.typography.titleLarge)
                CircularProgressIndicator()
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
        return
    }

    val box = remember(events) { computeBoxByPlayer(events) }

    // דקות: “בסוף משחק” => period=quartersCount, clock=0
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
        val boxes = box.values

        val totalSec = secondsPlayedById.values.sum()

        val fgm = boxes.sumOf { it.fgm }
        val fga = boxes.sumOf { it.fga }
        val threem = boxes.sumOf { it.threem }
        val threea = boxes.sumOf { it.threea }
        val ftm = boxes.sumOf { it.ftm }
        val fta = boxes.sumOf { it.fta }

        TeamTotals(
            totalSec = totalSec,
            pts = boxes.sumOf { it.pts },
            ast = boxes.sumOf { it.ast },
            rebTotal = boxes.sumOf { it.rebTotal },
            rebDef = boxes.sumOf { it.rebDef },
            rebOff = boxes.sumOf { it.rebOff },
            stl = boxes.sumOf { it.stl },
            blk = boxes.sumOf { it.blk },
            tov = boxes.sumOf { it.tov },
            pf = boxes.sumOf { it.pf },
            fgm = fgm, fga = fga,
            threem = threem, threea = threea,
            ftm = ftm, fta = fta
        )
    }

    val dateText = remember(info.gameDateEpoch) {
        if (info.gameDateEpoch == 0L) "" else SimpleDateFormat("dd/MM/yyyy").format(Date(info.gameDateEpoch))
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Game Summary", style = MaterialTheme.typography.headlineSmall)
                }
                TextButton(
                    onClick = {
                        val bounds = tableBounds ?: return@TextButton

                        // צילום כל החלון
                        val full = view.drawToBitmap()

                        // חיתוך לאזור הטבלה (boundsInWindow -> פיקסלים)
                        val left = bounds.left.roundToInt().coerceIn(0, full.width - 1)
                        val top = bounds.top.roundToInt().coerceIn(0, full.height - 1)
                        val right = bounds.right.roundToInt().coerceIn(left + 1, full.width)
                        val bottom = bounds.bottom.roundToInt().coerceIn(top + 1, full.height)

                        val cropped =
                            Bitmap.createBitmap(full, left, top, right - left, bottom - top)

                        // שמירה לקובץ זמני
                        val file = File(context.cacheDir, "game_summary_${gameId}.png")
                        FileOutputStream(file).use { out ->
                            cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }

                        // שיתוף עם FileProvider
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(Intent.createChooser(shareIntent, "Share box score"))
                    }
                ) {
                    Text("Export PNG")
                }

            }

            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.onGloballyPositioned { coords ->
                    tableBounds = coords.boundsInWindow()
                }
            ) {
                Column() {
                    Text(
                        "Round ${info.roundNumber} • $dateText",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "vs ${info.opponentName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(10.dp))
                }
                TableHeader()

                val starterIds = remember(events) { detectStartersByCreatedAt(events) }

                val starters = remember(info.players, starterIds) {
                    info.players.filter { it.id in starterIds }
                }

                val bench = remember(info.players, starterIds) {
                    info.players.filter { it.id !in starterIds }
                }

                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(starters, key = { it.id }) { p ->
                        val b: PlayerBox? = box[p.id]
                        val sec = secondsPlayedById[p.id] ?: 0
                        val pm = pmById[p.id] ?: 0

                        PlayerRow(
                            gameId = gameId,
                            playerName = p.name,
                            playerNumber = p.number,
                            box = b,
                            sec = sec,
                            pm = pm
                        )
                    }
                    item { HorizontalDivider(thickness = 4.dp) }
                    items(bench, key = { it.id }) { p ->
                        val b: PlayerBox? = box[p.id]
                        val sec = secondsPlayedById[p.id] ?: 0
                        val pm = pmById[p.id] ?: 0

                        PlayerRow(
                            gameId = gameId,
                            playerName = p.name,
                            playerNumber = p.number,
                            box = b,
                            sec = sec,
                            pm = pm
                        )
                    }
                    item {
                        HorizontalDivider(thickness = 4.dp)
                        TeamTotalRow(teamTotals)
                    }

                }
            }
        }
    }
}

private data class GameInfo(
    val opponentName: String,
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


private fun detectStartersByCreatedAt(events: List<LiveEvent>): Set<Long> {
    val sorted = events
        .asSequence()
        .filter {
            it.playerId != null && (it.type == EventType.SUB_IN ||
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
                if (onCourt.size == 5) return onCourt.toSet()
            }

            EventType.SUB_OUT -> onCourt.remove(pid)
            else -> Unit
        }
    }
    return emptySet()
}


@Composable
fun TableHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("PLAYER", Modifier.weight(3f))
            Text("MIN", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("PTS", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("AST", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("REB", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("DREB", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("OREB", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("STL", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("BLK", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("TO", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("FG", Modifier.weight(2f), textAlign = TextAlign.Center)
            Text("3PT", Modifier.weight(2f), textAlign = TextAlign.Center)
            Text("FT", Modifier.weight(2f), textAlign = TextAlign.Center)
            Text("PF", Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("+/-", Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
    HorizontalDivider()
}

@Composable
fun PlayerRow(
    gameId: Long,
    playerName: String,
    playerNumber: Int,
    box: PlayerBox?,
    sec: Int,
    pm: Int
) {
    val minText = formatMinutes(sec)
    val pmText = if (pm > 0) "+$pm" else "$pm"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(Modifier.weight(3f)) {
            Text(
                playerNumber.toString(),
                Modifier.weight(1f),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(playerName, Modifier.weight(5f))
        }

        Text(minText, Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.pts ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.ast ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.rebTotal ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.rebDef ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.rebOff ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.stl ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.blk ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${box?.tov ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)

        Text(
            "${box?.fgm}/${box?.fga} (${box?.fgPct}%)",
            Modifier.weight(2f),
            textAlign = TextAlign.Center
        )
        Text(
            "${box?.threem}/${box?.threea} (${box?.threePct}%)",
            Modifier.weight(2f),
            textAlign = TextAlign.Center
        )
        Text(
            "${box?.ftm}/${box?.fta} (${box?.ftPct}%)",
            Modifier.weight(2f),
            textAlign = TextAlign.Center
        )

        Text("${box?.pf ?: 0}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(if (gameId.toInt() == 2) "" else pmText, Modifier.weight(1f), textAlign = TextAlign.Center)
    }
    HorizontalDivider()
}

@Composable
private fun TeamTotalRow(t: TeamTotals) {
    //val minText = formatMinutes(t.totalSec)

    fun pct(m: Int, a: Int): Int {
        if (a == 0) return 0
        return ((m.toDouble() / a.toDouble()) * 100.0).toInt()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            "TOTAL",
            Modifier.weight(3f),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text("", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.pts}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.ast}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.rebTotal}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.rebDef}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.rebOff}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.stl}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.blk}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${t.tov}", Modifier.weight(1f), textAlign = TextAlign.Center)

        Text(
            "${t.fgm}/${t.fga} (${pct(t.fgm, t.fga)}%)",
            Modifier.weight(2f),
            textAlign = TextAlign.Center
        )
        Text(
            "${t.threem}/${t.threea} (${pct(t.threem, t.threea)}%)",
            Modifier.weight(2f),
            textAlign = TextAlign.Center
        )
        Text(
            "${t.ftm}/${t.fta} (${pct(t.ftm, t.fta)}%)",
            Modifier.weight(2f),
            textAlign = TextAlign.Center
        )

        Text("${t.pf}", Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("", Modifier.weight(1f), textAlign = TextAlign.Center)
    }
    HorizontalDivider()
}

