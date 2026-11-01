package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.features.home.ui.buildHomeState
import org.junit.Assert.*
import org.junit.Test

class HomeStateTest {
    private fun game(id: Long) = GameEntity(id = id, opponentName = "Team $id", roundNumber = 1,
        gameDateEpoch = id, createdAt = id, season = GameSeason.CURRENT, teamScore = 80, opponentScore = 70)

    @Test fun unfinishedGamesRemainReachableEvenWhenNewerFinishedGamesExist() {
        val state = buildHomeState(listOf(game(1).copy(status = GameStatus.LIVE), game(3),
            game(2).copy(status = GameStatus.LIVE), game(4).copy(status = GameStatus.LIVE, isDeleted = true)))
        assertEquals(listOf(2L, 1L), state.liveGames.map { it.id })
        assertEquals(listOf(3L), state.recentGames.map { it.id })
    }

    @Test fun seasonRecordExcludesLegacyDeletedAndLiveGamesAndCountsTiesSeparately() {
        val state = buildHomeState(listOf(game(1), game(2).copy(teamScore = 60),
            game(3).copy(teamScore = 70), game(4).copy(season = GameSeason.LEGACY),
            game(5).copy(isDeleted = true), game(6).copy(status = GameStatus.LIVE)))
        assertEquals(3, state.played)
        assertEquals(1, state.wins)
        assertEquals(1, state.losses)
        assertEquals(1, state.ties)
    }

    @Test fun recentResultsUseGameDateAndIncludePreviousSeasons() {
        val state = buildHomeState(listOf(game(1).copy(gameDateEpoch = 100, season = GameSeason.LEGACY),
            game(4), game(3), game(2)))
        assertEquals(listOf(1L, 4L, 3L), state.recentGames.map { it.id })
    }

    @Test fun emptyDatabaseFinishesLoadingWithoutInventingGameOrRecord() {
        val state = buildHomeState(emptyList())
        assertFalse(state.loading)
        assertTrue(state.liveGames.isEmpty())
        assertTrue(state.recentGames.isEmpty())
        assertEquals(0, state.played)
    }
}
