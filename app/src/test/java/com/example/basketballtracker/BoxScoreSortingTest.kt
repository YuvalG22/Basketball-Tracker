package com.example.basketballtracker

import com.example.basketballtracker.features.summary.ui.GameSummaryPlayerRowUi
import com.example.basketballtracker.features.summary.ui.sortBoxScoreRows
import org.junit.Assert.assertEquals
import org.junit.Test

class BoxScoreSortingTest {
    private val base = GameSummaryPlayerRowUi(playerId = 1, playerName = "Zed", playerNumber = 1,
        isStarter = true, min = "9:59", pts = 2, reb = 0, ast = 0, rebDef = 0, rebOff = 0,
        stl = 0, blk = 0, fgm = 1, fga = 2, threem = 0, threea = 0, ftm = 0, fta = 0,
        tov = 0, pf = 0, plusMinus = -3)
    private val other = base.copy(playerId = 2, playerName = "Amy", isStarter = false,
        min = "10:01", pts = 10, fga = 5, plusMinus = 2)

    @Test fun sortsNumericallyAcrossStarterAndBench() {
        for (column in listOf("MIN", "PTS", "+/-", "FG")) {
            assertEquals(listOf(other, base), sortBoxScoreRows(listOf(base, other), column, false))
            assertEquals(listOf(base, other), sortBoxScoreRows(listOf(other, base), column, true))
        }
    }
    @Test fun namesAndResetAndTies() {
        assertEquals(listOf(other, base), sortBoxScoreRows(listOf(base, other), "PLAYER", true))
        assertEquals(listOf(base, other), sortBoxScoreRows(listOf(base, other), null, false))
        assertEquals(listOf(base, other), sortBoxScoreRows(listOf(other, base), "STL", false))
    }
}
