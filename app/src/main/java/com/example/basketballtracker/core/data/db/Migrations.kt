package com.example.basketballtracker.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE games 
            ADD COLUMN teamScore INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE games 
            ADD COLUMN opponentScore INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN teamScoreAtEvent INTEGER")
        db.execSQL("ALTER TABLE events ADD COLUMN opponentScoreAtEvent INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN isHomeGame INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN shotX REAL")
        db.execSQL("ALTER TABLE events ADD COLUMN shotY REAL")
        db.execSQL("ALTER TABLE events ADD COLUMN shotDistance REAL")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE events ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE games ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE players ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE players ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE roster ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE roster ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
    }
}