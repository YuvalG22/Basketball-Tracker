package com.example.basketballtracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.newgame.ui.NewGameDraft
import com.example.basketballtracker.features.newgame.ui.NewGameSetupContent
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NewGameSetupInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val players = (1..6).map { PlayerEntity(id = it.toLong(), name = "Player $it", number = it) }

    private fun rosterNode(text: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("setup-roster").performScrollToNode(hasText(text))
        return compose.onNodeWithText(text)
    }

    @Test fun searchBulkSelectionAndBackPreserveSetup() {
        var submitted: NewGameDraft? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            BasketballTrackerTheme {
                Box(Modifier.width(390.dp)) {
                    NewGameSetupContent(players, onBack = {}, onSubmit = { submitted = it })
                }
            }
        }
        compose.onNodeWithText("Continue to roster").assertIsNotEnabled()
        compose.onNodeWithText("Opponent").performTextInput("Falcons")
        compose.onNodeWithText("Continue to roster").performClick()
        compose.onNodeWithText("Start game").assertIsNotEnabled()
        rosterNode("Search name or jersey number").performTextInput("#2")
        rosterNode("Select all").performClick()
        compose.onNodeWithText("Start game").assertIsEnabled()
        compose.onNodeWithText("Back to details").performClick()
        compose.onNodeWithText("Falcons").assertExists()
        compose.onNodeWithText("Continue to roster").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Start game").performClick()
        assertEquals("Falcons", submitted?.opponent)
        assertEquals(6, submitted?.playerIds?.size)
        rosterNode("Clear selection").performClick()
        compose.onNodeWithText("Start game").assertIsNotEnabled()
    }

    @Test fun playoffRequiresPositiveGameNumber() {
        compose.setContent { BasketballTrackerTheme { NewGameSetupContent(players, onBack = {}, onSubmit = {}) } }
        compose.onNodeWithText("Opponent").performTextInput("Eagles")
        compose.onNodeWithText("Opponent").performImeAction()
        compose.onNodeWithTag("setup-details").performScrollToNode(hasTestTag("choice-Playoff"))
        compose.onNodeWithTag("choice-Playoff").performClick()
        compose.onNodeWithTag("setup-details").performScrollToNode(hasText("Game number"))
        compose.onNodeWithText("Game number").performTextReplacement("0")
        compose.onNodeWithText("Continue to roster").assertIsNotEnabled()
        compose.onNodeWithText("Game number").performTextReplacement("2")
        compose.onNodeWithText("Continue to roster").assertIsEnabled()
    }

    @Test fun savingBlocksRepeatSubmissionAndFailureKeepsSelection() {
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        var submissions = 0
        compose.setContent { BasketballTrackerTheme {
            NewGameSetupContent(players, saving = saving, error = error, onBack = {}, onSubmit = { submissions++; saving = true })
        } }
        compose.onNodeWithText("Opponent").performTextInput("Eagles")
        compose.onNodeWithText("Opponent").performImeAction()
        compose.onNodeWithText("Continue to roster").performClick()
        rosterNode("Select all").performClick()
        compose.onNodeWithText("Start game").performClick()
        compose.onNodeWithText("Starting game\u2026").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(1, submissions); saving = false; error = "Save failed" }
        compose.onNodeWithText("Save failed").assertIsDisplayed()
        compose.onNodeWithText("Retry start game").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, submissions) }
    }

    @Test fun loadingEmptyAndSearchEmptyAreDistinct() {
        var roster by mutableStateOf<List<PlayerEntity>?>(null)
        compose.setContent { BasketballTrackerTheme { NewGameSetupContent(roster, onBack = {}, onSubmit = {}) } }
        compose.onNodeWithText("Opponent").performTextInput("Eagles")
        compose.onNodeWithText("Opponent").performImeAction()
        compose.onNodeWithText("Continue to roster").performClick()
        compose.onNodeWithText("Loading players\u2026").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { roster = emptyList() }
        compose.onNodeWithText("No players yet.", substring = true).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { roster = players }
        rosterNode("Search name or jersey number").performTextInput("missing")
        compose.onNodeWithTag("setup-roster").performScrollToNode(hasText("No players match your search."))
        compose.onNodeWithText("No players match your search.").assertIsDisplayed()
    }
}
