package com.example.basketballtracker.features.summary.ui

internal fun sortBoxScoreRows(rows: List<GameSummaryPlayerRowUi>, column: String?, ascending: Boolean): List<GameSummaryPlayerRowUi> {
    if (column == null) return rows
    fun value(row: GameSummaryPlayerRowUi): Int = with(row) {
        when (column) {
            "MIN" -> min.split(':').let { (it.firstOrNull()?.toIntOrNull() ?: 0) * 60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) }
            "PTS" -> pts
            "REB" -> reb
            "AST" -> ast
            "DREB" -> rebDef
            "OREB" -> rebOff
            "STL" -> stl
            "BLK" -> blk
            "FG" -> fgm
            "3PT" -> threem
            "FT" -> ftm
            "TO" -> tov
            "PF" -> pf
            "+/-" -> plusMinus
            else -> 0
        }
    }
    fun attempts(row: GameSummaryPlayerRowUi) = when (column) {
        "FG" -> row.fga
        "3PT" -> row.threea
        "FT" -> row.fta
        else -> 0
    }
    val comparator = if (column == "PLAYER") compareBy<GameSummaryPlayerRowUi> { it.playerName.lowercase(java.util.Locale.ROOT) }
        else compareBy<GameSummaryPlayerRowUi> { value(it) }.thenBy { attempts(it) }
    return rows.sortedWith((if (ascending) comparator else comparator.reversed()).thenBy { it.playerId })
}
