package com.example.basketballtracker.core.data.db.entities

import androidx.room.Entity

@Entity(
    tableName = "roster",
    primaryKeys = ["gameId", "playerId"]
)
data class RosterEntity(
    val gameId: Long,
    val playerId: Long
)