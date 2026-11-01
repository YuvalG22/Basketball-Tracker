package com.example.basketballtracker.features.players.data

import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedForPlayer
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedForPlayerAfterEnd
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedForPlayerInPeriod
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedForPlayerInPeriodAfterEnd

fun buildPlayerGameStats(
    playerId: Long,
    game: GameEntity,
    events: List<EventEntity>,
    selectedPeriod: Int? = null
): PlayerGameStats {

    val playerEvents = events.filter {
        it.gameId == game.id && it.playerId == playerId && it.syncStatus != "PENDING_DELETE"
    }
    val filteredEvents = if (selectedPeriod == null) {
        playerEvents
    } else {
        playerEvents.filter { it.period == selectedPeriod }
    }

    val twoMade = filteredEvents.count { it.type == EventType.TWO_MADE.name }
    val twoMiss = filteredEvents.count { it.type == EventType.TWO_MISS.name }
    val threeMade = filteredEvents.count { it.type == EventType.THREE_MADE.name }
    val threeMiss = filteredEvents.count { it.type == EventType.THREE_MISS.name }
    val ftMade = filteredEvents.count { it.type == EventType.FT_MADE.name }
    val ftMiss = filteredEvents.count { it.type == EventType.FT_MISS.name }

    val points = twoMade * 2 + threeMade * 3 + ftMade

    fun secondsThroughPeriod(period: Int): Int {
        if (period <= 0) return 0
        return computeSecondsPlayedForPlayerAfterEnd(
            playerId = playerId,
            events = playerEvents.filter { it.period <= period },
            quarterLengthSec = game.quarterLengthSec,
            currentPeriod = period,
            currentClockSecRemaining = 0
        )
    }
    val secPlayed = if (selectedPeriod == null) {
        secondsThroughPeriod(maxOf(game.quartersCount, playerEvents.maxOfOrNull { it.period } ?: 1))
    } else {
        (secondsThroughPeriod(selectedPeriod) - secondsThroughPeriod(selectedPeriod - 1)).coerceAtLeast(0)
    }

    val shots = filteredEvents.filter {
        it.type == EventType.TWO_MADE.name ||
                it.type == EventType.TWO_MISS.name ||
                it.type == EventType.THREE_MADE.name ||
                it.type == EventType.THREE_MISS.name
    }

    return PlayerGameStats(
        game = game,
        secondsPlayed = secPlayed,
        points = points,
        rebounds = filteredEvents.count { it.type == EventType.REB_DEF.name } +
                filteredEvents.count { it.type == EventType.REB_OFF.name },
        rebDef = filteredEvents.count { it.type == EventType.REB_DEF.name },
        rebOff = filteredEvents.count { it.type == EventType.REB_OFF.name },
        assists = filteredEvents.count { it.type == EventType.AST.name },
        steals = filteredEvents.count { it.type == EventType.STL.name },
        blocks = filteredEvents.count { it.type == EventType.BLK.name },
        turnovers = filteredEvents.count { it.type == EventType.TOV.name },
        pf = filteredEvents.count { it.type == EventType.PF.name },

        twoMade = twoMade,
        twoMiss = twoMiss,
        threeMade = threeMade,
        threeMiss = threeMiss,
        ftMade = ftMade,
        ftMiss = ftMiss,

        shots = shots,
        events = playerEvents
    )
}
