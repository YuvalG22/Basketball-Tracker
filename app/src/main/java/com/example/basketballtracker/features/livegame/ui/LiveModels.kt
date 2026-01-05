package com.example.basketballtracker.features.livegame.ui

import kotlin.math.roundToInt

enum class EventType {
    FT_MADE, FT_MISS,
    TWO_MADE, TWO_MISS,
    THREE_MADE, THREE_MISS,
    REB_OFF, REB_DEF,
    AST, STL, TOV, BLK, PF,
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
    val createdAt: Long
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
    val tov: Int
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

fun computeBoxByPlayer(events: List<LiveEvent>): Map<Long, PlayerBox> {
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
            tov = c(EventType.TOV)
        )
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
}