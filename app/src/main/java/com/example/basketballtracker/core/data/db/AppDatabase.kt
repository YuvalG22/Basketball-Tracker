package com.example.basketballtracker.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.basketballtracker.core.data.db.dao.*
import com.example.basketballtracker.core.data.db.entities.*

@Database(
    entities = [PlayerEntity::class, GameEntity::class, RosterEntity::class, EventEntity::class, PlayerSeasonStatsEntity::class],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun rosterDao(): RosterDao
    abstract fun eventDao(): EventDao
    abstract fun playerSeasonStatsDao(): PlayerSeasonStatsDao

    companion object {
        fun getDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "basketball.db"
            )
                .addMigrations(
                    MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14
                )
                .build()
        }
    }
}