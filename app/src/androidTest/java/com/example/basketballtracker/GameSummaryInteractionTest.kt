package com.example.basketballtracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.basketballtracker.features.summary.ui.GameSummaryContent
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import org.junit.Rule
import org.junit.Test

class GameSummaryInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun showSummary() {
        compose.setContent {
            BasketballTrackerTheme { GameSummaryContent(boxScorePreviewState(), {}) }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
    }

    @Test
    fun tappingStarterNameOpensShotChartAndQuarterControls() {
        showSummary()
        compose.onNodeWithText("Jordan Williams").performScrollTo().performClick()
        compose.onNodeWithContentDescription("No player photo").assertIsDisplayed()
        compose.onNodeWithText("○ Made").assertExists()
        compose.onNodeWithText("× Missed").assertExists()
        compose.onNodeWithText("Q2").performClick()
        compose.onNodeWithText("PERFORMANCE").assertExists()
    }

    @Test
    fun tappingNumericCellsOpensSamePlayerSheet() {
        showSummary()
        compose.onNode(hasClickAction() and hasText("7")).performScrollTo().performClick()
        compose.onNodeWithContentDescription("No player photo").assertIsDisplayed()
        compose.onNodeWithText("○ Made").assertExists()
    }

    @Test
    fun benchPlayersAlsoOpenTheirGameSheet() {
        showSummary()
        compose.onNodeWithText("Sam Cohen").performScrollTo().performClick()
        compose.onNodeWithContentDescription("No player photo").assertIsDisplayed()
        compose.onNodeWithText("○ Made").assertExists()
    }
}
