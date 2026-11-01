package com.example.basketballtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.players.data.PlayerAverages
import com.example.basketballtracker.features.players.data.PlayerGameStats
import com.example.basketballtracker.features.players.data.buildPlayerGameStats
import com.example.basketballtracker.features.players.ui.*
import com.example.basketballtracker.features.summary.ui.*
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme

// Design fixtures live only in debug builds; previews never read or write the team database.
private val previewPlayer = PlayerEntity(id = 1, name = "Jordan Williams", number = 12)
private val previewGame = GameEntity(id = 1, opponentName = "Tel Aviv", roundNumber = 8,
    gameDateEpoch = 1792972800000, createdAt = 1792972800000, teamScore = 84, opponentScore = 76)
private val previewStats = PlayerGameStats(previewGame, 1865, 24, 8, 6, 2, 5, 2, 1, 3, 2,
    6, 5, 3, 4, 3, 4, emptyList(), emptyList())

@Preview(widthDp = 390, heightDp = 844)
@Composable
fun SummaryDesignPreview() {
    BasketballTrackerTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())) {
            SummaryTopBar("GAME SUMMARY", "Box score and team statistics", {})
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GameResultCard("Tel Aviv", 8, "26/10/2026", 84, 76, true,
                    listOf(QuarterScore(1, 22, 18), QuarterScore(2, 20, 21),
                        QuarterScore(3, 18, 17), QuarterScore(4, 24, 20)))
                TeamSummaryCard(TeamTotals(12000, 84, 21, 42, 31, 11, 8, 4, 12, 18,
                    31, 68, 9, 24, 13, 18))
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
fun RosterDesignPreview() {
    BasketballTrackerTheme {
        RosterScreen(listOf(previewPlayer,
            PlayerEntity(id = 2, name = "Daniel Cohen", number = 4),
            PlayerEntity(id = 3, name = "Alex Thompson", number = 23),
            PlayerEntity(id = 4, name = "Christopher Alexander Montgomery", number = 32)),
            onPlayerClick = {}, onAddPlayer = { _, _, _ -> },
            onEditPlayer = { _, _, _, _ -> }, onBack = {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
fun PlayerDesignPreview() {
    BasketballTrackerTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())) {
            SummaryTopBar("PLAYER PROFILE", previewPlayer.name, {})
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayerHeader(previewPlayer, PlayerAverages(18.4, 7.2, 4.6, 1.8, 0.6, 2.1), 12)
                PlayerGameCard(previewStats, {})
                SheetStatsRows(previewStats)
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
fun PlayerSheetDesignPreview() {
    BasketballTrackerTheme {
        GameStatsBottomSheet(previewPlayer.id, previewPlayer.name, null,
            buildPlayerGameStats(previewPlayer.id, previewGame, boxScorePreviewState().events), {})
    }
}

internal fun boxScorePreviewState(): GameSummaryUiState {
    fun event(player: Long, type: String, period: Int, clock: Int, x: Float, y: Float) =
        EventEntity(gameId = previewGame.id, playerId = player, type = type, period = period,
            clockSecRemaining = clock, createdAt = 0, shotX = x, shotY = y)
    fun row(id: Long, name: String, starter: Boolean, pts: Int) = GameSummaryPlayerRowUi(
        playerId = id, playerName = name, playerNumber = id.toInt(), isStarter = starter,
        min = "12", pts = pts, reb = 0, ast = 0, rebDef = 0, rebOff = 0, stl = 0, blk = 0,
        fgm = if (starter) 3 else 1, fga = if (starter) 4 else 1,
        threem = if (starter) 1 else 0, threea = if (starter) 1 else 0,
        ftm = 0, fta = 0, tov = 0, pf = 0, plusMinus = 0)
    return GameSummaryUiState(game = previewGame,
        opponentName = previewGame.opponentName, roundNumber = 8, dateText = "26/10/2026",
        teamScore = 9, opponentScore = 4, isWin = true,
        teamTotals = TeamTotals(0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 4, 5, 1, 1, 0, 0),
        quarterScores = listOf(QuarterScore(1, 5, 4), QuarterScore(2, 4, 0)),
        rows = listOf(row(1, "Jordan Williams", true, 7), row(2, "Sam Cohen", false, 2)),
        events = listOf(event(1, "THREE_MADE", 1, 500, 2f, 9f),
            event(1, "TWO_MISS", 1, 400, 11f, 6f), event(1, "TWO_MADE", 1, 300, 6f, 3f),
            event(1, "TWO_MADE", 2, 300, 9f, 5f), event(2, "TWO_MADE", 2, 200, 5f, 7f)))
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
fun BoxScoreDesignPreview() {
    BasketballTrackerTheme { GameSummaryContent(boxScorePreviewState(), {}) }
}
