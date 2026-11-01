package com.example.basketballtracker.features.stats.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.stats.data.SeasonStatsRepository
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class StatsDisplayMode {
    TOTAL, PER_GAME
}

class SeasonStatsViewModel(
    private val repo: SeasonStatsRepository
) : ViewModel() {
    val data = repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.example.basketballtracker.features.stats.data.SeasonalData())
}
