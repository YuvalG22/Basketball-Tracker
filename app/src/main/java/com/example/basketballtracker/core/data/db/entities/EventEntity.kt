package com.example.basketballtracker.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [Index(value = ["remoteId"], unique = true), Index(value = ["syncKey"], unique = true)]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val playerId: Long?,
    val assistedByPlayerId: Long? = null,
    val type: String,
    val period: Int,
    val clockSecRemaining: Int,
    val createdAt: Long,
    val teamScoreAtEvent: Int? = null,
    val opponentScoreAtEvent: Int? = null,
    val shotX: Float? = null,
    val shotY: Float? = null,
    val shotDistance: Float? = null,
    val remoteId: String? = null,
    val syncStatus: String = "PENDING",
    val actionGroupId: String? = null,
    val sourceOrder: Long? = null,
    @ColumnInfo(defaultValue = "''") val syncKey: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val localRevision: Long = 0,
    @ColumnInfo(defaultValue = "0") val serverVersion: Long = 0,
)