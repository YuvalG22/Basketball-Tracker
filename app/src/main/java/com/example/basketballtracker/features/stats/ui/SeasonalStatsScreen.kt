package com.example.basketballtracker.features.stats.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChipDefaults.filterChipColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.basketballtracker.features.core.ui.components.FilterChipsRow
import com.example.basketballtracker.features.core.ui.components.PlayerRowSeasonal
import com.example.basketballtracker.features.core.ui.components.PlayerRowUi
import com.example.basketballtracker.features.core.ui.components.SeasonTotals
import com.example.basketballtracker.features.core.ui.components.TableHeaderSeasonal
import com.example.basketballtracker.features.history.state.GamesHistoryViewModel
import com.example.basketballtracker.features.history.ui.GamesHistoryScreen
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import com.example.basketballtracker.features.stats.state.SeasonStatsViewModel
import com.example.basketballtracker.features.stats.state.StatsMode
import com.example.basketballtracker.features.stats.state.toRowUi
import com.example.basketballtracker.features.stats.state.toTotalRowUi

@Composable
fun SeasonStatsScreen(vm: SeasonStatsViewModel, gamesVm: GamesHistoryViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val totals = computeSeasonTotals(state.items)
    val totalRow = totals.toTotalRowUi(state.mode)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val filters = listOf(StatsMode.PER_GAME, StatsMode.TOTALS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.3f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                GamesHistoryScreen(
                    vm = gamesVm,
                    onGameClick = { id ->
                        vm.setSelectedGameId(id)
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.7f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!vm.isSelectedGameId()) {
                            FilterChipsRow(
                                filters = filters,
                                selected = state.mode,
                                onSelect = { vm.setMode(it) }
                            )
                        } else vm.setMode(StatsMode.TOTALS)
                    }
                    val scrollState = rememberScrollState()
                    TableHeaderSeasonal(
                        onSort = { vm.setSort(it) },
                        scrollState = scrollState
                    )
                    val rows = state.items.map { it.toRowUi(state.mode) }
                    val totalCount = rows.size + 1
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(rows) { i, row ->
                            PlayerRowSeasonal(
                                row = row,
                                index = i,
                                size = totalCount,
                                scrollState = scrollState
                            )
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 4.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    PlayerRowSeasonal(
                        row = totalRow,
                        index = rows.size,
                        size = totalCount,
                        scrollState = scrollState
                    )
                }
            }
        }
    }
}

private fun computeSeasonTotals(items: List<PlayerSeasonStats>): SeasonTotals {
    if (items.isEmpty()) {
        return SeasonTotals(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }
    return SeasonTotals(
        gp = items.maxOf { it.gp },
        pts = items.sumOf { it.pts },
        ast = items.sumOf { it.ast },
        reb = items.sumOf { it.rebTotal },
        dreb = items.sumOf { it.rebDef },
        oreb = items.sumOf { it.rebOff },
        stl = items.sumOf { it.stl },
        blk = items.sumOf { it.blk },
        tov = items.sumOf { it.tov },
        pf = items.sumOf { it.pf },
        fgm = items.sumOf { it.fgm },
        fga = items.sumOf { it.fga },
        threem = items.sumOf { it.threem },
        threea = items.sumOf { it.threea },
        ftm = items.sumOf { it.ftm },
        fta = items.sumOf { it.fta }
    )
}