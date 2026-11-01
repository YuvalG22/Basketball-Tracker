package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE gameId = :gameId AND actionGroupId = :groupId AND syncStatus != 'PENDING_DELETE' ORDER BY id")
    suspend fun getGroup(gameId: Long, groupId: String): List<EventEntity>

    @Transaction
    suspend fun undoAction(gameId: Long): EventEntity? {
        val last = getLastEvent(gameId) ?: return null
        val group = last.actionGroupId?.let { getGroup(gameId, it) } ?: listOf(last)
        group.forEach { markPendingDelete(it.id) }
        return last
    }

    @Transaction
    suspend fun upsertEventsFromCloud(events: List<EventEntity>) {
        events.forEach { event ->
            // חיפוש לפי remoteId של האירוע
            val localId = getLocalIdByRemoteId(event.remoteId)
            if (localId != null) {
                val old = getById(localId)
                if (old?.syncStatus != "PENDING_DELETE") update(event.copy(id = localId, actionGroupId = old?.actionGroupId))
            } else {
                insert(event)
            }
        }
    }

    @Insert
    suspend fun insert(e: EventEntity): Long

    @Query("DELETE FROM events WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSyncedEvents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query("SELECT id FROM events WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLocalIdByRemoteId(remoteId: String?): Long?

    @Update
    suspend fun update(event: EventEntity)

    @Query(
        """
SELECT * FROM events
WHERE gameId = :gameId
AND syncStatus != 'PENDING_DELETE'
ORDER BY period ASC, clockSecRemaining DESC, createdAt ASC
"""
    )
    fun observeEvents(gameId: Long): Flow<List<EventEntity>>

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)

    @Query(
        """
        SELECT id FROM events
        WHERE gameId = :gameId
        ORDER BY createdAt DESC
        LIMIT 1
    """
    )
    suspend fun getLastEventId(gameId: Long): Long?

    @Query("SELECT * FROM events WHERE gameId = :gameId AND syncStatus != 'PENDING_DELETE' ORDER BY id DESC LIMIT 1")
    suspend fun getLastEvent(gameId: Long): EventEntity?

    @Query(
        """
    SELECT * FROM events
    WHERE gameId = :gameId AND syncStatus != 'PENDING_DELETE'
    ORDER BY period ASC, clockSecRemaining DESC, createdAt ASC
"""
    )
    suspend fun getEvents(gameId: Long): List<EventEntity>

    @Query("DELETE FROM events WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query(
        """
    SELECT * FROM events
    WHERE gameId = :gameId
      AND playerId = :playerId
      AND type IN ('TWO_MADE', 'TWO_MISS', 'THREE_MADE', 'THREE_MISS')
      AND shotX IS NOT NULL
      AND shotY IS NOT NULL
    ORDER BY createdAt ASC
"""
    )
    fun observeShotEventsForPlayer(
        gameId: Long,
        playerId: Long
    ): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE syncStatus = 'PENDING'")
    suspend fun getPendingEvents(): List<EventEntity>

    @Query("UPDATE events SET syncStatus = CASE WHEN syncStatus = 'PENDING_DELETE' THEN 'PENDING_DELETE' ELSE 'SYNCED' END, remoteId = :remoteId WHERE id = :localId")
    suspend fun markSynced(localId: Long, remoteId: String)

    @Query(
        """
        SELECT * FROM events
        WHERE playerId = :playerId
        ORDER BY gameId ASC, period ASC, clockSecRemaining DESC
    """
    )
    fun observeEventsByPlayer(playerId: Long): Flow<List<EventEntity>>

    @Query(
        """
UPDATE events
SET syncStatus = 'PENDING_DELETE'
WHERE id = :eventId
"""
    )
    suspend fun markPendingDelete(eventId: Long)

    @Query("SELECT * FROM events WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteEvents(): List<EventEntity>
}