package com.example.basketballtracker.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.basketballtracker.core.data.db.dao.*
import com.example.basketballtracker.core.data.db.entities.*

@Database(
    entities = [PlayerEntity::class, GameEntity::class, RosterEntity::class, EventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun rosterDao(): RosterDao
    abstract fun eventDao(): EventDao
}