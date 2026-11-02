package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("UPDATE players SET lastAttemptKey = :key WHERE id = :id")
    suspend fun markAttempt(id: Long, key: String)
    @Query("UPDATE players SET lastAttemptKey = NULL WHERE id = :id AND lastAttemptKey = :key")
    suspend fun clearAttempt(id: Long, key: String)

    @Transaction
    suspend fun upsertPlayersFromCloud(players: List<PlayerEntity>) {
        players.forEach { player ->
            val localId = player.remoteId?.let { getLocalIdByRemoteId(it) } ?: getBySyncKey(player.syncKey)?.id
            if (localId != null) {
                val localPlayer = getPlayerById(localId)
                // A cloud refresh must not overwrite edits that have not uploaded yet.
                if (localPlayer?.syncStatus == "SYNCED") {
                    // Photos belong to this device and are not part of the cloud payload.
                    update(player.copy(id = localId, photoUri = localPlayer.photoUri, syncKey = localPlayer.syncKey, localRevision = localPlayer.localRevision, mutationKey = localPlayer.mutationKey, lastAttemptKey = localPlayer.lastAttemptKey))
                }
            } else {
                insert(player)
            }
        }
    }

    @Query("SELECT * FROM players WHERE syncKey = :key LIMIT 1")
    suspend fun getBySyncKey(key: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :playerId LIMIT 1")
    suspend fun getPlayerById(playerId: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :playerId LIMIT 1")
    fun observePlayerById(playerId: Long): Flow<PlayerEntity?>

    @Query("SELECT COUNT(*) FROM players")
    suspend fun countPlayers(): Int

    @Query("DELETE FROM players WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSyncedPlayers()

    @Query("SELECT id FROM players WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLocalIdByRemoteId(remoteId: String?): Long?

    @Query("SELECT * FROM players ORDER BY name ASC")
    fun observeAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE isDeleted = 0 ORDER BY name ASC")
    fun observePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id IN (:ids) ORDER BY name ASC")
    fun getPlayersByIds(ids: List<Long>): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun update(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>): List<Long>

    @Query("UPDATE players SET name = :name, number = :number, syncStatus = 'PENDING', localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16))) WHERE id = :id")
    suspend fun updateById(id: Long, name: String, number: Int)

    @Query("""
        UPDATE players SET name = :name, number = :number, photoUri = :photoUri,
        mutationKey = CASE WHEN name != :name OR number != :number THEN lower(hex(randomblob(16))) ELSE mutationKey END,
        localRevision = CASE WHEN name != :name OR number != :number THEN localRevision + 1 ELSE localRevision END,
        syncStatus = CASE WHEN name != :name OR number != :number THEN 'PENDING' ELSE syncStatus END
        WHERE id = :id
    """)
    suspend fun updateDetailsById(id: Long, name: String, number: Int, photoUri: String?)

    @Query("UPDATE players SET isDeleted = 1, syncStatus = 'PENDING_DELETE', localRevision = localRevision + 1, mutationKey = lower(hex(randomblob(16))) WHERE id = :id")
    suspend fun markDeleted(id: Long)
    @Query("SELECT * FROM players WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(): List<PlayerEntity>
    @Query("UPDATE players SET syncStatus = 'SYNCED' WHERE id = :id AND isDeleted = 1")
    suspend fun markDeleteSynced(id: Long)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
UPDATE players
SET syncStatus = CASE WHEN syncStatus = 'PENDING_DELETE' THEN 'PENDING_DELETE'
 WHEN localRevision = :expectedRevision AND :expectedRevision = :appliedRevision THEN 'SYNCED' ELSE 'PENDING' END,
 remoteId = :remoteId, serverVersion = MAX(serverVersion, :serverVersion)
WHERE id = :localId
"""
    )
    suspend fun markSynced(localId: Long, remoteId: String, expectedRevision: Long = 0, appliedRevision: Long = expectedRevision, serverVersion: Long = 0)

    @Query("SELECT * FROM players WHERE syncStatus = 'PENDING'")
    suspend fun getPendingPlayers(): List<PlayerEntity>
}
