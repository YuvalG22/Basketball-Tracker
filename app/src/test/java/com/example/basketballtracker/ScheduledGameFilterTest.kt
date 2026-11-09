package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.features.history.state.buildHistoryState
import com.example.basketballtracker.features.home.ui.buildHomeState
import com.example.basketballtracker.features.stats.data.SeasonalData
import org.junit.Assert.*
import org.junit.Test

class ScheduledGameFilterTest {
    @Test fun futureFixturesNeverCountAsResultsOrLiveGames() {
        val scheduled = GameEntity(opponentName = "Future", roundNumber = 1,
            gameDateEpoch = 1795644000000L, createdAt = 0, season = GameSeason.CURRENT,
            status = GameStatus.SCHEDULED)
        val home = buildHomeState(listOf(scheduled))
        assertEquals(0, home.played)
        assertTrue(home.liveGames.isEmpty())
        assertTrue(home.recentGames.isEmpty())
        assertTrue(buildHistoryState(listOf(scheduled), GameSeason.CURRENT).games.isEmpty())
        assertEquals(0, SeasonalData(games = listOf(scheduled.copy(teamScore = 12))).teamPoints(GameSeason.CURRENT))
        assertEquals(GameStatus.SCHEDULED, scheduled.status)
    }
}
