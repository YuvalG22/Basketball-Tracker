package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Transaction
    suspend fun upsertPlayersFromCloud(players: List<PlayerEntity>) {
        players.forEach { player ->
            val localId = player.remoteId?.let { getLocalIdByRemoteId(it) }
            if (localId != null) {
                val localPlayer = getPlayerById(localId)
                // A cloud refresh must not overwrite edits that have not uploaded yet.
                if (localPlayer?.syncStatus != "PENDING") {
                    // Photos belong to this device and are not part of the cloud payload.
                    update(player.copy(id = localId, photoUri = localPlayer?.photoUri))
                }
            } else {
                insert(player)
            }
        }
    }

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
    fun observePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id IN (:ids) ORDER BY name ASC")
    fun getPlayersByIds(ids: List<Long>): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun update(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>): List<Long>

    @Query("UPDATE players SET name = :name, number = :number WHERE id = :id")
    suspend fun updateById(id: Long, name: String, number: Int)

    @Query("""
        UPDATE players SET name = :name, number = :number, photoUri = :photoUri,
        syncStatus = CASE WHEN name != :name OR number != :number THEN 'PENDING' ELSE syncStatus END
        WHERE id = :id
    """)
    suspend fun updateDetailsById(id: Long, name: String, number: Int, photoUri: String?)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
UPDATE players 
SET syncStatus = 'SYNCED', remoteId = :remoteId 
WHERE id = :localId
"""
    )
    suspend fun markSynced(localId: Long, remoteId: String)

    @Query("SELECT * FROM players WHERE syncStatus = 'PENDING'")
    suspend fun getPendingPlayers(): List<PlayerEntity>
}
