package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.features.stats.data.SeasonalData
import com.example.basketballtracker.features.stats.state.StatsDisplayMode
import com.example.basketballtracker.features.stats.ui.formatRecordedTeamStat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SeasonRecordedStatsTest {
    private fun game(id: Long) = GameEntity(id = id, opponentName = "Opponent",
        roundNumber = 1, gameDateEpoch = 0, createdAt = 0, season = "25/26")
    private fun event(game: Long, type: String) = EventEntity(gameId = game,
        playerId = 1, type = type, period = 1, clockSecRemaining = 300, createdAt = 0)

    @Test fun averagesUseRecordedGamesIncludingGamesWithZeroInACategory() {
        val counts = listOf("STL" to 96, "BLK" to 8, "TOV" to 115, "PF" to 134)
        val events = (1L..7L).map { event(it, "TWO_MISS") } +
            counts.flatMap { (type, count) -> List(count) { event(1, type) } }
        val data = SeasonalData(games = (1L..9L).map(::game), events = events)
        val games = data.recordedGameCount("25/26")
        assertEquals(7, games)
        val player = data.stats("25/26").single()
        assertEquals(listOf(96, 8, 115, 134), listOf(player.stl, player.blk, player.tov, player.pf))
        listOf(96 to 13.7, 8 to 1.1, 115 to 16.4, 134 to 19.1).forEach { (total, avg) ->
            assertEquals(String.format(Locale.getDefault(), "%.1f", avg),
                formatRecordedTeamStat(total, games, StatsDisplayMode.PER_GAME))
            assertEquals(total.toString(), formatRecordedTeamStat(total, games, StatsDisplayMode.TOTAL))
        }
    }

    @Test fun ignoresUnavailableDeletedLiveOtherSeasonAndAdministrativeOnlyGames() {
        val data = SeasonalData(games = listOf(game(1), game(2), game(3).copy(isDeleted = true),
            game(4).copy(status = GameStatus.LIVE), game(5).copy(season = "26/27"), game(6), game(7)),
            events = listOf(event(1, "STL"), event(2, "BLK").copy(syncStatus = "PENDING_DELETE"),
                event(3, "TOV"), event(4, "PF"), event(5, "STL"), event(6, "SUB_IN")))
        assertEquals(1, data.recordedGameCount("25/26"))
        assertEquals(1, data.stats("25/26").sumOf { it.stl })
        assertEquals(0, data.stats("25/26").sumOf { it.blk + it.tov + it.pf })
        assertEquals(0, data.recordedGameCount("24/25"))
        assertEquals("—", formatRecordedTeamStat(0, 0, StatsDisplayMode.PER_GAME))
    }
}
