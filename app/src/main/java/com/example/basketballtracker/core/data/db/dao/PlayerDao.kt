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

    @Query("UPDATE players SET name = :name, number = :number WHERE id = :id")
    suspend fun updateById(id: Long, name: String, number: Int)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: Long)

//    @Query(
//        """
//    SELECT
//        p.id as playerId, p.name as playerName, p.number as playerNumber,
//        COUNT(DISTINCT e.gameId) as gamesPlayed,
//        SUM(CASE WHEN e.type = 'TWO_MADE' THEN 1 ELSE 0 END) as twoMade,
//        SUM(CASE WHEN e.type = 'TWO_MISS' THEN 1 ELSE 0 END) as twoMiss,
//        SUM(CASE WHEN e.type = 'THREE_MADE' THEN 1 ELSE 0 END) as threeMade,
//        SUM(CASE WHEN e.type = 'THREE_MISS' THEN 1 ELSE 0 END) as threeMiss,
//        SUM(CASE WHEN e.type = 'FT_MADE' THEN 1 ELSE 0 END) as ftMade,
//        SUM(CASE WHEN e.type = 'FT_MISS' THEN 1 ELSE 0 END) as ftMiss,
//        SUM(CASE WHEN e.type = 'AST' THEN 1 ELSE 0 END) as ast,
//        SUM(CASE WHEN e.type = 'REB_DEF' THEN 1 ELSE 0 END) as rebDef,
//        SUM(CASE WHEN e.type = 'REB_OFF' THEN 1 ELSE 0 END) as rebOff,
//        SUM(CASE WHEN e.type = 'STL' THEN 1 ELSE 0 END) as stl,
//        SUM(CASE WHEN e.type = 'BLK' THEN 1 ELSE 0 END) as blk,
//        SUM(CASE WHEN e.type = 'TOV' THEN 1 ELSE 0 END) as tov,
//        SUM(CASE WHEN e.type = 'PF' THEN 1 ELSE 0 END) as pf
//    FROM players p
//    LEFT JOIN events e ON p.id = e.playerId
//    WHERE (:gameId IS NULL OR e.gameId = :gameId)
//    GROUP BY p.id
//    HAVING gamesPlayed > 0
//"""
//    )
//    fun getPlayerStats(gameId: Long? = null): Flow<List<PlayerStatSummary>>
}