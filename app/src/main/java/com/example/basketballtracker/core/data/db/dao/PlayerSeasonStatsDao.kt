package com.example.basketballtracker.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.basketballtracker.core.data.db.entities.PlayerSeasonStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerSeasonStatsDao {
    @Query("SELECT * FROM player_season_stats ORDER BY pts DESC")
    fun observeSeasonStats(): Flow<List<PlayerSeasonStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<PlayerSeasonStatsEntity>)

    @Query("DELETE FROM player_season_stats")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(stats: List<PlayerSeasonStatsEntity>) {
        clear()
        insertAll(stats)
    }
}