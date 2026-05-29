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
    val isPlayoff: Boolean = false,
    val playoffStage: String? = null,
    val playoffGameNumber: Int? = null,
    val gameDateEpoch: Long,
    val createdAt: Long,
    val quarterLengthSec: Int = 600,
    val quartersCount: Int = 4,
    val teamScore: Int = 0,
    val opponentScore: Int = 0,
    val remoteId: String? = null,
    val syncStatus: String = "PENDING",
    val isDeleted: Boolean = false,
    val status: String = GameStatus.FINISHED,
    val currentPeriod: Int = 1,
    val clockSecRemaining: Int = 600,
    val isClockRunning: Boolean = false,
    val lastClockStartedAt: Long? = null,
)

object GameStatus {
    const val LIVE = "LIVE"
    const val FINISHED = "FINISHED"
}

enum class PlayoffStage {
    QUARTER_FINAL,
    SEMI_FINAL,
    FINAL
}