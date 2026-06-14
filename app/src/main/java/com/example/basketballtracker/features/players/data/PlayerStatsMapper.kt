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

    val filteredEvents = if (selectedPeriod == null) {
        events
    } else {
        events.filter { it.period == selectedPeriod }
    }

    val twoMade = filteredEvents.count { it.type == EventType.TWO_MADE.name }
    val twoMiss = filteredEvents.count { it.type == EventType.TWO_MISS.name }
    val threeMade = filteredEvents.count { it.type == EventType.THREE_MADE.name }
    val threeMiss = filteredEvents.count { it.type == EventType.THREE_MISS.name }
    val ftMade = filteredEvents.count { it.type == EventType.FT_MADE.name }
    val ftMiss = filteredEvents.count { it.type == EventType.FT_MISS.name }

    val points = twoMade * 2 + threeMade * 3 + ftMade

    val secPlayed = if (selectedPeriod == null) {
        computeSecondsPlayedForPlayerAfterEnd(
            playerId = playerId,
            events = events,
            quarterLengthSec = 10,
            currentPeriod = 4,
            currentClockSecRemaining = 0
        )
    } else {
        computeSecondsPlayedForPlayerInPeriodAfterEnd(
            playerId = playerId,
            events = events,
            period = selectedPeriod,
            quarterLengthSec = 10
        )
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
        events = events
    )
}