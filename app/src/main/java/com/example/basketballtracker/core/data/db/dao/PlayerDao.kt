package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT COUNT(*) FROM players")
    suspend fun countPlayers(): Int
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun observePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id IN (:ids) ORDER BY name ASC")
    fun getPlayersByIds(ids: List<Long>): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>): List<Long>

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: Long)
}