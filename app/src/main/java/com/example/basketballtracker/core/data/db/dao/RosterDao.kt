package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RosterDao {
    @Query("SELECT remoteId FROM roster WHERE syncStatus = 'SYNCED' AND remoteId IS NOT NULL")
    suspend fun syncedRemoteIds(): List<String>
    @Query("UPDATE roster SET syncStatus = 'DELETED' WHERE remoteId IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun markRemoteMissing(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RosterEntity)

    @Query("DELETE FROM roster WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSyncedRoster()

    @Transaction
    suspend fun mergeFromCloud(items: List<RosterEntity>) {
        items.forEach { item ->
            val old = get(item.gameId, item.playerId)
            if (old == null || old.syncStatus == "SYNCED") insert(item)
        }
    }
    @Query("SELECT * FROM roster WHERE gameId = :gameId AND playerId = :playerId")
    suspend fun get(gameId: Long, playerId: Long): RosterEntity?
    @Query("UPDATE roster SET syncStatus = 'DELETED' WHERE gameId = :gameId AND playerId = :playerId AND syncStatus = 'SYNCED'")
    suspend fun removeFromCloud(gameId: Long, playerId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RosterEntity>)

    @Query(
        """
        SELECT playerId FROM roster
        WHERE gameId = :gameId AND syncStatus != 'DELETED'
    """
    )
    fun observeRosterPlayerIds(gameId: Long): Flow<List<Long>>

    @Query(
        """
UPDATE roster
SET syncStatus = 'SYNCED', remoteId = :remoteId
WHERE gameId = :gameId AND playerId = :playerId
"""
    )
    suspend fun markSynced(
        gameId: Long,
        playerId: Long,
        remoteId: String
    )

    @Query("UPDATE roster SET syncStatus = 'DELETED' WHERE gameId = :gameId")
    suspend fun markGameDeleted(gameId: Long)

    @Query("SELECT * FROM roster WHERE syncStatus = 'PENDING'")
    suspend fun getPendingRoster(): List<RosterEntity>
}