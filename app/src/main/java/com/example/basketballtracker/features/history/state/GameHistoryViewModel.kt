package com.example.basketballtracker.features.history.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameSeason
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GamesHistoryViewModel(
    private val gamesRepo: GamesRepository,
    private val repo: LiveGameRepository
) : ViewModel() {

    private val selectedSeason = MutableStateFlow<String?>(GameSeason.CURRENT)

    val history = combine(gamesRepo.observeGames(), selectedSeason, ::buildHistoryState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HistoryState()
        )

    fun selectSeason(season: String?) { selectedSeason.value = season }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            repo.deleteEventsByGame(gameId)
            gamesRepo.deleteGame(gameId)
        }
    }
}

data class HistoryState(
    val selectedSeason: String? = GameSeason.CURRENT,
    val seasons: List<String> = listOf(GameSeason.CURRENT),
    val games: List<GameEntity> = emptyList()
)

internal fun buildHistoryState(games: List<GameEntity>, season: String?): HistoryState {
    val activeGames = games.filterNot { it.isDeleted || it.status == com.example.basketballtracker.core.data.db.entities.GameStatus.SCHEDULED }
    return HistoryState(
        selectedSeason = season,
        seasons = (activeGames.map { it.season } + GameSeason.CURRENT + listOfNotNull(season))
            .distinct().sortedDescending(),
        games = activeGames.filter { season == null || it.season == season }
    )
}
