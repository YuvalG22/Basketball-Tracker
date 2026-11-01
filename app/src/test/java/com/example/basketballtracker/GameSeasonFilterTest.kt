package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameSeason
import com.example.basketballtracker.features.history.state.HistoryState
import com.example.basketballtracker.features.history.state.buildHistoryState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSeasonFilterTest {
    private fun game(id: Long, season: String) = GameEntity(id = id, season = season,
        opponentName = "Team", roundNumber = 1, gameDateEpoch = 0, createdAt = 0)

    @Test fun startsWithCurrentSeasonEvenWhenEmpty() {
        assertEquals(GameSeason.CURRENT, HistoryState().selectedSeason)
        val state = buildHistoryState(listOf(game(1, "25/26")), GameSeason.CURRENT)
        assertEquals(emptyList<GameEntity>(), state.games)
        assertEquals(listOf("26/27", "25/26"), state.seasons)
    }

    @Test fun filteringPreservesOrderAndExcludesDeletedGames() {
        val games = listOf(game(4, "26/27"), game(3, "25/26"), game(2, "26/27"),
            game(1, "24/25").copy(isDeleted = true))
        assertEquals(listOf(4L, 2L), buildHistoryState(games, "26/27").games.map { it.id })
        assertEquals(listOf(3L), buildHistoryState(games, "25/26").games.map { it.id })
        assertEquals(listOf(4L, 3L, 2L), buildHistoryState(games, null).games.map { it.id })
        assertEquals(listOf("26/27", "25/26"), buildHistoryState(games, null).seasons)
    }

    @Test fun deletingLastGameKeepsSelectedSeasonAvailable() {
        val state = buildHistoryState(emptyList(), "25/26")
        assertEquals("25/26", state.selectedSeason)
        assertEquals(listOf("26/27", "25/26"), state.seasons)
    }
}
