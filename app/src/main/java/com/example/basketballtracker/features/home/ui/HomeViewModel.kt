package com.example.basketballtracker.features.home.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.core.data.db.SyncManager
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameSeason
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.domain.computeTeamScore
import com.example.basketballtracker.features.livegame.domain.computeOppScore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val liveGames: List<GameEntity> = emptyList(),
    val recentGames: List<GameEntity> = emptyList(),
    val played: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0
)

internal fun buildHomeState(games: List<GameEntity>): HomeState {
    val active = games.filterNot { it.isDeleted }
    val finished = active.filter { it.status == GameStatus.FINISHED }
    val season = finished.filter { it.season == GameSeason.CURRENT }
    return HomeState(
        loading = false,
        liveGames = active.filter { it.status == GameStatus.LIVE }.sortedByDescending { it.createdAt },
        recentGames = finished.sortedWith(compareByDescending<GameEntity> { it.gameDateEpoch }
            .thenByDescending { it.createdAt }).take(3),
        played = season.size,
        wins = season.count { it.teamScore > it.opponentScore },
        losses = season.count { it.teamScore < it.opponentScore },
        ties = season.count { it.teamScore == it.opponentScore }
    )
}

class HomeViewModel(
    private val gamesRepo: GamesRepository,
    private val liveGameRepo: LiveGameRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    var isSyncing by mutableStateOf(false)
        private set
    var syncError by mutableStateOf<String?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val home = gamesRepo.observeGames().flatMapLatest { games ->
        val state = buildHomeState(games)
        val live = state.liveGames.firstOrNull()
        if (live == null) flowOf(state)
        else liveGameRepo.observeLiveEvents(live.id).map { events ->
            state.copy(liveGames = listOf(live.copy(teamScore = computeTeamScore(events),
                opponentScore = computeOppScore(events))) + state.liveGames.drop(1))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun manualSync() {
        if (isSyncing) return
        isSyncing = true
        syncError = null
        viewModelScope.launch {
            try {
                syncManager.syncPending()
                syncManager.fetchAllFromCloud()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                syncError = "Couldn't sync. Please try again."
            } finally {
                isSyncing = false
            }
        }
    }
}
