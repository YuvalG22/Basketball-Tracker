package com.example.basketballtracker.features.summary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.core.data.db.AppDatabase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.filter

class GameSummaryViewModel(
    private val gameId: Long,
    private val db: AppDatabase,
    private val gamesRepo: GamesRepository,
    private val liveRepo: LiveGameRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GameSummaryUiState> =
        liveRepo.observeLiveEvents(gameId)
            .mapLatest { events ->
                withContext(Dispatchers.IO) {
                    buildUiState(events)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = GameSummaryUiState(isLoading = true)
            )

    private suspend fun buildUiState(events: List<LiveEvent>): GameSummaryUiState {
        val game = gamesRepo.getById(gameId)
            ?: return GameSummaryUiState(isLoading = false)

        val rosterIds = db.rosterDao()
            .observeRosterPlayerIds(gameId)
            .first()

        val players = if (rosterIds.isEmpty()) {
            emptyList()
        } else {
            db.playerDao().getPlayersByIds(rosterIds)
        }

        val box = computeBoxByPlayer(
            events = events,
            quarterLengthSec = game.quarterLengthSec,
            currentPeriod = game.quartersCount,
            currentClockSecRemaining = 0
        )

        val secondsPlayedById = computeSecondsPlayedByPlayer(
            events = events,
            quarterLengthSec = game.quarterLengthSec,
            currentPeriod = game.quartersCount,
            currentClockSecRemaining = 0
        )

        val pmById = computePlusMinusByPlayer(events)

        val teamTotals = buildTeamTotals(
            boxes = box.values.toList(),
            secondsPlayedById = secondsPlayedById
        )

        val starterIds = detectStartersByCreatedAt(events)

        val starters = players.filter { it.id in starterIds }
        val bench = players.filter { it.id !in starterIds }

        val rows = (starters + bench).map { player ->
            val b = box[player.id]

            GameSummaryPlayerRowUi(
                playerId = player.id,
                playerName = player.name,
                playerNumber = player.number,
                isStarter = player.id in starterIds,
                min = formatMinutes(secondsPlayedById[player.id] ?: 0),
                pts = b?.pts ?: 0,
                reb = b?.rebTotal ?: 0,
                ast = b?.ast ?: 0,
                rebDef = b?.rebDef ?: 0,
                rebOff = b?.rebOff ?: 0,
                stl = b?.stl ?: 0,
                blk = b?.blk ?: 0,
                fgm = b?.fgm ?: 0,
                fga = b?.fga ?: 0,
                threem = b?.threem ?: 0,
                threea = b?.threea ?: 0,
                ftm = b?.ftm ?: 0,
                fta = b?.fta ?: 0,
                tov = b?.tov ?: 0,
                pf = b?.pf ?: 0,
                plusMinus = pmById[player.id] ?: 0
            )
        }

        return GameSummaryUiState(
            isLoading = false,
            opponentName = game.opponentName,
            roundNumber = game.roundNumber,
            dateText = formatDate(game.gameDateEpoch),
            teamScore = game.teamScore,
            opponentScore = game.opponentScore,
            isWin = game.teamScore > game.opponentScore,
            teamTotals = teamTotals,
            quarterScores = buildQuarterScores(events, game.quartersCount),
            rows = rows
        )
    }
}

private fun buildTeamTotals(
    boxes: List<PlayerBox>,
    secondsPlayedById: Map<Long, Int>
): TeamTotals {
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
        fgm = boxes.sumOf { it.fgm },
        fga = boxes.sumOf { it.fga },
        threem = boxes.sumOf { it.threem },
        threea = boxes.sumOf { it.threea },
        ftm = boxes.sumOf { it.ftm },
        fta = boxes.sumOf { it.fta }
    )
}

private fun buildQuarterScores(
    events: List<LiveEvent>,
    quartersCount: Int
): List<QuarterScore> {
    return (1..quartersCount).map { period ->
        val periodEvents = events.filter { it.period == period }

        QuarterScore(
            period = period,
            teamScore = periodEvents.sumOf {
                when (it.type) {
                    EventType.TWO_MADE -> 2
                    EventType.THREE_MADE -> 3
                    EventType.FT_MADE -> 1
                    else -> 0
                }
            },
            opponentScore = periodEvents.sumOf {
                when (it.type) {
                    EventType.OPP_TWO_MADE -> 2
                    EventType.OPP_THREE_MADE -> 3
                    EventType.OPP_FT_MADE -> 1
                    else -> 0
                }
            }
        )
    }
}

private fun detectStartersByCreatedAt(events: List<LiveEvent>): Set<Long> {
    val onCourt = LinkedHashSet<Long>()

    events
        .filter {
            it.playerId != null &&
                    (it.type == EventType.SUB_IN || it.type == EventType.SUB_OUT)
        }
        .sortedBy { it.createdAt }
        .forEach { event ->
            val pid = event.playerId ?: return@forEach

            when (event.type) {
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

private fun formatDate(epoch: Long): String {
    if (epoch == 0L) return ""
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .format(Date(epoch))
}

data class GameSummaryUiState(
    val isLoading: Boolean = false,
    val opponentName: String = "",
    val roundNumber: Int = 0,
    val dateText: String = "",
    val teamScore: Int = 0,
    val opponentScore: Int = 0,
    val isWin: Boolean = false,
    val teamTotals: TeamTotals? = null,
    val quarterScores: List<QuarterScore> = emptyList(),
    val rows: List<GameSummaryPlayerRowUi> = emptyList()
)

data class GameSummaryPlayerRowUi(
    val playerId: Long,
    val playerName: String,
    val playerNumber: Int,
    val isStarter: Boolean,
    val min: String,
    val pts: Int,
    val reb: Int,
    val ast: Int,
    val rebDef: Int,
    val rebOff: Int,
    val stl: Int,
    val blk: Int,
    val fgm: Int,
    val fga: Int,
    val threem: Int,
    val threea: Int,
    val ftm: Int,
    val fta: Int,
    val tov: Int,
    val pf: Int,
    val plusMinus: Int
)

data class TeamTotals(
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

data class QuarterScore(
    val period: Int,
    val teamScore: Int,
    val opponentScore: Int
)