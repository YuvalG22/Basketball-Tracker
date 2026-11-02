package com.example.basketballtracker.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [Index(value = ["remoteId"], unique = true), Index(value = ["syncKey"], unique = true)]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val number: Int,
    val remoteId: String? = null,
    val syncStatus: String = "PENDING",
    val photoUri: String? = null,
    @ColumnInfo(defaultValue = "''") val syncKey: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "''") val mutationKey: String = java.util.UUID.randomUUID().toString(),
    val lastAttemptKey: String? = null,
    @ColumnInfo(defaultValue = "0") val localRevision: Long = 0,
    @ColumnInfo(defaultValue = "0") val serverVersion: Long = 0,
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false,
)
