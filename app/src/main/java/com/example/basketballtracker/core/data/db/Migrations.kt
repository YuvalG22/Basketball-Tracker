package com.example.basketballtracker.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.impl.Migration_12_13

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

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_games_remoteId` ON `games` (`remoteId`)")

        // יצירת אינדקס ייחודי לטבלת players
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_players_remoteId` ON `players` (`remoteId`)")

        // יצירת אינדקס ייחודי לטבלת rosters
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_rosters_remoteId` ON `roster` (`remoteId`)")

        // יצירת אינדקס ייחודי לטבלת events
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_events_remoteId` ON `events` (`remoteId`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN shotZone TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS player_season_stats (
                playerId INTEGER NOT NULL PRIMARY KEY,
                playerName TEXT NOT NULL,
                playerNumber INTEGER NOT NULL,
                gp INTEGER NOT NULL,
                pts INTEGER NOT NULL,
                ast INTEGER NOT NULL,
                rebTotal INTEGER NOT NULL,
                rebDef INTEGER NOT NULL,
                rebOff INTEGER NOT NULL,
                stl INTEGER NOT NULL,
                blk INTEGER NOT NULL,
                tov INTEGER NOT NULL,
                pf INTEGER NOT NULL,
                fgm INTEGER NOT NULL,
                fga INTEGER NOT NULL,
                threem INTEGER NOT NULL,
                threea INTEGER NOT NULL,
                ftm INTEGER NOT NULL,
                fta INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE games
            ADD COLUMN status TEXT NOT NULL DEFAULT 'FINISHED'
            """.trimIndent()
        )
    }
}