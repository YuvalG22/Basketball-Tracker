package com.example.basketballtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
                BasketballTrackerTheme {
                    val ctx = LocalContext.current
                    val nav = rememberNavController()

                    val db = remember {
                        Room.databaseBuilder(ctx, AppDatabase::class.java, "basketball.db")
                            .addMigrations(MIGRATION_3_4)
                            .build()
                    }

                    val gamesRepo = remember { GamesRepository(db.gameDao()) }
                    val liveRepo = remember { LiveGameRepository(db.eventDao()) }

                    AppNavGraph(
                        nav = nav,
                        db = db,
                        gamesRepo = gamesRepo,
                        liveRepo = liveRepo,
                        quarterLengthDefault = 10
                    )
                }
            }
        }
    }