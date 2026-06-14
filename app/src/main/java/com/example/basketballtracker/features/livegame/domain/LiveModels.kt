package com.example.basketballtracker.features.livegame.domain

import com.example.basketballtracker.core.data.db.entities.EventEntity
import kotlin.collections.iterator
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
    OPP_PF,

    PERIOD_START,
    PERIOD_END;

    fun isOpponentEvent(): Boolean = when (this) {
        OPP_TWO_MADE, OPP_THREE_MADE, OPP_FT_MADE, OPP_PF -> true
        else -> false
    }

    fun isScoreEvent(): Boolean = when (this) {
        TWO_MADE, THREE_MADE, FT_MADE, OPP_TWO_MADE, OPP_THREE_MADE, OPP_FT_MADE, PERIOD_START, PERIOD_END -> true
        else -> false
    }

    fun isShotEvent(): Boolean = when (this) {
        TWO_MADE, TWO_MISS, THREE_MADE, THREE_MISS -> true
        else -> false
    }

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

data class ShotMeta(
    val x: Float,
    val y: Float,
    val distance: Float
)

enum class ShotZone {
    RESTRICTED_AREA,
    PAINT,

    LEFT_SHORT_MID,
    RIGHT_SHORT_MID,

    LEFT_CORNER_MID,
    RIGHT_CORNER_MID,
    LEFT_WING_MID,
    RIGHT_WING_MID,
    TOP_MID,

    LEFT_CORNER_3,
    RIGHT_CORNER_3,
    LEFT_WING_3,
    RIGHT_WING_3,
    TOP_3
}

data class LiveEvent(
    val id: Long,
    val gameId: Long,
    val playerId: Long?,
    val assistedByPlayerId: Long? = null,
    val type: EventType,
    val period: Int,
    val clockSecRemaining: Int,
    val createdAt: Long,
    val teamScoreAtEvent: Int?,
    val opponentScoreAtEvent: Int?,
    val shotX: Float? = null,
    val shotY: Float? = null,
    val shotDistance: Float? = null,
    val shotZone: String? = null
)

data class PlayerBox(
    val playerId: Long,
    var secondsPlayed: Int = 0,
    var plusMinus: Int = 0,
    var pts: Int = 0,
    var twom: Int = 0, var twoa: Int = 0,
    var threem: Int = 0, var threea: Int = 0,
    var ftm: Int = 0, var fta: Int = 0,
    var rebOff: Int = 0,
    var rebDef: Int = 0,
    var ast: Int = 0,
    var tov: Int = 0,
    var stl: Int = 0,
    var blk: Int = 0,
    var pf: Int = 0,
) {
    val fgm get() = twom + threem
    val fga get() = twoa + threea
    val rebTotal get() = rebOff + rebDef

    val ftPct get() = pct(ftm, fta)
    val fgPct get() = pct(fgm, fga)
    val threePct get() = pct(threem, threea)

    private fun pct(made: Int, att: Int): Int {
        if (att == 0) return 0
        return ((made * 100.0) / att).roundToInt()
    }
}

fun String?.toShotZoneOrNull(): ShotZone? {
    return try {
        if (this == null) null
        else ShotZone.valueOf(this)
    } catch (e: Exception) {
        null
    }
}

data class ZoneStats(
    val zone: ShotZone,
    val made: Int,
    val attempted: Int
) {
    val percentage: Float
        get() = if (attempted == 0) 0f
        else made.toFloat() / attempted
}

fun computeBoxByPlayer(
    events: List<LiveEvent>,
    quarterLengthSec: Int,
    currentPeriod: Int,
    currentClockSecRemaining: Int,
): Map<Long, PlayerBox> {

    val minutesMap = computeSecondsPlayedByPlayer(
        events,
        quarterLengthSec,
        currentPeriod,
        currentClockSecRemaining
    )
    val plusMinusMap = computePlusMinusByPlayer(events)

    val boxMap = mutableMapOf<Long, PlayerBox>()

    for (e in events) {
        val pid = e.playerId ?: continue

        val box = boxMap.getOrPut(pid) {
            PlayerBox(
                playerId = pid,
                plusMinus = plusMinusMap[pid] ?: 0,
                secondsPlayed = minutesMap[pid] ?: 0
            )
        }

        when (e.type) {
            EventType.FT_MADE -> {
                box.ftm++; box.fta++; box.pts += 1
            }

            EventType.FT_MISS -> box.fta++

            EventType.TWO_MADE -> {
                box.twom++; box.twoa++; box.pts += 2
            }

            EventType.TWO_MISS -> box.twoa++

            EventType.THREE_MADE -> {
                box.threem++; box.threea++; box.pts += 3
            }

            EventType.THREE_MISS -> box.threea++

            EventType.REB_OFF -> box.rebOff++
            EventType.REB_DEF -> box.rebDef++
            EventType.AST -> box.ast++
            EventType.TOV -> box.tov++
            EventType.STL -> box.stl++
            EventType.BLK -> box.blk++
            EventType.PF -> box.pf++

            else -> Unit
        }
    }
    return boxMap
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

fun computeTeamFoulsThisPeriod(
    events: List<LiveEvent>,
    gameId: Long,
    period: Int
): Int {
    return events.count {
        it.gameId == gameId &&
                it.type == EventType.PF &&
                it.period == period
    }
}

fun computeOpponentFoulsThisPeriod(
    events: List<LiveEvent>,
    gameId: Long,
    period: Int
): Int {
    return events.count {
        it.gameId == gameId &&
                it.type == EventType.OPP_PF &&
                it.period == period
    }
}


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
    EventType.OPP_PF -> "PF"
    EventType.PERIOD_START -> "Start Q"
    EventType.PERIOD_END -> "End Q"
}

fun formatEventPBP(t: EventType) = when (t) {
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
    EventType.OPP_PF -> "PF"
    EventType.PERIOD_START -> "Start Q"
    EventType.PERIOD_END -> "End Q"
}

fun computeSecondsPlayedForPlayer(
    playerId: Long?,
    events: List<LiveEvent>,
    quarterLengthSec: Int,
    currentPeriod: Int,
    currentClockSecRemaining: Int
): Int {
    val nowT = toGameElapsedSec(
        period = currentPeriod,
        clockSecRemaining = currentClockSecRemaining,
        quarterLengthSec = quarterLengthSec
    )

    val sorted = events.sortedWith(
        compareBy<LiveEvent>({ it.period }, { -it.clockSecRemaining }, { it.createdAt })
    )

    var inTime: Int? = null
    var total = 0

    for (e in sorted) {
        if (e.playerId != playerId) continue
        if (e.type != EventType.SUB_IN && e.type != EventType.SUB_OUT) continue

        val t = toGameElapsedSec(e.period, e.clockSecRemaining, quarterLengthSec)

        when (e.type) {
            EventType.SUB_IN -> {
                if (inTime == null) {
                    inTime = t
                }
            }

            EventType.SUB_OUT -> {
                val tIn = inTime ?: continue
                total += max(0, t - tIn)
                inTime = null
            }

            else -> Unit
        }
    }

    if (inTime != null) {
        total += max(0, nowT - inTime)
    }

    return total
}

fun computeSecondsPlayedForPlayerInPeriod(
    playerId: Long?,
    events: List<LiveEvent>,
    period: Int,
    quarterLengthSec: Int
): Int {
    val periodEvents = events
        .filter { it.period == period }
        .sortedWith(compareBy<LiveEvent>({ -it.clockSecRemaining }, { it.createdAt }))

    var inTime: Int? = null
    var total = 0

    for (e in periodEvents) {
        if (e.playerId != playerId) continue
        if (e.type != EventType.SUB_IN && e.type != EventType.SUB_OUT) continue

        val t = quarterLengthSec - e.clockSecRemaining

        when (e.type) {
            EventType.SUB_IN -> {
                if (inTime == null) inTime = t
            }

            EventType.SUB_OUT -> {
                val tIn = inTime ?: continue
                total += max(0, t - tIn)
                inTime = null
            }

            else -> Unit
        }
    }

    if (inTime != null) {
        total += max(0, quarterLengthSec - inTime)
    }

    return total
}

fun computePlusMinusForPlayer(
    playerId: Long,
    events: List<LiveEvent>
): Int {
    var pm = 0
    val onCourt = linkedSetOf<Long>()

    val sorted = events.sortedWith(
        compareBy<LiveEvent>(
            { it.period },
            { -it.clockSecRemaining },
            { it.createdAt },
            { it.id }
        )
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
            EventType.SUB_IN -> {
                e.playerId?.let { onCourt.add(it) }
            }

            EventType.SUB_OUT -> {
                e.playerId?.let { onCourt.remove(it) }
            }

            else -> {
                val delta = deltaPoints(e.type)
                if (delta != 0 && playerId in onCourt) {
                    pm += delta
                }
            }
        }
    }

    return pm
}

fun computeSecondsPlayedForPlayerAfterEnd(
    playerId: Long?,
    events: List<EventEntity>,
    quarterLengthSec: Int,
    currentPeriod: Int,
    currentClockSecRemaining: Int
): Int {
    val nowT = toGameElapsedSec(
        period = currentPeriod,
        clockSecRemaining = currentClockSecRemaining,
        quarterLengthSec = quarterLengthSec
    )

    val sorted = events.sortedWith(
        compareBy<EventEntity>({ it.period }, { -it.clockSecRemaining }, { it.createdAt })
    )

    var inTime: Int? = null
    var total = 0

    for (e in sorted) {
        if (e.playerId != playerId) continue
        if (e.type != EventType.SUB_IN.toString() && e.type != EventType.SUB_OUT.toString()) continue

        val t = toGameElapsedSec(e.period, e.clockSecRemaining, quarterLengthSec)

        when (e.type) {
            EventType.SUB_IN.toString() -> {
                if (inTime == null) {
                    inTime = t
                }
            }

            EventType.SUB_OUT.toString() -> {
                val tIn = inTime ?: continue
                total += max(0, t - tIn)
                inTime = null
            }

            else -> Unit
        }
    }

    if (inTime != null) {
        total += max(0, nowT - inTime)
    }

    return total
}

fun computeSecondsPlayedForPlayerInPeriodAfterEnd(
    playerId: Long?,
    events: List<EventEntity>,
    period: Int,
    quarterLengthSec: Int
): Int {
    val periodEvents = events
        .filter { it.period == period }
        .sortedWith(compareBy<EventEntity>({ -it.clockSecRemaining }, { it.createdAt }))

    var inTime: Int? = null
    var total = 0

    for (e in periodEvents) {
        if (e.playerId != playerId) continue
        if (e.type != EventType.SUB_IN.toString() && e.type != EventType.SUB_OUT.toString()) continue

        val t = quarterLengthSec - e.clockSecRemaining

        when (e.type) {
            EventType.SUB_IN.toString() -> {
                if (inTime == null) inTime = t
            }

            EventType.SUB_OUT.toString() -> {
                val tIn = inTime ?: continue
                total += max(0, t - tIn)
                inTime = null
            }

            else -> Unit
        }
    }

    if (inTime != null) {
        total += max(0, quarterLengthSec - inTime)
    }

    return total
}