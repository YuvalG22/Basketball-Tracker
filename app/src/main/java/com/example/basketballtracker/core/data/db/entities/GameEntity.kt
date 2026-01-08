package com.example.basketballtracker.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opponentName: String,
    val roundNumber: Int,
    val gameDateEpoch: Long,
    val createdAt: Long,
    val quarterLengthSec: Int = 600,
    val quartersCount: Int = 4
)