package com.example.basketballtracker.core.data.db.dao

import androidx.room.*
import com.example.basketballtracker.core.data.db.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(e: EventEntity): Long

    @Query("""
        SELECT * FROM events
        WHERE gameId = :gameId
        ORDER BY period ASC, clockSecRemaining DESC, createdAt ASC
    """)
    fun observeEvents(gameId: Long): Flow<List<EventEntity>>

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)

    @Query("""
        SELECT id FROM events
        WHERE gameId = :gameId
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    suspend fun getLastEventId(gameId: Long): Long?
}