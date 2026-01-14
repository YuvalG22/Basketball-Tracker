package com.example.basketballtracker.features.players.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.players.data.PlayersRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayersViewModel(private val repo: PlayersRepository) : ViewModel() {

    val players: StateFlow<List<PlayerEntity>> =
        repo.observePlayers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, number: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.addPlayer(trimmed, number) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deletePlayer(id) }
    }

    fun update(id: Long, name: String, number: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.updatePlayer(id, trimmed, number) }
    }
}