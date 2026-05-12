package com.example.basketballtracker.core.data

import android.app.Application
import androidx.room.Room
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.MIGRATION_10_11
import com.example.basketballtracker.core.data.db.MIGRATION_3_4
import com.example.basketballtracker.core.data.db.MIGRATION_4_5
import com.example.basketballtracker.core.data.db.MIGRATION_5_6
import com.example.basketballtracker.core.data.db.MIGRATION_6_7
import com.example.basketballtracker.core.data.db.MIGRATION_7_8
import com.example.basketballtracker.core.data.db.MIGRATION_8_9
import com.example.basketballtracker.core.data.db.MIGRATION_9_10
import com.example.basketballtracker.core.data.db.SyncManager
import com.example.basketballtracker.core.data.remote.RetrofitClient
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.stats.data.SeasonStatsRepository
import kotlin.getValue

class BasketballApp: Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    val syncManager by lazy {
        SyncManager(
            gameDao = database.gameDao(),
            playerDao = database.playerDao(),
            rosterDao = database.rosterDao(),
            eventDao = database.eventDao(),
            playerSeasonStatsDao = database.playerSeasonStatsDao(),
            gameApi = RetrofitClient.gameApi,
            playerApi = RetrofitClient.playerApi,
            rosterApi = RetrofitClient.rosterApi,
            eventApi = RetrofitClient.eventApi,
            statsApi = RetrofitClient.statsApi,
        )
    }

    val gamesRepo by lazy {
        GamesRepository(database.gameDao(), RetrofitClient.gameApi)
    }

    val liveRepo by lazy {
        LiveGameRepository(
            database.eventDao(),
            database.gameDao(),
            database.playerDao(),
            RetrofitClient.eventApi
        )
    }

    val statsRepo by lazy {
        SeasonStatsRepository(database.playerSeasonStatsDao())
    }
}