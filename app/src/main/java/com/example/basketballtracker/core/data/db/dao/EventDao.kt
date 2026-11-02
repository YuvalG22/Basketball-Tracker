package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT remoteId FROM events WHERE syncStatus = 'SYNCED' AND remoteId IS NOT NULL")
    suspend fun syncedRemoteIds(): List<String>
    @Query("UPDATE events SET syncStatus = 'DELETED' WHERE remoteId IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun markRemoteMissing(ids: List<String>)

    @Query("SELECT payload FROM pending_actions WHERE gameId = :gameId")
    suspend fun getDraft(gameId: Long): String?
    @Query("INSERT OR REPLACE INTO pending_actions(gameId,payload) VALUES(:gameId,:payload)")
    suspend fun saveDraft(gameId: Long, payload: String)
    @Query("DELETE FROM pending_actions WHERE gameId = :gameId")
    suspend fun clearDraft(gameId: Long)

    companion object {
        val scoringTypes = setOf("TWO_MADE", "THREE_MADE", "FT_MADE", "OPP_TWO_MADE", "OPP_THREE_MADE", "OPP_FT_MADE")
    }

    @Query("""
        UPDATE games SET
        teamScore = (SELECT COALESCE(SUM(CASE type WHEN 'TWO_MADE' THEN 2 WHEN 'THREE_MADE' THEN 3 WHEN 'FT_MADE' THEN 1 ELSE 0 END),0)
            FROM events WHERE gameId = :gameId AND syncStatus NOT IN ('PENDING_DELETE','DELETED')),
        opponentScore = (SELECT COALESCE(SUM(CASE type WHEN 'OPP_TWO_MADE' THEN 2 WHEN 'OPP_THREE_MADE' THEN 3 WHEN 'OPP_FT_MADE' THEN 1 ELSE 0 END),0)
            FROM events WHERE gameId = :gameId AND syncStatus NOT IN ('PENDING_DELETE','DELETED')),
        localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16))), syncStatus = 'PENDING'
        WHERE id = :gameId
    """)
    suspend fun refreshGameScore(gameId: Long)

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE gameId = :gameId AND actionGroupId = :groupId AND syncStatus NOT IN ('PENDING_DELETE', 'DELETED') ORDER BY id")
    suspend fun getGroup(gameId: Long, groupId: String): List<EventEntity>

    @Transaction
    suspend fun undoAction(gameId: Long): EventEntity? {
        val last = getLastEvent(gameId) ?: return null
        val group = last.actionGroupId?.let { getGroup(gameId, it) } ?: listOf(last)
        group.forEach { markPendingDelete(it.id) }
        if (group.any { it.type in scoringTypes }) refreshGameScore(gameId)
        return last
    }

    @Transaction
    suspend fun upsertEventsFromCloud(events: List<EventEntity>) {
        events.forEach { event ->
            // חיפוש לפי remoteId של האירוע
            val localId = getLocalIdByRemoteId(event.remoteId) ?: getBySyncKey(event.syncKey)?.id
            if (localId != null) {
                val old = getById(localId)
                if (old?.syncStatus == "SYNCED") update(event.copy(id = localId, syncKey = old.syncKey, actionGroupId = event.actionGroupId ?: old.actionGroupId))
            } else {
                insert(event)
            }
        }
    }

    @Query("SELECT * FROM events WHERE syncKey = :key LIMIT 1")
    suspend fun getBySyncKey(key: String): EventEntity?
    @Query("SELECT COALESCE(MAX(createdAt), 0) FROM events WHERE gameId = :gameId")
    suspend fun lastTimestamp(gameId: Long): Long
    @Query("SELECT EXISTS(SELECT 1 FROM games WHERE id = :gameId AND isDeleted = 0 AND status = 'LIVE')")
    suspend fun canRecord(gameId: Long): Boolean
    @Transaction
    suspend fun recordAction(events: List<EventEntity>) {
        if (events.isEmpty()) return
        require(events.all { it.gameId == events.first().gameId }) { "One game per action" }
        check(canRecord(events.first().gameId)) { "The game is closed or deleted. No action was saved." }
        var timestamp = lastTimestamp(events.first().gameId)
        val group = java.util.UUID.randomUUID().toString()
        insertAll(events.map {
            timestamp = maxOf(timestamp + 1, it.createdAt)
            it.copy(createdAt = timestamp, actionGroupId = it.actionGroupId ?: group)
        })
        if (events.any { it.type in scoringTypes }) refreshGameScore(events.first().gameId)
        if (events.any { it.type == "TWO_MADE" || it.type == "THREE_MADE" }) clearDraft(events.first().gameId)
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
AND syncStatus NOT IN ('PENDING_DELETE', 'DELETED')
ORDER BY period ASC, clockSecRemaining DESC, createdAt ASC, COALESCE(sourceOrder, id) ASC, COALESCE(remoteId, printf('%020d', id)) ASC, id ASC
"""
    )
    fun observeEvents(gameId: Long): Flow<List<EventEntity>>

    @Query("UPDATE events SET syncStatus = 'DELETED' WHERE id = :eventId")
    suspend fun markDeleteSynced(eventId: Long)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)

    @Query(
        """
        SELECT id FROM events
        WHERE gameId = :gameId AND syncStatus NOT IN ('PENDING_DELETE', 'DELETED')
        ORDER BY createdAt DESC, COALESCE(sourceOrder, id) DESC, id DESC
        LIMIT 1
    """
    )
    suspend fun getLastEventId(gameId: Long): Long?

    @Query("SELECT * FROM events WHERE gameId = :gameId AND syncStatus NOT IN ('PENDING_DELETE', 'DELETED') ORDER BY createdAt DESC, COALESCE(sourceOrder, id) DESC, COALESCE(remoteId, printf('%020d', id)) DESC, id DESC LIMIT 1")
    suspend fun getLastEvent(gameId: Long): EventEntity?

    @Query(
        """
    SELECT * FROM events
    WHERE gameId = :gameId AND syncStatus NOT IN ('PENDING_DELETE', 'DELETED')
    ORDER BY period ASC, clockSecRemaining DESC, createdAt ASC, COALESCE(sourceOrder, id) ASC, COALESCE(remoteId, printf('%020d', id)) ASC, id ASC
"""
    )
    suspend fun getEvents(gameId: Long): List<EventEntity>

    @Query("DELETE FROM events WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    @Query("SELECT * FROM events WHERE syncStatus NOT IN ('PENDING_DELETE', 'DELETED')")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query(
        """
    SELECT * FROM events
    WHERE gameId = :gameId
      AND playerId = :playerId
      AND syncStatus NOT IN ('PENDING_DELETE', 'DELETED')
      AND type IN ('TWO_MADE', 'TWO_MISS', 'THREE_MADE', 'THREE_MISS')
      AND shotX IS NOT NULL
      AND shotY IS NOT NULL
    ORDER BY createdAt ASC, COALESCE(sourceOrder, id) ASC, COALESCE(remoteId, printf('%020d', id)) ASC, id ASC
"""
    )
    fun observeShotEventsForPlayer(
        gameId: Long,
        playerId: Long
    ): Flow<List<EventEntity>>

    @Query("UPDATE events SET syncStatus = 'UPLOADING' WHERE id IN (:ids) AND syncStatus = 'PENDING'")
    suspend fun markUploading(ids: List<Long>)

    @Query("SELECT * FROM events WHERE syncStatus IN ('PENDING', 'UPLOADING') ORDER BY createdAt, id")
    suspend fun getPendingEvents(): List<EventEntity>

    @Query("UPDATE events SET syncStatus = CASE WHEN syncStatus = 'PENDING_DELETE' THEN 'PENDING_DELETE' ELSE 'SYNCED' END, remoteId = :remoteId WHERE id = :localId")
    suspend fun markSynced(localId: Long, remoteId: String)

    @Query(
        """
        SELECT * FROM events
        WHERE playerId = :playerId AND syncStatus NOT IN ('PENDING_DELETE', 'DELETED')
        ORDER BY gameId ASC, period ASC, clockSecRemaining DESC, createdAt ASC, COALESCE(sourceOrder, id) ASC, COALESCE(remoteId, printf('%020d', id)) ASC
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
