package com.example.basketballtracker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.core.graphics.toColorInt
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.basketballtracker.app.navigation.BottomNavBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setupBackgroundSync()

        setContent {
            BasketballTrackerTheme {
                val nav = rememberNavController()

                val app = LocalContext.current.applicationContext as BasketballApp
                val db = app.database

                val navBackStackEntry by nav.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isLiveScreen = currentRoute == "live/{gameId}"
                // או לפי ה-route האמיתי שלך

                SystemBarsController(
                    hideSystemBars = isLiveScreen
                )

                Scaffold(
                    bottomBar = {
                        if (currentRoute in listOf("home", "history", "players", "stats")) {
                            BottomNavBar(
                                navController = nav
                            )
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        nav = nav,
                        db = db,
                        gamesRepo = app.gamesRepo,
                        liveRepo = app.liveRepo,
                        statsRepository = app.statsRepo,
                        quarterLengthDefault = 600,
                        syncManager = app.syncManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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

@Composable
private fun SystemBarsController(
    hideSystemBars: Boolean
) {
    val view = LocalView.current

    LaunchedEffect(hideSystemBars) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (hideSystemBars) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}