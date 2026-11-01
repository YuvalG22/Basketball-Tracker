package com.example.basketballtracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.players.ui.RosterScreen
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PlayerEditingTest {
    @get:Rule val compose = createComposeRule()
    private val original = PlayerEntity(
        id = 42, name = "Alex", number = 7, remoteId = "remote-42",
        syncStatus = "SYNCED", photoUri = "content://photos/42"
    )
    private var edited: PlayerEntity? = null
    private var openedPlayer: Long? = null

    private fun showRoster() {
        var player by mutableStateOf(original)
        compose.setContent {
            BasketballTrackerTheme {
                RosterScreen(
                    players = listOf(player),
                    onPlayerClick = { openedPlayer = it },
                    onAddPlayer = { _, _, _ -> },
                    onEditPlayer = { id, name, number, photo ->
                        player = player.copy(id = id, name = name, number = number, photoUri = photo)
                        edited = player
                    },
                    onBack = {}
                )
            }
        }
        compose.onNodeWithContentDescription("Edit Alex").performClick()
        compose.onNodeWithText("Edit Player").assertIsDisplayed()
    }

    @Test
    fun editPrefillsValuesAndSavesNameNumberAndPhotoRemoval() {
        showRoster()
        compose.onAllNodes(hasSetTextAction())[0].assertTextEquals("Alex")
            .performTextReplacement("  Alex Updated  ")
        compose.onAllNodes(hasSetTextAction())[1].assertTextEquals("7")
            .performTextReplacement("23")
        compose.onNodeWithText("Remove photo").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Edit Player").assertDoesNotExist()
        compose.onNodeWithText("Alex Updated").assertIsDisplayed()
        compose.onNodeWithText("JERSEY #23").assertIsDisplayed()
        assertEquals(original.copy(name = "Alex Updated", number = 23, photoUri = null), edited)
        assertNull(openedPlayer)
    }

    @Test
    fun cancelDiscardsDraftAndReopeningShowsOriginalValues() {
        showRoster()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Discard me")
        compose.onNodeWithText("Remove photo").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertNull(edited)
        compose.onNodeWithContentDescription("Edit Alex").performClick()
        compose.onAllNodes(hasSetTextAction())[0].assertTextEquals("Alex")
        compose.onNodeWithText("Remove photo").assertExists()
    }

    @Test
    fun invalidFieldsDisableSaving() {
        showRoster()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("   ")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Alex")
        compose.onAllNodes(hasSetTextAction())[1].performTextClearance()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("999999999999999")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        assertNull(edited)
    }
}
