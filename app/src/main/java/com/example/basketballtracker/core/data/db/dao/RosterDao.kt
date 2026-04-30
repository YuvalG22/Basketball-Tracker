package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RosterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RosterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RosterEntity>)

    @Query(
        """
        SELECT playerId FROM roster
        WHERE gameId = :gameId
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

    @Query("SELECT * FROM roster WHERE syncStatus = 'PENDING'")
    suspend fun getPendingRoster(): List<RosterEntity>
}