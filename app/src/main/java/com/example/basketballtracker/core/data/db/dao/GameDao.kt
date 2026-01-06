package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.GameEntity

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: GameEntity): Long

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GameEntity?

    @Query("""
    SELECT id FROM games
    ORDER BY createdAt DESC
    LIMIT 1
""")
    fun observeLastGameId(): kotlinx.coroutines.flow.Flow<Long?>
}