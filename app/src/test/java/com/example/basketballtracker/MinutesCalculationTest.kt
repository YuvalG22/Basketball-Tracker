package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.features.livegame.domain.*
import com.example.basketballtracker.features.players.data.buildPlayerGameStats
import org.junit.Assert.assertEquals
import org.junit.Test

class MinutesCalculationTest {
    private val game = GameEntity(id = 1, opponentName = "Visitors", roundNumber = 1,
        gameDateEpoch = 0, createdAt = 0)

    private fun sub(id: Long, type: EventType, period: Int, clock: Int, player: Long = 1) =
        LiveEvent(id = id, gameId = game.id, playerId = player, type = type,
            period = period, clockSecRemaining = clock, createdAt = id,
            teamScoreAtEvent = null, opponentScoreAtEvent = null)

    private fun List<LiveEvent>.stored() = map {
        EventEntity(id = it.id, gameId = it.gameId, playerId = it.playerId,
            type = it.type.name, period = it.period, clockSecRemaining = it.clockSecRemaining,
            createdAt = it.createdAt)
    }

    private fun assertTotal(expected: Int, events: List<LiveEvent>, period: Int = 4,
                            clock: Int = 0, length: Int = 600, player: Long = 1) {
        assertEquals("Live player", expected,
            computeSecondsPlayedForPlayer(player, events, length, period, clock))
        assertEquals("Live team map", expected,
            computeSecondsPlayedByPlayer(events, length, period, clock)[player] ?: 0)
        assertEquals("Saved player", expected,
            computeSecondsPlayedForPlayerAfterEnd(player, events.stored(), length, period, clock))
        assertEquals("Box score", expected,
            computeBoxByPlayer(events, length, period, clock)[player]?.secondsPlayed ?: 0)
    }

    private fun assertQuarters(expected: List<Int>, events: List<LiveEvent>, length: Int = 600) {
        val stored = events.stored()
        expected.forEachIndexed { index, seconds ->
            val period = index + 1
            assertEquals("Live Q$period", seconds,
                computeSecondsPlayedForPlayerInPeriod(1, events, period, length))
            assertEquals("Saved Q$period", seconds,
                computeSecondsPlayedForPlayerInPeriodAfterEnd(1, stored, period, length))
            assertEquals("Player details Q$period", seconds,
                buildPlayerGameStats(1, game.copy(quarterLengthSec = length), stored, period).secondsPlayed)
        }
        assertTotal(expected.sum(), events, length = length)
        assertEquals("Quarter sum equals game total", expected.sum(),
            buildPlayerGameStats(1, game.copy(quarterLengthSec = length), stored).secondsPlayed)
    }

    @Test fun benchPlayerAndEmptyGameHaveZeroMinutes() {
        assertTotal(0, emptyList())
        assertTotal(0, listOf(sub(1, EventType.SUB_IN, 1, 600, player = 2)))
        assertQuarters(listOf(0, 0, 0, 0), emptyList())
    }

    @Test fun starterPlaysFullGameWithoutBoundarySubstitutions() {
        assertQuarters(listOf(600, 600, 600, 600), listOf(sub(1, EventType.SUB_IN, 1, 600)))
    }

    @Test fun substitutionAcrossQuarterBoundarySplitsMinutes() {
        assertQuarters(listOf(120, 120, 0, 0), listOf(
            sub(1, EventType.SUB_IN, 1, 120), sub(2, EventType.SUB_OUT, 2, 480)))
    }

    @Test fun stintSpanningAnEntireQuarterCountsThatQuarter() {
        assertQuarters(listOf(60, 600, 90, 0), listOf(
            sub(1, EventType.SUB_IN, 1, 60), sub(2, EventType.SUB_OUT, 3, 510)))
    }

    @Test fun repeatedStintsPreserveSecondsBeforeRounding() {
        assertQuarters(listOf(61, 0, 0, 0), listOf(
            sub(1, EventType.SUB_IN, 1, 600), sub(2, EventType.SUB_OUT, 1, 569),
            sub(3, EventType.SUB_IN, 1, 400), sub(4, EventType.SUB_OUT, 1, 370)))
        assertEquals("0", formatMinutes(59))
        assertEquals("1", formatMinutes(61))
    }

    @Test fun explicitBoundaryOutAndInDoNotDoubleCount() {
        assertQuarters(listOf(600, 120, 0, 0), listOf(
            sub(1, EventType.SUB_IN, 1, 600), sub(2, EventType.SUB_OUT, 1, 0),
            sub(3, EventType.SUB_IN, 2, 600), sub(4, EventType.SUB_OUT, 2, 480)))
    }

    @Test fun duplicateInsAndUnmatchedOutsDoNotAddTime() {
        assertQuarters(listOf(120, 0, 0, 0), listOf(
            sub(1, EventType.SUB_OUT, 1, 600), sub(2, EventType.SUB_IN, 1, 500),
            sub(3, EventType.SUB_IN, 1, 450), sub(4, EventType.SUB_OUT, 1, 380),
            sub(5, EventType.SUB_OUT, 1, 300)))
    }

    @Test fun currentClockLimitsOpenStintAndPausedClockAddsNoTime() {
        val events = listOf(sub(1, EventType.SUB_IN, 1, 600))
        assertTotal(0, events, period = 1, clock = 600)
        assertTotal(125, events, period = 1, clock = 475)
        assertTotal(125, events, period = 1, clock = 475)
        assertTotal(126, events, period = 1, clock = 474)
    }

    @Test fun liveQuarterStopsAtCurrentClockAndFutureQuartersHaveZeroMinutes() {
        val events = listOf(sub(1, EventType.SUB_IN, 1, 120))
        val expected = listOf(120, 75, 0, 0)
        expected.forEachIndexed { index, seconds ->
            assertEquals("Live Q${index + 1}", seconds,
                computeSecondsPlayedForPlayerInPeriod(1, events, index + 1, 600,
                    currentPeriod = 2, currentClockSecRemaining = 525))
        }
        assertTotal(expected.sum(), events, period = 2, clock = 525)
        assertEquals(0, computeSecondsPlayedForPlayerInPeriod(1, events, 2, 600,
            currentPeriod = 2, currentClockSecRemaining = 600))
    }

    @Test fun substitutionWithoutClockMovementAddsNoSeconds() {
        assertQuarters(listOf(0, 0, 0, 0), listOf(
            sub(1, EventType.SUB_IN, 1, 451), sub(2, EventType.SUB_OUT, 1, 451)))
    }

    @Test fun savedMinutesIgnoreOtherPlayersOtherGamesAndDeletedSubstitutions() {
        val events = listOf(sub(1, EventType.SUB_IN, 1, 600),
            sub(2, EventType.SUB_OUT, 1, 480)).stored() + listOf(
            sub(3, EventType.SUB_OUT, 1, 590).storedEvent().copy(syncStatus = "PENDING_DELETE"),
            sub(4, EventType.SUB_OUT, 1, 580).storedEvent().copy(gameId = 2),
            sub(5, EventType.SUB_IN, 2, 600, player = 2).storedEvent())
        assertEquals(120, buildPlayerGameStats(1, game, events).secondsPlayed)
        assertEquals(120, buildPlayerGameStats(1, game, events, 1).secondsPlayed)
        assertEquals(0, buildPlayerGameStats(1, game, events, 2).secondsPlayed)
    }

    private fun LiveEvent.storedEvent() = listOf(this).stored().single()

    @Test fun historicalSnapshotDoesNotCountFutureSubstitutions() {
        val events = listOf(sub(1, EventType.SUB_IN, 1, 600), sub(2, EventType.SUB_OUT, 1, 300),
            sub(3, EventType.SUB_IN, 2, 600), sub(4, EventType.SUB_OUT, 2, 0))
        assertTotal(60, events, period = 1, clock = 540)
    }

    @Test fun shuffledEventsUseClockOrderAndIdToBreakTimestampTies() {
        val events = listOf(sub(1, EventType.SUB_IN, 1, 600), sub(2, EventType.SUB_OUT, 1, 480),
            sub(3, EventType.SUB_IN, 1, 480), sub(4, EventType.SUB_OUT, 1, 360))
            .map { it.copy(createdAt = 0) }.reversed()
        assertQuarters(listOf(240, 0, 0, 0), events)
    }

    @Test fun configuredEightMinuteQuartersAreUsed() {
        assertQuarters(listOf(480, 480, 480, 480),
            listOf(sub(1, EventType.SUB_IN, 1, 480)), length = 480)
    }

    @Test fun fivePlayersAlwaysOnCourtProduceTwoHundredTeamMinutes() {
        val starters = (1L..5L).map { sub(it, EventType.SUB_IN, 1, 600, player = it) }
        val events = starters + listOf(sub(6, EventType.SUB_OUT, 2, 300),
            sub(7, EventType.SUB_IN, 2, 300, player = 6))
        val totals = computeSecondsPlayedByPlayer(events, 600, 4, 0)
        assertEquals(200 * 60, totals.values.sum())
        assertEquals(900, totals[1])
        assertEquals(1500, totals[6])
        for (period in 1..4) {
            assertEquals("Team Q$period", 50 * 60, (1L..6L).sumOf {
                computeSecondsPlayedForPlayerInPeriod(it, events, period, 600)
            })
        }
    }
}
