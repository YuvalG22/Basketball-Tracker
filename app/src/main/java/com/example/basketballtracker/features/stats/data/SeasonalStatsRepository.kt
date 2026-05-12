package com.example.basketballtracker.features.stats.data

import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.dao.PlayerSeasonStatsDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.mapper.toDomain
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SeasonStatsRepository(
    private val seasonStatsDao: PlayerSeasonStatsDao
) {
    fun seasonStats(): Flow<List<PlayerSeasonStats>> {
        return seasonStatsDao.observeSeasonStats()
            .map { list -> list.map { it.toDomain() } }
    }
}