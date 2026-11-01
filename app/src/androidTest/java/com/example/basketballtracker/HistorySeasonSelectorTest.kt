package com.example.basketballtracker

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.basketballtracker.features.history.ui.HistorySeasonSelector
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import org.junit.Rule
import org.junit.Test

class HistorySeasonSelectorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectsLegacyCurrentAndAllSeasons() {
        compose.setContent {
            var selected by remember { mutableStateOf<String?>("26/27") }
            BasketballTrackerTheme {
                HistorySeasonSelector(selected, listOf("26/27", "25/26")) { selected = it }
            }
        }
        compose.onNodeWithText("Season: 26/27").performClick()
        compose.onNodeWithText("25/26").performClick()
        compose.onNodeWithText("Season: 25/26").assertIsDisplayed().performClick()
        compose.onNodeWithText("All seasons").performClick()
        compose.onNodeWithText("Season: All seasons").assertIsDisplayed().performClick()
        compose.onNodeWithText("26/27").performClick()
        compose.onNodeWithText("Season: 26/27").assertIsDisplayed()
    }
}
