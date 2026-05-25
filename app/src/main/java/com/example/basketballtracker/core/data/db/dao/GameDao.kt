package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Transaction
    suspend fun upsertFromCloud(games: List<GameEntity>) {
        games.forEach { game ->
            val localId = getLocalIdByRemoteId(game.remoteId)
            if (localId != null) {
                update(game.copy(id = localId))
            } else {
                insert(game)
            }
        }
    }

    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("DELETE FROM games WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSyncedGames()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Query("SELECT id FROM games WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLocalIdByRemoteId(remoteId: String?): Long?

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GameEntity?

    @Query(
        """
    SELECT id FROM games
    ORDER BY createdAt DESC
    LIMIT 1
"""
    )
    fun observeLastGameId(): Flow<Long?>

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun observeAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    fun observeGame(id: Long): Flow<GameEntity?>

    @Query(
        """
    UPDATE games
    SET teamScore = :teamScore, opponentScore = :opponentScore
    WHERE id = :gameId
"""
    )
    suspend fun updateGameResult(
        gameId: Long,
        teamScore: Int,
        opponentScore: Int,
    )

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteById(gameId: Long)

    @Query(
        """
    UPDATE games 
    SET syncStatus = 'SYNCED', remoteId = :remoteId
    WHERE id = :localId
    """
    )
    suspend fun markSynced(localId: Long, remoteId: String)

    @Query("SELECT * FROM games WHERE syncStatus = 'PENDING'")
    suspend fun getPendingGames(): List<GameEntity>

    @Query("SELECT * FROM games WHERE isDeleted = 0 ORDER BY gameDateEpoch DESC")
    fun getAllActiveGames(): Flow<List<GameEntity>>

    // 2. סימון מחיקה רכה
    @Query("UPDATE games SET isDeleted = 1, syncStatus = 'PENDING' WHERE id = :gameId")
    suspend fun markAsDeleted(gameId: Long)

    // 3. שליפת אלו שממתינים למחיקה בענן
    @Query("SELECT * FROM games WHERE isDeleted = 1")
    suspend fun getPendingDeletion(): List<GameEntity>

    @Query("SELECT * FROM games WHERE remoteId IS NOT NULL")
    suspend fun getAllWithRemoteIdNow(): List<GameEntity>

    @Query(
        """
    UPDATE games
    SET status = :status,
        syncStatus = 'PENDING'
    WHERE id = :gameId
"""
    )
    suspend fun updateGameStatus(gameId: Long, status: String)

    @Query(
        """
UPDATE games
SET currentPeriod = :period,
    clockSecRemaining = :secRemaining,
    isClockRunning = :isRunning,
    lastClockStartedAt = :lastStartedAt,
    syncStatus = 'PENDING'
WHERE id = :gameId
"""
    )
    suspend fun updateLiveState(
        gameId: Long,
        period: Int,
        secRemaining: Int,
        isRunning: Boolean,
        lastStartedAt: Long?
    )

    @Query(
        """
UPDATE games
SET teamScore = :teamScore,
    opponentScore = :opponentScore,
    status = :status,
    isClockRunning = 0,
    syncStatus = 'PENDING'
WHERE id = :gameId
"""
    )
    suspend fun finishGame(
        gameId: Long,
        teamScore: Int,
        opponentScore: Int,
        status: String
    )
}