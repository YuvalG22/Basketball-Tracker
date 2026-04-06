package com.example.basketballtracker.features.stats.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.core.ui.components.PlayerRowUi
import com.example.basketballtracker.features.core.ui.components.SeasonTotals
import com.example.basketballtracker.features.stats.data.SeasonStatsRepository
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class StatsMode { PER_GAME, TOTALS }

fun formatMode(mode: StatsMode): String =
    when (mode) {
        StatsMode.PER_GAME -> "Per Game"
        StatsMode.TOTALS -> "Totals"
    }

enum class StatsSort {
    PTS, AST, REB, DREB, OREB, STL, BLK, TOV, PF, FGM, THREEM, FTM,
    FG_PCT, THREE_PCT, FT_PCT, GP
}

data class SeasonStatsUiState(
    val mode: StatsMode = StatsMode.PER_GAME,
    val sort: StatsSort = StatsSort.PTS,
    val items: List<PlayerSeasonStats> = emptyList()
)

private fun pct(m: Int, a: Int): Int =
    if (a == 0) 0 else ((m.toDouble() / a.toDouble()) * 100.0).toInt()

fun PlayerSeasonStats.toRowUi(mode: StatsMode): PlayerRowUi {
    val gpSafe = gp.coerceAtLeast(1)

    fun perGame(v: Int): String =
        "%.1f".format(v.toDouble() / gpSafe)

    val ptsText = if (mode == StatsMode.PER_GAME) perGame(pts) else pts.toString()
    val astText = if (mode == StatsMode.PER_GAME) perGame(ast) else ast.toString()
    val rebText = if (mode == StatsMode.PER_GAME) perGame(rebTotal) else rebTotal.toString()
    val rebDefText = if (mode == StatsMode.PER_GAME) perGame(rebDef) else rebDef.toString()
    val rebOffText = if (mode == StatsMode.PER_GAME) perGame(rebOff) else rebOff.toString()
    val stlText = if (mode == StatsMode.PER_GAME) perGame(stl) else stl.toString()
    val blkText = if (mode == StatsMode.PER_GAME) perGame(blk) else blk.toString()
    val tovText = if (mode == StatsMode.PER_GAME) perGame(tov) else tov.toString()
    val pfText = if (mode == StatsMode.PER_GAME) perGame(pf) else pf.toString()

    return PlayerRowUi(
        number = playerNumber,
        name = playerName,

        games = gpSafe.toString(),

        pts = ptsText,
        ast = astText,
        reb = rebText,

        dreb = rebDefText,
        oreb = rebOffText,

        stl = stlText,
        blk = blkText,
        tov = tovText,

        fg = "$fgm/$fga",
        fgPct = "${pct(fgm, fga)}",
        three = "$threem/$threea",
        threePct = "${pct(threem, threea)}",
        ft = "$ftm/$fta",
        ftPct = "${pct(ftm, fta)}",

        pf = pfText,

        pm = "-"
    )
}

fun SeasonTotals.toTotalRowUi(mode: StatsMode): PlayerRowUi {
    val gpSafe = gp.coerceAtLeast(1)

    fun perGame(v: Int): String =
        "%.1f".format(v.toDouble() / gpSafe)

    val pts = if (mode == StatsMode.PER_GAME) perGame(pts) else pts.toString()
    val ast = if (mode == StatsMode.PER_GAME) perGame(ast) else ast.toString()
    val reb = if (mode == StatsMode.PER_GAME) perGame(reb) else reb.toString()
    val dreb = if (mode == StatsMode.PER_GAME) perGame(dreb) else dreb.toString()
    val oreb = if (mode == StatsMode.PER_GAME) perGame(oreb) else oreb.toString()
    val stl = if (mode == StatsMode.PER_GAME) perGame(stl) else stl.toString()
    val blk = if (mode == StatsMode.PER_GAME) perGame(blk) else blk.toString()
    val tov = if (mode == StatsMode.PER_GAME) perGame(tov) else tov.toString()
    val pf = if (mode == StatsMode.PER_GAME) perGame(pf) else pf.toString()
    return PlayerRowUi(
        number = 0,
        name = "TOTAL",
        games = gp.toString(),
        pts = pts,
        ast = ast,
        reb = reb,
        dreb = dreb,
        oreb = oreb,
        stl = stl,
        blk = blk,
        tov = tov,
        fg = "$fgm/$fga",
        fgPct = "${pct(fgm, fga)}",
        three = "$threem/$threea",
        threePct = "${pct(threem, threea)}",
        ft = "$ftm/$fta",
        ftPct = "${pct(ftm, fta)}",
        pf = pf,
        pm = "-"
    )
}

private fun PlayerSeasonStats.sortValue(sort: StatsSort, mode: StatsMode): Double {
    val gpSafe = gp.coerceAtLeast(1)

    fun perGame(v: Int) = v.toDouble() / gpSafe

    return when (sort) {
        StatsSort.PTS -> if (mode == StatsMode.PER_GAME) perGame(pts) else pts.toDouble()
        StatsSort.AST -> if (mode == StatsMode.PER_GAME) perGame(ast) else ast.toDouble()
        StatsSort.REB -> if (mode == StatsMode.PER_GAME) perGame(rebTotal) else rebTotal.toDouble()
        StatsSort.STL -> if (mode == StatsMode.PER_GAME) perGame(stl) else stl.toDouble()
        StatsSort.BLK -> if (mode == StatsMode.PER_GAME) perGame(blk) else blk.toDouble()
        StatsSort.TOV -> if (mode == StatsMode.PER_GAME) perGame(tov) else tov.toDouble()
        StatsSort.PF -> if (mode == StatsMode.PER_GAME) perGame(pf) else pf.toDouble()
        StatsSort.GP -> gp.toDouble()
        StatsSort.DREB -> if (mode == StatsMode.PER_GAME) perGame(rebDef) else rebDef.toDouble()
        StatsSort.OREB -> if (mode == StatsMode.PER_GAME) perGame(rebOff) else rebOff.toDouble()

        StatsSort.FGM -> if (mode == StatsMode.PER_GAME) perGame(fgm) else fgm.toDouble()
        StatsSort.FG_PCT -> pct(fgm, fga).toDouble()
        StatsSort.THREEM -> if (mode == StatsMode.PER_GAME) perGame(threem) else threem.toDouble()
        StatsSort.THREE_PCT -> pct(threem, threea).toDouble()
        StatsSort.FTM -> if (mode == StatsMode.PER_GAME) perGame(ftm) else ftm.toDouble()
        StatsSort.FT_PCT -> pct(ftm, fta).toDouble()
    }
}

class SeasonStatsViewModel(
    private val repo: SeasonStatsRepository
) : ViewModel() {
    private val mode = MutableStateFlow(StatsMode.PER_GAME)
    private val sort = MutableStateFlow(StatsSort.PTS)
    private val selectedGameId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<SeasonStatsUiState> = selectedGameId
        .flatMapLatest { id ->
            // 1. Switch between Season or Game data Flow
            if (id == null) repo.seasonStats() else repo.gameStats(id)
        }
        .combine(mode) { items, mode -> items to mode }
        .combine(sort) { (items, mode), sort ->
            // 2. Now 'items' is a real List, so you can sort it!
            val sorted = items.sortedByDescending { it.sortValue(sort, mode) }
            SeasonStatsUiState(
                mode = mode,
                sort = sort,
                items = sorted
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SeasonStatsUiState()
        )

    fun setMode(m: StatsMode) {
        mode.value = m
    }

    fun setSort(s: StatsSort) {
        sort.value = s
    }

    fun setSelectedGameId(id: Long?) {
        selectedGameId.value = id
    }
    fun isSelectedGameId(): Boolean {
        return selectedGameId.value != null
    }
}
