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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.basketballtracker.app.navigation.AppNavGraph
import com.example.basketballtracker.core.data.BasketballApp
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
import com.example.basketballtracker.core.data.db.SyncWorker
import com.example.basketballtracker.core.data.remote.RetrofitClient
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.stats.data.SeasonStatsRepository
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setupBackgroundSync()

        setContent {
            BasketballTrackerTheme {
                val nav = rememberNavController()

                val app = LocalContext.current.applicationContext as BasketballApp
                val db = app.database

                AppNavGraph(
                    nav = nav,
                    db = db,
                    gamesRepo = app.gamesRepo,
                    liveRepo = app.liveRepo,
                    statsRepository = app.statsRepo,
                    quarterLengthDefault = 600
                )
            }
        }
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BasketballSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}