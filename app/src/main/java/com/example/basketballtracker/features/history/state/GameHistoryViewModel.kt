package com.example.basketballtracker.features.history.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.games.data.GamesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GamesHistoryViewModel(
    private val gamesRepo: GamesRepository
) : ViewModel() {

    val games = gamesRepo.observeGames()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            gamesRepo.deleteGame(gameId)
        }
    }
}
