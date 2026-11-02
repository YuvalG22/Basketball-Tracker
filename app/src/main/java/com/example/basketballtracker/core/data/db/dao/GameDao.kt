package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.core.data.db.entities.GameSeason
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT remoteId FROM games WHERE syncStatus = 'SYNCED' AND remoteId IS NOT NULL")
    suspend fun syncedRemoteIds(): List<String>
    @Query("""UPDATE games SET isDeleted = 1, isClockRunning = 0 WHERE remoteId IN (:ids) AND syncStatus = 'SYNCED'
        AND NOT EXISTS (SELECT 1 FROM events WHERE gameId = games.id AND syncStatus IN ('PENDING','UPLOADING','PENDING_DELETE'))
        AND NOT EXISTS (SELECT 1 FROM roster WHERE gameId = games.id AND syncStatus = 'PENDING')
        AND NOT EXISTS (SELECT 1 FROM pending_actions WHERE gameId = games.id)""")
    suspend fun markRemoteMissing(ids: List<String>)
    @Query("""SELECT EXISTS(SELECT 1 FROM events WHERE gameId = :id AND syncStatus IN ('PENDING','UPLOADING','PENDING_DELETE'))
        OR EXISTS(SELECT 1 FROM roster WHERE gameId = :id AND syncStatus = 'PENDING')
        OR EXISTS(SELECT 1 FROM pending_actions WHERE gameId = :id)""")
    suspend fun hasPendingChildren(id: Long): Boolean

    @Query("UPDATE games SET lastAttemptKey = :key WHERE id = :id")
    suspend fun markAttempt(id: Long, key: String)
    @Query("UPDATE games SET lastAttemptKey = NULL WHERE id = :id AND lastAttemptKey = :key")
    suspend fun clearAttempt(id: Long, key: String)

    @Query("UPDATE games SET serverVersion = MAX(serverVersion, :version) WHERE id = :id")
    suspend fun acknowledgeEventVersion(id: Long, version: Long)

    @Query("SELECT * FROM games WHERE syncKey = :key LIMIT 1")
    suspend fun getBySyncKey(key: String): GameEntity?

    @Transaction
    suspend fun upsertFromCloud(games: List<GameEntity>) {
        games.forEach { game ->
            val old = getLocalIdByRemoteId(game.remoteId)?.let { getById(it) } ?: getBySyncKey(game.syncKey)
            if (old == null) insert(game)
            else if (old.syncStatus == "SYNCED") {
                check(!game.isDeleted || !hasPendingChildren(old.id)) { "Game deleted remotely; local actions are kept for review." }
                update(game.copy(id = old.id,
                syncKey = old.syncKey, localRevision = old.localRevision, mutationKey = old.mutationKey, lastAttemptKey = old.lastAttemptKey,
                season = if (game.season.isBlank()) old.season else game.season))
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
    SET teamScore = :teamScore, opponentScore = :opponentScore, syncStatus = 'PENDING', localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16)))
    WHERE id = :gameId
"""
    )
    suspend fun updateGameResult(
        gameId: Long,
        teamScore: Int,
        opponentScore: Int,
    )

    @Query("UPDATE games SET syncStatus = 'SYNCED' WHERE id = :gameId AND isDeleted = 1")
    suspend fun markDeleteSynced(gameId: Long)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteById(gameId: Long)

    @Query(
        """
    UPDATE games 
    SET syncStatus = CASE WHEN localRevision = :expectedRevision AND :expectedRevision = :appliedRevision
        THEN 'SYNCED' ELSE 'PENDING' END, remoteId = :remoteId, serverVersion = MAX(serverVersion, :serverVersion)
    WHERE id = :localId
    """
    )
    suspend fun markSynced(localId: Long, remoteId: String, expectedRevision: Long = 0, appliedRevision: Long = expectedRevision, serverVersion: Long = 0)

    @Query("SELECT * FROM games WHERE syncStatus = 'PENDING' AND isDeleted = 0")
    suspend fun getPendingGames(): List<GameEntity>

    @Query("SELECT * FROM games WHERE isDeleted = 0 ORDER BY gameDateEpoch DESC")
    fun getAllActiveGames(): Flow<List<GameEntity>>

    // 2. סימון מחיקה רכה
    @Query("UPDATE games SET isDeleted = 1, syncStatus = 'PENDING', localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16))) WHERE id = :gameId")
    suspend fun markAsDeleted(gameId: Long)

    // 3. שליפת אלו שממתינים למחיקה בענן
    @Query("SELECT * FROM games WHERE isDeleted = 1 AND syncStatus != 'SYNCED'")
    suspend fun getPendingDeletion(): List<GameEntity>

    @Query("SELECT * FROM games WHERE remoteId IS NOT NULL")
    suspend fun getAllWithRemoteIdNow(): List<GameEntity>

    @Query(
        """
    UPDATE games
    SET status = :status,
        syncStatus = 'PENDING', localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16)))
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
    syncStatus = 'PENDING', localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16)))
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
    currentPeriod = COALESCE(:period, currentPeriod),
    clockSecRemaining = COALESCE(:seconds, clockSecRemaining),
    lastClockStartedAt = NULL,
    isClockRunning = 0,
    syncStatus = 'PENDING', localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16)))
WHERE id = :gameId
"""
    )
    suspend fun finishGame(
        gameId: Long,
        teamScore: Int,
        opponentScore: Int,
        status: String,
        period: Int? = null,
        seconds: Int? = null
    )
}
