package com.example.basketballtracker.core.data.db.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "roster",
    primaryKeys = ["gameId", "playerId"],
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class RosterEntity(
    val gameId: Long,
    val playerId: Long,
    val remoteId: String? = null,
    val syncStatus: String = "PENDING"
)