package com.example.basketballtracker.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val playerId: Long?,
    val type: String,
    val period: Int,
    val clockSecRemaining: Int,
    val createdAt: Long,
    val teamScoreAtEvent: Int? = null,
    val opponentScoreAtEvent: Int? = null
)