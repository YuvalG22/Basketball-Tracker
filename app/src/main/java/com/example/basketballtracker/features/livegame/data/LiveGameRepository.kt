package com.example.basketballtracker.features.livegame.data

import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.features.livegame.ui.EventType
import com.example.basketballtracker.features.livegame.ui.LiveEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LiveGameRepository(
    private val eventDao: EventDao
) {
    fun observeLiveEvents(gameId: Long): Flow<List<LiveEvent>> =
        eventDao.observeEvents(gameId).map { list -> list.map { it.toDomain() } }

    suspend fun addEvent(
        gameId: Long,
        playerId: Long?,
        type: EventType,
        period: Int,
        clockSecRemaining: Int
    ) {
        eventDao.insert(
            EventEntity(
                gameId = gameId,
                playerId = playerId,
                type = type.name,
                period = period,
                clockSecRemaining = clockSecRemaining,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun undoLast(gameId: Long) {
        val lastId = eventDao.getLastEventId(gameId) ?: return
        eventDao.deleteById(lastId)
    }
}

private fun EventEntity.toDomain() = LiveEvent(
    id = id,
    gameId = gameId,
    playerId = playerId,
    type = EventType.valueOf(type),
    period = period,
    clockSecRemaining = clockSecRemaining,
    createdAt = createdAt
)