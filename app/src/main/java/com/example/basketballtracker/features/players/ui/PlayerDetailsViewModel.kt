package com.example.basketballtracker.features.players.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.players.data.PlayerDetailsRepository
import com.example.basketballtracker.features.players.data.PlayerDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class PlayerDetailsViewModel(
    private val repository: PlayerDetailsRepository
) : ViewModel() {

    private val _playerId = MutableStateFlow<Long?>(null)
    private val _selectedPeriod = MutableStateFlow<Int?>(null)

    val selectedPeriod: StateFlow<Int?> = _selectedPeriod

    val uiState: StateFlow<PlayerDetailsUiState> =
        combine(
            _playerId.filterNotNull(),
            _selectedPeriod
        ) { playerId, selectedPeriod ->
            playerId to selectedPeriod
        }
            .flatMapLatest { (playerId, selectedPeriod) ->
                repository.observePlayerDetails(
                    playerId = playerId,
                    selectedPeriod = selectedPeriod
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PlayerDetailsUiState()
            )

    fun loadPlayer(playerId: Long) {
        _playerId.value = playerId
    }

    fun selectPeriod(period: Int?) {
        _selectedPeriod.value = period
    }
}