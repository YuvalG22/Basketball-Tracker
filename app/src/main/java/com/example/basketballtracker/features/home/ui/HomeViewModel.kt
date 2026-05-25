package com.example.basketballtracker.features.home.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.core.data.db.SyncManager
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val gamesRepo: GamesRepository,
    private val liveGameRepo: LiveGameRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    var isSyncing by mutableStateOf(false)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val lastGame: StateFlow<GameEntity?> =
        gamesRepo.observeLastGameId()
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    gamesRepo.observeGame(id)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val lastGameEvents: StateFlow<List<LiveEvent>> =
        lastGame
            .flatMapLatest { game ->
                if (game == null) {
                    flowOf(emptyList())
                } else {
                    liveGameRepo.observeLiveEvents(game.id)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun manualSync() {
        viewModelScope.launch {
            isSyncing = true
            try {
                syncManager.syncPending()
                syncManager.fetchAllFromCloud()
            } catch (e: Exception) {
                // טיפול בשגיאה (אופציונלי: להציג Toast)
            } finally {
                isSyncing = false
            }
        }
    }
}