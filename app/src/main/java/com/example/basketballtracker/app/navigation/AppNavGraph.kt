package com.example.basketballtracker.app.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.combine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.basketballtracker.core.data.BasketballApp
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.SyncManager
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.history.state.GamesHistoryViewModel
import com.example.basketballtracker.features.history.ui.GamesHistoryScreen
import com.example.basketballtracker.features.home.ui.HomeScreen
import com.example.basketballtracker.features.home.ui.HomeViewModel
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.ui.LiveGameTabletScreen
import com.example.basketballtracker.features.livegame.ui.LiveGameViewModel
import com.example.basketballtracker.features.newgame.ui.NewGameScreen
import com.example.basketballtracker.features.players.data.PlayerDetailsRepository
import com.example.basketballtracker.features.players.data.PlayersRepository
import kotlinx.coroutines.flow.first
import com.example.basketballtracker.features.players.state.PlayersViewModel
import com.example.basketballtracker.features.players.ui.PlayerDetailsScreen
import com.example.basketballtracker.features.players.ui.PlayerDetailsViewModel
import com.example.basketballtracker.features.players.ui.RosterScreen
import com.example.basketballtracker.features.stats.data.SeasonStatsRepository
import com.example.basketballtracker.features.stats.state.SeasonStatsViewModel
import com.example.basketballtracker.features.stats.ui.SeasonStatsScreen
import com.example.basketballtracker.features.summary.ui.GameSummaryScreen
import com.example.basketballtracker.features.summary.ui.GameSummaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun AppNavGraph(
    nav: NavHostController,
    db: AppDatabase,
    gamesRepo: GamesRepository,
    liveRepo: LiveGameRepository,
    statsRepository: SeasonStatsRepository,
    quarterLengthDefault: Int = 600,
    syncManager: SyncManager,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as BasketballApp
    val playersRepo = remember { PlayersRepository(db.playerDao(), application::requestSync) }
    NavHost(navController = nav, startDestination = Routes.HOME, modifier = modifier) {

        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(gamesRepo, liveRepo, syncManager) as T
                }
            })
            HomeScreen(
                viewModel = homeViewModel,
                onNewGame = { nav.navigate(Routes.NEW_GAME) },
                onContinue = { gameId -> nav.navigate(Routes.live(gameId)) },
                onGameSummary = { gameId -> nav.navigate(Routes.summary(gameId)) },
                onHistory = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } }
            )
        }

        composable(Routes.NEW_GAME) {
            NewGameScreen(
                defaultQuarterLengthSec = quarterLengthDefault,
                gamesRepo = gamesRepo,
                playersRepo = playersRepo,
                rosterDao = db.rosterDao(),
                playerDao = db.playerDao(),
                gameDao = db.gameDao(),
                onStart = { gameId -> nav.navigate(Routes.live(gameId)) {
                    popUpTo(Routes.NEW_GAME) {
                        inclusive = true
                    }
                } },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.LIVE,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments!!.getLong("gameId")
            val liveGame by db.gameDao().observeGame(gameId).collectAsStateWithLifecycle(initialValue = null)
            val rosterPlayers by produceState<List<PlayerEntity>>(emptyList(), gameId) {
                combine(db.rosterDao().observeRosterPlayerIds(gameId), db.playerDao().observeAllPlayers()) { ids, all ->
                    all.filter { it.id in ids }
                }.collect { value = it }
            }

            if (rosterPlayers.isEmpty() || liveGame == null) {
                // אפשר להציג Loading קצר
                CircularProgressIndicator()
                return@composable
            }

            val app = LocalContext.current.applicationContext as BasketballApp


            val vm: LiveGameViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                key = "live-$gameId",
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return LiveGameViewModel(
                            repo = liveRepo,
                            gamesRepo = gamesRepo,
                            syncManager = app.syncManager,
                            gameId = gameId,
                            players = rosterPlayers,
                            quarterLengthSec = liveGame!!.quarterLengthSec
                        ) as T
                    }
                }
            )

            LaunchedEffect(rosterPlayers) { vm.updatePlayers(rosterPlayers) }
            LiveGameTabletScreen(vm = vm, onEndGameNavigate = {
                vm.endGame {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
                }
            })
        }

        composable(Routes.PLAYERS) {
            val vm = remember { PlayersViewModel(playersRepo) }
            val players by vm.players.collectAsStateWithLifecycle()

            RosterScreen(
                players = players,
                onPlayerClick = { playerId ->
                    nav.navigate(Routes.playerDetails(playerId))
                },
                onAddPlayer = { name, number, photoUri ->
                    vm.add(name, number, photoUri)
                },
                onEditPlayer = { id, name, number, photoUri ->
                    vm.update(id, name, number, photoUri)
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable("player_details/{playerId}") { backStackEntry ->

            val playerId = backStackEntry.arguments
                ?.getString("playerId")
                ?.toLongOrNull() ?: return@composable

            val vm = remember {
                PlayerDetailsViewModel(
                    PlayerDetailsRepository(
                        playerDao = db.playerDao(),
                        gameDao = db.gameDao(),
                        eventDao = db.eventDao()
                    )
                )
            }

            PlayerDetailsScreen(
                playerId = playerId,
                viewModel = vm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            val vm: GamesHistoryViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GamesHistoryViewModel(gamesRepo, liveRepo) as T
                }
            })

            GamesHistoryScreen(
                vm = vm,
                onGameClick = { gameId ->
                    nav.navigate(Routes.summary(gameId))
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.SUMMARY
        ) { backStackEntry ->

            val gameId = backStackEntry.arguments
                ?.getString("gameId")
                ?.toLongOrNull()
                ?: return@composable

            val vm = remember(gameId) {
                GameSummaryViewModel(
                    gameId = gameId,
                    db = db,
                    gamesRepo = gamesRepo,
                    liveRepo = liveRepo
                )
            }

            GameSummaryScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.STATS) {
            val vm = remember { SeasonStatsViewModel(statsRepository) }
            SeasonStatsScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
