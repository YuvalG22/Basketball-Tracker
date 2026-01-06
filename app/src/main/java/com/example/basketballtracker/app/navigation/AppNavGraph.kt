package com.example.basketballtracker.app.navigation

import LiveGameTabletScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.home.ui.HomeScreen
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.state.LiveGameViewModel
import com.example.basketballtracker.features.newgame.ui.NewGameScreen
import com.example.basketballtracker.features.players.data.PlayersRepository
import kotlinx.coroutines.flow.first
import com.example.basketballtracker.features.players.state.PlayersViewModel
import com.example.basketballtracker.features.players.ui.PlayersScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun AppNavGraph(
    nav: NavHostController,
    db: AppDatabase,
    gamesRepo: GamesRepository,
    liveRepo: LiveGameRepository,
    quarterLengthDefault: Int = 600
) {
    // ✅ create repo here (MVP)
    val playersRepo = remember { PlayersRepository(db.playerDao()) }

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                gamesRepo = gamesRepo,
                onNewGame = { nav.navigate(Routes.NEW_GAME) },
                onContinue = { gameId -> nav.navigate(Routes.live(gameId)) },
                onPlayers = { nav.navigate(Routes.PLAYERS) }
            )
        }

        composable(Routes.NEW_GAME) {
            NewGameScreen(
                defaultQuarterLengthSec = quarterLengthDefault,
                gamesRepo = gamesRepo,
                playersRepo = playersRepo,
                rosterDao = db.rosterDao(),
                onStart = { gameId -> nav.navigate(Routes.live(gameId)) }
            )
        }

        composable(
            route = Routes.LIVE,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments!!.getLong("gameId")

            val rosterPlayers by produceState<List<PlayerEntity>>(
                initialValue = emptyList(),
                key1 = gameId
            ) {
                val ids = db.rosterDao().observeRosterPlayerIds(gameId).first()
                value = withContext(Dispatchers.IO) {
                    val ids = db.rosterDao().observeRosterPlayerIds(gameId).first()
                    if (ids.isEmpty()) emptyList()
                    else db.playerDao().getPlayersByIds(ids)
                }
            }

            val vm = remember(gameId, rosterPlayers) {
                LiveGameViewModel(
                    repo = liveRepo,
                    gameId = gameId,
                    players = rosterPlayers,
                    quarterLengthSec = quarterLengthDefault
                )
            }

            LiveGameTabletScreen(vm)
        }

        composable(Routes.PLAYERS) {
            val vm = remember { PlayersViewModel(playersRepo) }
            PlayersScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}