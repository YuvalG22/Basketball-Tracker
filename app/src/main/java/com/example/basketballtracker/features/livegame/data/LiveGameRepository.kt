package com.example.basketballtracker.features.livegame.data

import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.events.EventApi
import com.example.basketballtracker.core.data.remote.events.EventUploadDto
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.ShotMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LiveGameRepository(
    private val eventDao: EventDao,
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val eventApi: EventApi,
    private val onChanged: () -> Unit = {},

    ) {
    suspend fun getDraft(gameId: Long) = eventDao.getDraft(gameId)
    suspend fun saveDraft(gameId: Long, payload: String) = eventDao.saveDraft(gameId, payload)
    suspend fun clearDraft(gameId: Long) = eventDao.clearDraft(gameId)

    fun observeEventEntities(gameId: Long): Flow<List<EventEntity>> = eventDao.observeEvents(gameId)

    fun observeLiveEvents(gameId: Long): Flow<List<LiveEvent>> =
        observeEventEntities(gameId).map { list -> list.map { it.toDomain() } }

    suspend fun addEvent(
        gameId: Long,
        playerId: Long?,
        assistedByPlayerId: Long? = null,
        type: EventType,
        period: Int,
        clockSecRemaining: Int,
        teamScoreAtEvent: Int? = null,
        opponentScoreAtEvent: Int? = null,
        shotMeta: ShotMeta?
    ) {
        val event = EventEntity(
            gameId = gameId,
            playerId = playerId,
            assistedByPlayerId = assistedByPlayerId,
            type = type.name,
            period = period,
            clockSecRemaining = clockSecRemaining,
            createdAt = System.currentTimeMillis(),
            teamScoreAtEvent = teamScoreAtEvent,
            opponentScoreAtEvent = opponentScoreAtEvent,
            shotX = shotMeta?.x,
            shotY = shotMeta?.y,
            shotDistance = shotMeta?.distance,
            syncStatus = "PENDING"
        )

        recordAction(listOf(event))
    }

    suspend fun recordAction(events: List<EventEntity>) {
        eventDao.recordAction(events)
        onChanged()
    }

    suspend fun deleteEventsByGame(gameId: Long) {
        eventDao.getEvents(gameId).forEach { eventDao.markPendingDelete(it.id) }
        onChanged()
    }

    suspend fun getLiveEventsOnce(gameId: Long): List<LiveEvent> =
        eventDao.getEvents(gameId).map { it.toDomain() }


    suspend fun undoLast(gameId: Long) {
        val lastId = eventDao.getLastEventId(gameId) ?: return
        eventDao.markPendingDelete(lastId)
        onChanged()
    }

    suspend fun undoLastReturning(gameId: Long): LiveEvent? {
        val result = eventDao.undoAction(gameId)?.toDomain()
        onChanged()
        return result
    }
}

internal fun EventEntity.toDomain() = LiveEvent(
    id = id,
    actionGroupId = actionGroupId,
    sourceOrder = sourceOrder ?: id,
    orderKey = remoteId ?: id.toString().padStart(20, '0'),
    gameId = gameId,
    playerId = playerId,
    assistedByPlayerId = assistedByPlayerId,
    type = EventType.valueOf(type),
    period = period,
    clockSecRemaining = clockSecRemaining,
    createdAt = createdAt,
    teamScoreAtEvent = teamScoreAtEvent,
    opponentScoreAtEvent = opponentScoreAtEvent,
    shotX = shotX,
    shotY = shotY,
    shotDistance = shotDistance,
)
