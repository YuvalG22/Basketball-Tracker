package com.example.basketballtracker.core.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opponentName: String,
    val isHomeGame: Boolean = false,
    val roundNumber: Int,
    val gameDateEpoch: Long,
    val createdAt: Long,
    val quarterLengthSec: Int = 600,
    val quartersCount: Int = 4,
    val teamScore: Int = 0,
    val opponentScore: Int = 0,
    val remoteId: String? = null,
    val syncStatus: String = "PENDING",
    val isDeleted: Boolean = false
)