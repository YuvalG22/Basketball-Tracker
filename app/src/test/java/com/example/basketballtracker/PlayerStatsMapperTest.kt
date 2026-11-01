package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.features.players.data.buildPlayerGameStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStatsMapperTest {
    private val game = GameEntity(id = 10, opponentName = "Visitors", roundNumber = 1,
        gameDateEpoch = 0, createdAt = 0, quarterLengthSec = 600, quartersCount = 4)

    private fun event(type: String, period: Int = 1, clock: Int = 300, player: Long = 1) =
        EventEntity(gameId = game.id, playerId = player, type = type, period = period,
            clockSecRemaining = clock, createdAt = 0, shotX = 5f, shotY = 6f)

    @Test
    fun statsAndShotsBelongOnlyToSelectedPlayerAndGame() {
        val events = listOf(event("THREE_MADE"), event("TWO_MISS"), event("AST"),
            event("TWO_MADE", player = 2), event("THREE_MADE").copy(gameId = 99),
            event("THREE_MADE").copy(syncStatus = "PENDING_DELETE"))
        val stats = buildPlayerGameStats(1, game, events)
        assertEquals(3, stats.points)
        assertEquals(1, stats.assists)
        assertEquals(2, stats.shots.size)
        assertTrue(stats.events.all { it.playerId == 1L && it.gameId == game.id })
    }

    @Test
    fun quarterFilterRetainsFullHistoryAndCountsMinutesAcrossQuarterBoundary() {
        val events = listOf(event("SUB_IN", clock = 120), event("TWO_MADE", clock = 90),
            event("THREE_MADE", period = 2, clock = 550), event("SUB_OUT", period = 2, clock = 480))
        val all = buildPlayerGameStats(1, game, events)
        val second = buildPlayerGameStats(1, game, events, selectedPeriod = 2)
        assertEquals(240, all.secondsPlayed)
        assertEquals(5, all.points)
        assertEquals(120, second.secondsPlayed)
        assertEquals(3, second.points)
        assertEquals(1, second.shots.size)
        assertEquals(4, second.events.size)
    }

    @Test
    fun emptyPlayerHasZeroStatsAndConfiguredPeriodLengthIsUsed() {
        assertEquals(0, buildPlayerGameStats(2, game, emptyList()).points)
        val shorterGame = game.copy(quarterLengthSec = 480)
        val stats = buildPlayerGameStats(1, shorterGame,
            listOf(event("SUB_IN", clock = 480), event("SUB_OUT", clock = 60)))
        assertEquals(420, stats.secondsPlayed)
    }
}
