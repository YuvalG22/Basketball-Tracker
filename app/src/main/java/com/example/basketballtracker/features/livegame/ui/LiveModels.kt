package com.example.basketballtracker.features.livegame.ui

import kotlin.math.max

import kotlin.math.roundToInt

enum class EventType {
    FT_MADE, FT_MISS,
    TWO_MADE, TWO_MISS,
    THREE_MADE, THREE_MISS,
    REB_OFF, REB_DEF,
    AST, STL, TOV, BLK, PF,
    SUB_IN, SUB_OUT,

    //opponent events
    OPP_TWO_MADE,
    OPP_THREE_MADE,
    OPP_FT_MADE,

    PERIOD_START,
    PERIOD_END;

    /**
     * Indicates whether the event was recorded for the opponent team.
     *
     * @return `true` if the event is one of the opponent score events (`OPP_TWO_MADE`, `OPP_THREE_MADE`, `OPP_FT_MADE`), `false` otherwise.
     */
    fun isOpponentEvent(): Boolean = when (this) {
        OPP_TWO_MADE, OPP_THREE_MADE, OPP_FT_MADE -> true
        else -> false
    }

    /**
     * Indicates whether the event is scoring-related or a period boundary marker.
     *
     * @return `true` if the event is a made score (team or opponent) or a period start/end marker, `false` otherwise.
     */
    fun isScoreEvent(): Boolean = when (this) {
        TWO_MADE, THREE_MADE, FT_MADE, OPP_TWO_MADE, OPP_THREE_MADE, OPP_FT_MADE, PERIOD_START, PERIOD_END -> true
        else -> false
    }

    /**
     * Indicates whether this event type is associated with a specific player.
     *
     * @return `true` if the event requires a player (for example: scoring, rebound, assist, steal, block, turnover, or foul), `false` otherwise.
     */
    fun requiresPlayer(): Boolean = when (this) {
        TWO_MADE,
        TWO_MISS,
        THREE_MADE,
        THREE_MISS,
        FT_MADE,
        FT_MISS,
        REB_DEF,
        REB_OFF,
        AST,
        STL,
        BLK,
        TOV,
        PF -> true

        else -> false // Opponent / System
    }
}

data class GameClock(
    val period: Int = 1,
    val secRemaining: Int,
    val isRunning: Boolean = false
)

data class LiveEvent(
    val id: Long,
    val gameId: Long,
    val playerId: Long?,
    val type: EventType,
    val period: Int,
    val clockSecRemaining: Int,
    val createdAt: Long,
    val teamScoreAtEvent: Int?,
    val opponentScoreAtEvent: Int?
)

data class PlayerBox(
    val playerId: Long,
    val pts: Int,
    val twom: Int, val twoa: Int,
    val threem: Int, val threea: Int,
    val ftm: Int, val fta: Int,
    val rebOff: Int,
    val rebDef: Int,
    val ast: Int,
    val tov: Int,
    val stl: Int,
    val blk: Int,
    val pf: Int,
) {
    val fgm get() = twom + threem
    val fga get() = twoa + threea
    val rebTotal get() = rebOff + rebDef

    val ftPct get() = pct(ftm, fta)
    val fgPct get() = pct(fgm, fga)
    val twoPct get() = pct(twom, twoa)
    val threePct get() = pct(threem, threea)

    private fun pct(made: Int, att: Int): Int {
        if (att == 0) return 0
        return ((made * 100.0) / att).roundToInt()
    }
}

fun computeBoxByPlayer(events: List<LiveEvent>): Map<Long, PlayerBox> {
    val plusMinusById = computePlusMinusByPlayer(events)
    val grouped = events.filter { it.playerId != null }.groupBy { it.playerId!! }
    return grouped.mapValues { (pid, evs) ->
        fun c(t: EventType) = evs.count { it.type == t }

        val ftm = c(EventType.FT_MADE)
        val ftmiss = c(EventType.FT_MISS)

        val twom = c(EventType.TWO_MADE)
        val twomiss = c(EventType.TWO_MISS)

        val threemiss = c(EventType.THREE_MISS)
        val threem = c(EventType.THREE_MADE)

        val fta = ftm + ftmiss
        val twoa = twom + twomiss
        val threea = threem + threemiss

        PlayerBox(
            playerId = pid,
            pts = ftm * 1 + twom * 2 + threem * 3,
            ftm = ftm, fta = fta,
            twom = twom, twoa = twoa,
            threem = threem, threea = threea,
            rebOff = c(EventType.REB_OFF),
            rebDef = c(EventType.REB_DEF),
            ast = c(EventType.AST),
            tov = c(EventType.TOV),
            stl = c(EventType.STL),
            blk = c(EventType.BLK),
            pf = c(EventType.PF),
        )
    }
}

fun computeTeamScore(events: List<LiveEvent>): Int {
    fun c(t: EventType) = events.count { it.type == t }

    val ftm = c(EventType.FT_MADE)
    val twom = c(EventType.TWO_MADE)
    val threem = c(EventType.THREE_MADE)

    val pts = ftm * 1 + twom * 2 + threem * 3

    return pts
}

fun computeOppScore(events: List<LiveEvent>): Int {
    fun c(t: EventType) = events.count { it.type == t }

    val ftm = c(EventType.OPP_FT_MADE)
    val twom = c(EventType.OPP_TWO_MADE)
    val threem = c(EventType.OPP_THREE_MADE)

    val pts = ftm * 1 + twom * 2 + threem * 3

    return pts
}

fun computeOnCourtIds(events: List<LiveEvent>): Set<Long> {
    val onCourt = linkedSetOf<Long>()
    for (e in events) {
        val pid = e.playerId ?: continue
        when (e.type) {
            EventType.SUB_IN -> onCourt.add(pid)
            EventType.SUB_OUT -> onCourt.remove(pid)
            else -> Unit
        }
    }
    return onCourt
}

fun computeSecondsPlayedByPlayer(
    events: List<LiveEvent>,
    quarterLengthSec: Int,
    currentPeriod: Int,
    currentClockSecRemaining: Int
): Map<Long, Int> {
    val nowT = toGameElapsedSec(
        period = currentPeriod,
        clockSecRemaining = currentClockSecRemaining,
        quarterLengthSec = quarterLengthSec
    )

    val sorted = events.sortedWith(
        compareBy<LiveEvent>({ it.period }, { -it.clockSecRemaining }, { it.createdAt })
    )

    val inTime = mutableMapOf<Long, Int>()
    val total = mutableMapOf<Long, Int>()

    for (e in sorted) {
        val pid = e.playerId ?: continue
        if (e.type != EventType.SUB_IN && e.type != EventType.SUB_OUT) continue

        val t = toGameElapsedSec(e.period, e.clockSecRemaining, quarterLengthSec)

        when (e.type) {
            EventType.SUB_IN -> {
                if (!inTime.containsKey(pid)) inTime[pid] = t
            }

            EventType.SUB_OUT -> {
                val tIn = inTime.remove(pid) ?: continue
                val delta = max(0, t - tIn)
                total[pid] = (total[pid] ?: 0) + delta
            }

            else -> Unit
        }
    }
    for ((pid, tIn) in inTime) {
        val delta = max(0, nowT - tIn)
        total[pid] = (total[pid] ?: 0) + delta
    }

    return total
}

/**
 * Compute per-player plus-minus based on scoring events and substitutions.
 *
 * Processes the provided live events to track which players are on court and adjusts each on-court
 * player's plus-minus when scoring events occur: TWO_MADE = +2, THREE_MADE = +3, FT_MADE = +1,
 * OPP_TWO_MADE = -2, OPP_THREE_MADE = -3, OPP_FT_MADE = -1. Substitution events (SUB_IN / SUB_OUT)
 * determine on-court membership used to attribute plus-minus.
 *
 * @param events The list of live game events (may be in any order).
 * @return A map from player ID to that player's accumulated plus-minus; players with no plus-minus
 *         adjustments may be absent from the map.
 */
fun computePlusMinusByPlayer(events: List<LiveEvent>): Map<Long, Int> {
    val pm = mutableMapOf<Long, Int>()
    val onCourt = linkedSetOf<Long>()

    val sorted = events.sortedWith(
        compareBy<LiveEvent>({ it.period }, { -it.clockSecRemaining }, { it.createdAt }, { it.id })
    )

    fun deltaPoints(type: EventType) = when (type) {
        EventType.TWO_MADE -> 2
        EventType.THREE_MADE -> 3
        EventType.FT_MADE -> 1
        EventType.OPP_TWO_MADE -> -2
        EventType.OPP_THREE_MADE -> -3
        EventType.OPP_FT_MADE -> -1
        else -> 0
    }

    for (e in sorted) {
        when (e.type) {
            EventType.SUB_IN -> e.playerId?.let { onCourt.add(it); pm.putIfAbsent(it, 0) }
            EventType.SUB_OUT -> e.playerId?.let { onCourt.remove(it) }
            else -> {
                val delta = deltaPoints(e.type)
                if (delta != 0) {
                    onCourt.forEach { pid -> pm[pid] = (pm[pid] ?: 0) + delta }
                }
            }
        }
    }

    return pm
}


private fun toGameElapsedSec(
    period: Int,
    clockSecRemaining: Int,
    quarterLengthSec: Int
): Int {
    val periodIndex = (period - 1).coerceAtLeast(0)
    val elapsedInQuarter = quarterLengthSec - clockSecRemaining
    return periodIndex * quarterLengthSec + elapsedInQuarter
}

fun formatMinutes(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val mm = s / 60
    val ss = s % 60
    return String.format("%d", mm)
}


/**
 * Produce a short, user-facing label for the given game event.
 *
 * @param t The event type to format.
 * @return A concise, human-readable label for the event (for example: "FT ✓", "2PT ✓", "REB O", "Start Q").
 */
fun formatEvent(t: EventType) = when (t) {
    EventType.FT_MADE -> "FT ✓"
    EventType.FT_MISS -> "FT ✗"
    EventType.TWO_MADE -> "2PT ✓"
    EventType.TWO_MISS -> "2PT ✗"
    EventType.THREE_MADE -> "3PT ✓"
    EventType.THREE_MISS -> "3PT ✗"
    EventType.REB_OFF -> "REB O"
    EventType.REB_DEF -> "REB D"
    EventType.AST -> "AST"
    EventType.STL -> "STL"
    EventType.TOV -> "TOV"
    EventType.BLK -> "BLK"
    EventType.PF -> "PF"
    EventType.SUB_IN -> "IN"
    EventType.SUB_OUT -> "OUT"
    EventType.OPP_TWO_MADE -> "2PT ✓"
    EventType.OPP_THREE_MADE -> "3PT ✓"
    EventType.OPP_FT_MADE -> "FT ✓"
    EventType.PERIOD_START -> "Start Q"
    EventType.PERIOD_END -> "End Q"
}

/**
 * Maps an EventType to its concise play-by-play label.
 *
 * @param t The event type to format for play-by-play display.
 * @return A short, human-readable label for the event (e.g., "FT", "2PT", "IN", "Start Q").
 */
fun formatEventPBP(t: EventType) = when (t) {
    EventType.FT_MADE -> "FT"
    EventType.FT_MISS -> "MISS FT"
    EventType.TWO_MADE -> "2PT"
    EventType.TWO_MISS -> "MISS 2PT"
    EventType.THREE_MADE -> "3PT"
    EventType.THREE_MISS -> "MISS 3PT"
    EventType.REB_OFF -> "REB O"
    EventType.REB_DEF -> "REB D"
    EventType.AST -> "AST"
    EventType.STL -> "STL"
    EventType.TOV -> "TOV"
    EventType.BLK -> "BLK"
    EventType.PF -> "PF"
    EventType.SUB_IN -> "IN"
    EventType.SUB_OUT -> "OUT"
    EventType.OPP_TWO_MADE -> "2PT"
    EventType.OPP_THREE_MADE -> "3PT"
    EventType.OPP_FT_MADE -> "FT"
    EventType.PERIOD_START -> "Start Q"
    EventType.PERIOD_END -> "End Q"
}