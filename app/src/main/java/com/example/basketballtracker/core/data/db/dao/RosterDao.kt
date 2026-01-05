package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RosterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RosterEntity>)

    @Query("""
        SELECT playerId FROM roster
        WHERE gameId = :gameId
    """)
    fun observeRosterPlayerIds(gameId: Long): Flow<List<Long>>
}