package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.features.stats.data.SeasonalData
import com.example.basketballtracker.features.stats.state.StatsDisplayMode
import com.example.basketballtracker.features.stats.ui.formatTeamStat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SeasonTeamPointsTest {
    private fun game(id: Long, points: Int) = GameEntity(
        id = id, opponentName = "Opponent", roundNumber = id.toInt(),
        gameDateEpoch = 0, createdAt = 0, teamScore = points, season = "25/26"
    )

    @Test fun savedResultsCountEvenWithoutPlayByPlay() {
        val games = listOf(73, 87, 70, 60, 71, 59, 28, 70, 67)
            .mapIndexed { index, points -> game(index.toLong() + 1, points) }
        val data = SeasonalData(games = games)
        assertEquals(585, data.teamPoints("25/26"))
        assertEquals(String.format(Locale.getDefault(), "%.1f", 65.0),
            formatTeamStat(data.teamPoints("25/26"), games.size, StatsDisplayMode.PER_GAME))
        assertEquals("585", formatTeamStat(data.teamPoints("25/26"), games.size, StatsDisplayMode.TOTAL))
    }

    @Test fun excludesOtherSeasonsDeletedAndLiveGames() {
        val data = SeasonalData(games = listOf(game(1, 73),
            game(2, 100).copy(season = "26/27"),
            game(3, 102).copy(isDeleted = true),
            game(4, 40).copy(status = GameStatus.LIVE)))
        assertEquals(73, data.teamPoints("25/26"))
        assertEquals(0, data.teamPoints("24/25"))
    }
}
