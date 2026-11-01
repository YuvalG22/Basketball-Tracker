package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.features.livegame.data.toDomain
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.computeBoxByPlayer
import com.example.basketballtracker.features.livegame.domain.computeOppScore
import com.example.basketballtracker.features.livegame.domain.computePlusMinusByPlayer
import com.example.basketballtracker.features.livegame.domain.computePlusMinusForPlayer
import com.example.basketballtracker.features.livegame.domain.computeTeamScore
import org.junit.Assert.assertEquals
import org.junit.Test

class PlusMinusCalculationTest {
    private fun event(id: Long, type: EventType, clock: Int = 500,
                      player: Long? = null, period: Int = 1, createdAt: Long = id) =
        LiveEvent(id = id, gameId = 1, playerId = player, type = type,
            period = period, clockSecRemaining = clock, createdAt = createdAt,
            teamScoreAtEvent = null, opponentScoreAtEvent = null)

    private fun starter(player: Long = 1) = event(player, EventType.SUB_IN, 600, player)

    private fun assertPlayer(expected: Int, events: List<LiveEvent>, player: Long = 1) {
        assertEquals("Player $player", expected, computePlusMinusForPlayer(player, events))
        assertEquals("Team map for $player", expected, computePlusMinusByPlayer(events)[player] ?: 0)
        assertEquals("Box score for $player", expected,
            computeBoxByPlayer(events, 600, 4, 0)[player]?.plusMinus ?: 0)
    }

    private fun quarter(player: Long, events: List<LiveEvent>, period: Int): Int {
        val result = computePlusMinusForPlayer(player, events, selectedPeriod = period)
        assertEquals("Player and team map agree for Q$period", result,
            computePlusMinusByPlayer(events, selectedPeriod = period)[player] ?: 0)
        return result
    }

    @Test fun emptyGameAndUnusedBenchPlayerHaveZeroPlusMinus() {
        assertPlayer(0, emptyList())
        assertPlayer(0, listOf(starter(2), event(10, EventType.THREE_MADE, player = 2)))
    }

    @Test fun everyMadeShotHasTheCorrectSignedValue() {
        val points = mapOf(EventType.FT_MADE to 1, EventType.TWO_MADE to 2,
            EventType.THREE_MADE to 3, EventType.OPP_FT_MADE to -1,
            EventType.OPP_TWO_MADE to -2, EventType.OPP_THREE_MADE to -3)
        points.forEach { (type, delta) ->
            assertPlayer(delta, listOf(starter(), event(10, type)))
        }
    }

    @Test fun missesAndNonScoringEventsDoNotChangePlusMinus() {
        val ignored = listOf(EventType.FT_MISS, EventType.TWO_MISS, EventType.THREE_MISS,
            EventType.OPP_FT_MISS, EventType.OPP_TWO_MISS, EventType.OPP_THREE_MISS,
            EventType.REB_OFF, EventType.REB_DEF, EventType.AST, EventType.STL,
            EventType.TOV, EventType.BLK, EventType.PF, EventType.OPP_PF,
            EventType.PERIOD_START, EventType.PERIOD_END)
        ignored.forEach { type ->
            assertPlayer(0, listOf(starter(), event(10, type, player = 1)))
        }
    }

    @Test fun teammateBasketCountsForEveryPlayerOnCourt() {
        val events = (1L..5L).map { starter(it) } + event(10, EventType.THREE_MADE, player = 3)
        (1L..5L).forEach { assertPlayer(3, events, it) }
        assertPlayer(0, events, 6)
    }

    @Test fun scoresBeforeEntryAndAfterExitDoNotCount() {
        val events = listOf(event(1, EventType.THREE_MADE, 590),
            event(2, EventType.SUB_IN, 550, 1), event(3, EventType.TWO_MADE, 500, 1),
            event(4, EventType.SUB_OUT, 450, 1), event(5, EventType.OPP_THREE_MADE, 400))
        assertPlayer(2, events)
    }

    @Test fun substitutionTransfersScoringToTheIncomingPlayer() {
        val events = listOf(starter(), event(10, EventType.TWO_MADE, 550, 1),
            event(11, EventType.SUB_OUT, 500, 1), event(12, EventType.SUB_IN, 500, 2),
            event(13, EventType.OPP_THREE_MADE, 450))
        assertPlayer(2, events, 1)
        assertPlayer(-3, events, 2)
    }

    @Test fun multipleStintsAccumulateAndExcludeBenchScoring() {
        val events = listOf(starter(), event(10, EventType.THREE_MADE, 550, 2),
            event(11, EventType.SUB_OUT, 500, 1), event(12, EventType.OPP_THREE_MADE, 450),
            event(13, EventType.SUB_IN, 400, 1), event(14, EventType.OPP_FT_MADE, 350))
        assertPlayer(2, events)
    }

    @Test fun duplicateEntriesAndUnmatchedExitsDoNotMultiplyPoints() {
        val events = listOf(event(1, EventType.SUB_OUT, 600, 1),
            event(2, EventType.SUB_IN, 580, 1), event(3, EventType.SUB_IN, 550, 1),
            event(4, EventType.TWO_MADE, 500), event(5, EventType.SUB_OUT, 450, 1),
            event(6, EventType.SUB_OUT, 400, 1), event(7, EventType.OPP_THREE_MADE, 350))
        assertPlayer(2, events)
    }

    @Test fun nullPlayerSubstitutionsDoNotChangeTheLineup() {
        val events = listOf(starter(), event(10, EventType.SUB_OUT, 550),
            event(11, EventType.SUB_IN, 540), event(12, EventType.TWO_MADE, 500))
        assertPlayer(2, events)
        assertEquals(setOf(1L), computePlusMinusByPlayer(events).keys)
    }

    @Test fun sameClockEventsUseCreationTimeBeforeId() {
        val events = listOf(starter(),
            event(13, EventType.FT_MADE, createdAt = 10),
            event(12, EventType.SUB_OUT, player = 1, createdAt = 11),
            event(11, EventType.SUB_IN, player = 2, createdAt = 12),
            event(10, EventType.FT_MADE, createdAt = 13)).reversed()
        assertPlayer(1, events, 1)
        assertPlayer(1, events, 2)
    }

    @Test fun sameClockAndCreationTimeUseIdToOrderSubstitutionsBetweenFreeThrows() {
        val events = listOf(starter(), event(10, EventType.FT_MADE),
            event(11, EventType.SUB_OUT, player = 1), event(12, EventType.SUB_IN, player = 2),
            event(13, EventType.FT_MADE)).map { it.copy(createdAt = 0) }.reversed()
        assertPlayer(1, events, 1)
        assertPlayer(1, events, 2)
    }

    @Test fun removingAnUndoneScoreRecalculatesPlusMinus() {
        val events = listOf(starter(), event(10, EventType.THREE_MADE, 550),
            event(11, EventType.OPP_TWO_MADE, 500))
        assertPlayer(1, events)
        assertPlayer(3, events.filterNot { it.id == 11L })
        assertPlayer(0, events.filterNot { it.id == 10L || it.id == 11L })
    }

    @Test fun persistedEventsProduceTheSamePlusMinusAfterMapping() {
        val events = listOf(starter(), event(10, EventType.THREE_MADE, 550),
            event(11, EventType.OPP_FT_MADE, 500))
        val restored = events.map {
            EventEntity(id = it.id, gameId = it.gameId, playerId = it.playerId,
                type = it.type.name, period = it.period, clockSecRemaining = it.clockSecRemaining,
                createdAt = it.createdAt).toDomain()
        }
        assertPlayer(2, restored)
    }

    @Test fun quarterKeepsLineupCarriedFromPreviousQuarter() {
        val events = listOf(starter(), event(10, EventType.TWO_MADE, 550, period = 1),
            event(11, EventType.OPP_THREE_MADE, 550, period = 2))
        assertPlayer(-1, events)
        assertEquals(2, quarter(1, events, 1))
        assertEquals(-3, quarter(1, events, 2))
        assertEquals(0, quarter(1, events, 3))
    }

    @Test fun quarterCountsOnlyScoringDuringThatQuarterAndOnCourtStints() {
        val events = listOf(starter(), event(10, EventType.TWO_MADE, 550),
            event(11, EventType.OPP_THREE_MADE, 550, period = 2),
            event(12, EventType.SUB_OUT, 400, 1, period = 2),
            event(13, EventType.THREE_MADE, 300, period = 2),
            event(14, EventType.SUB_IN, 200, 1, period = 2),
            event(15, EventType.FT_MADE, 100, period = 2),
            event(16, EventType.OPP_TWO_MADE, 500, period = 3),
            event(17, EventType.THREE_MADE, 500, period = 4)).reversed()
        val quarters = (1..4).map { quarter(1, events, it) }
        assertEquals(listOf(2, -2, -2, 3), quarters)
        assertPlayer(1, events)
        assertEquals(computePlusMinusForPlayer(1, events), quarters.sum())
    }

    @Test fun boundarySubstitutionsAndOvertimeMaintainTheCorrectLineup() {
        val events = listOf(starter(), event(10, EventType.TWO_MADE, 0, period = 4),
            event(11, EventType.SUB_OUT, 0, 1, period = 4),
            event(12, EventType.SUB_IN, 600, 2, period = 5),
            event(13, EventType.OPP_THREE_MADE, 500, period = 5))
        assertPlayer(2, events, 1)
        assertPlayer(-3, events, 2)
        assertEquals(0, quarter(1, events, 5))
        assertEquals(-3, quarter(2, events, 5))
    }

    @Test fun sumOfPlayerPlusMinusEqualsFiveTimesScoreDifference() {
        val events = (1L..5L).map { starter(it) } + listOf(
            event(10, EventType.THREE_MADE, 550, 3),
            event(11, EventType.SUB_OUT, 500, 1), event(12, EventType.SUB_IN, 500, 6),
            event(13, EventType.OPP_TWO_MADE, 450),
            event(14, EventType.FT_MADE, 550, 2, period = 2),
            event(15, EventType.OPP_THREE_MADE, 500, period = 2))
        val expected = mapOf(1L to 3, 2L to -1, 3L to -1, 4L to -1, 5L to -1, 6L to -4)
        assertEquals(expected, computePlusMinusByPlayer(events))
        expected.forEach { (player, pm) -> assertPlayer(pm, events, player) }
        assertEquals(5 * (computeTeamScore(events) - computeOppScore(events)),
            computePlusMinusByPlayer(events).values.sum())
        assertEquals(5, (1L..6L).sumOf { quarter(it, events, 1) })
        assertEquals(-10, (1L..6L).sumOf { quarter(it, events, 2) })
    }
}
