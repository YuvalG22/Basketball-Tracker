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

    ) {
    fun observeLiveEvents(gameId: Long): Flow<List<LiveEvent>> =
        eventDao.observeEvents(gameId).map { list -> list.map { it.toDomain() } }

    suspend fun addEvent(
        gameId: Long,
        playerId: Long?,
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

        val localId = eventDao.insert(event)

        try {
            val savedEvent = event.copy(id = localId)

            val game = gameDao.getById(savedEvent.gameId)
            val gameRemoteId = game?.remoteId ?: return

            val playerRemoteId = savedEvent.playerId?.let { id ->
                playerDao.getPlayerById(id)?.remoteId
            }

            val response = eventApi.uploadEvent(
                EventUploadDto(
                    localId = savedEvent.id,
                    gameId = savedEvent.gameId,
                    playerId = savedEvent.playerId,
                    gameRemoteId = gameRemoteId,
                    playerRemoteId = playerRemoteId,
                    type = savedEvent.type,
                    period = savedEvent.period,
                    clockSecRemaining = savedEvent.clockSecRemaining,
                    createdAt = savedEvent.createdAt,
                    teamScoreAtEvent = savedEvent.teamScoreAtEvent,
                    opponentScoreAtEvent = savedEvent.opponentScoreAtEvent,
                    shotX = savedEvent.shotX,
                    shotY = savedEvent.shotY,
                    shotDistance = savedEvent.shotDistance
                )
            )
            eventDao.markSynced(
                localId = localId,
                remoteId = response.remoteId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // נשאר PENDING → יסונכרן אחר כך
        }
    }

    suspend fun deleteEventsByGame(gameId: Long) {
        eventDao.deleteByGameId(gameId)
    }

    suspend fun getLiveEventsOnce(gameId: Long): List<LiveEvent> =
        eventDao.getEvents(gameId).map { it.toDomain() }


    suspend fun undoLast(gameId: Long) {
        val lastId = eventDao.getLastEventId(gameId) ?: return
        eventDao.deleteById(lastId)
    }

    suspend fun undoLastReturning(gameId: Long): LiveEvent? {
        val last = eventDao.getLastEvent(gameId) ?: return null
        eventDao.deleteById(last.id)
        return last.toDomain()
    }
}

private fun EventEntity.toDomain() = LiveEvent(
    id = id,
    gameId = gameId,
    playerId = playerId,
    type = EventType.valueOf(type),
    period = period,
    clockSecRemaining = clockSecRemaining,
    createdAt = createdAt,
    teamScoreAtEvent = teamScoreAtEvent,
    opponentScoreAtEvent = opponentScoreAtEvent,
    shotX = shotX,
    shotY = shotY,
    shotDistance = shotDistance
)