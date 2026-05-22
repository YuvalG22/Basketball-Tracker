package com.example.basketballtracker.features.players.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.players.data.PlayerDetailsRepository
import com.example.basketballtracker.features.players.data.PlayerDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class PlayerDetailsViewModel(
    private val repository: PlayerDetailsRepository
) : ViewModel() {

    private val _playerId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<PlayerDetailsUiState> =
        _playerId
            .filterNotNull()
            .flatMapLatest { playerId ->
                repository.observePlayerDetails(playerId)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PlayerDetailsUiState()
            )

    fun loadPlayer(playerId: Long) {
        _playerId.value = playerId
    }
}