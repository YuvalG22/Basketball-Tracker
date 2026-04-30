package com.example.basketballtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.basketballtracker.app.navigation.AppNavGraph
import com.example.basketballtracker.core.data.db.AppDatabase
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
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            BasketballTrackerTheme {
                val ctx = LocalContext.current
                val nav = rememberNavController()

                val db = remember {
                    Room.databaseBuilder(ctx, AppDatabase::class.java, "basketball.db")
                        .addMigrations(
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10
                        )
                        .build()
                }

                val syncManager = remember {
                    SyncManager(
                        db.gameDao(),
                        db.playerDao(),
                        db.rosterDao(),
                        db.eventDao(),
                        RetrofitClient.gameApi,
                        RetrofitClient.playerApi,
                        RetrofitClient.rosterApi,
                        RetrofitClient.eventApi
                    )
                }
                LaunchedEffect(Unit) {
                    syncManager.syncPending()
                }

                val gamesRepo = remember { GamesRepository(db.gameDao(), RetrofitClient.gameApi) }
                val liveRepo =
                    remember { LiveGameRepository(db.eventDao(), RetrofitClient.eventApi) }
                val statsRepo = remember { SeasonStatsRepository(db.playerDao(), db.eventDao()) }

                AppNavGraph(
                    nav = nav,
                    db = db,
                    gamesRepo = gamesRepo,
                    liveRepo = liveRepo,
                    statsRepository = statsRepo,
                    quarterLengthDefault = 600
                )
            }
        }
    }
}