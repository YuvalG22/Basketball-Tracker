package com.example.basketballtracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.features.home.ui.HomeContent
import com.example.basketballtracker.features.home.ui.HomeState
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenInteractionTest {
    @get:Rule val compose = createComposeRule()
    private fun game(id: Long, name: String) = GameEntity(id = id, opponentName = name,
        roundNumber = 4, gameDateEpoch = 0, createdAt = 0, teamScore = 102, opponentScore = 67)

    @Test fun narrowScreenSupportsStartingAndOpeningRecentResultAndHistory() {
        var started = 0
        var summary: Long? = null
        var history = 0
        compose.setContent {
            BasketballTrackerTheme {
                Box(Modifier.width(360.dp)) {
                    HomeContent(HomeState(loading = false, recentGames = listOf(game(9, "Falcons"))),
                        onSync = {}, onNewGame = { started++ }, onContinue = {},
                        onGameSummary = { summary = it }, onHistory = { history++ })
                }
            }
        }
        compose.onNodeWithText("Start new game").performClick()
        assertEquals(1, started)
        compose.onNodeWithText("Falcons").performScrollTo().performClick()
        assertEquals(9L, summary)
        compose.onNodeWithText("All games").performScrollTo().performClick()
        assertEquals(1, history)
    }

    @Test fun resumeTargetsCorrectGameAndOtherUnfinishedGamesRemainReachable() {
        var resumed: Long? = null
        var started = 0
        compose.setContent {
            BasketballTrackerTheme {
                HomeContent(HomeState(loading = false, liveGames = listOf(
                    game(2, "Falcons").copy(status = GameStatus.LIVE),
                    game(1, "Eagles").copy(status = GameStatus.LIVE))),
                    onSync = {}, onNewGame = { started++ }, onContinue = { resumed = it },
                    onGameSummary = {}, onHistory = {})
            }
        }
        compose.onNodeWithText("Resume game").performClick()
        assertEquals(2L, resumed)
        compose.onNodeWithText("Eagles").performScrollTo().performClick()
        assertEquals(1L, resumed)
        compose.onNodeWithText("Start a different game").performScrollTo().performClick()
        assertEquals(1, started)
    }

    @Test fun loadingDoesNotShowEmptyStateAndSyncCannotBeTappedTwice() {
        compose.setContent {
            BasketballTrackerTheme {
                HomeContent(HomeState(), isSyncing = true, onSync = {}, onNewGame = {},
                    onContinue = {}, onGameSummary = {}, onHistory = {})
            }
        }
        compose.onNodeWithText("Loading games…").assertIsDisplayed()
        compose.onNodeWithText("Start new game").assertDoesNotExist()
        compose.onNodeWithText("Syncing…").assertIsNotEnabled()
    }
}
