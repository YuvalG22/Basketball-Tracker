package com.example.basketballtracker.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Device-local, unconfirmed shot/assist selection. Never sent to the server. */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(@PrimaryKey val gameId: Long, val payload: String)
