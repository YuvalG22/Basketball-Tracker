package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.GameEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun observeAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    fun observeGame(id: Long): Flow<GameEntity?>

    @Query("""
    UPDATE games
    SET teamScore = :teamScore, opponentScore = :opponentScore
    WHERE id = :gameId
""")
    suspend fun updateGameResult(
        gameId: Long,
        teamScore: Int,
        opponentScore: Int,
    )

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteById(gameId: Long)
}