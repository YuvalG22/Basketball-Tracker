package com.example.basketballtracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.features.players.data.buildPlayerGameStats
import com.example.basketballtracker.features.players.ui.GameStatsBottomSheet
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import org.junit.Rule
import org.junit.Test

class GameStatsBottomSheetInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun showSheet() {
        val game = GameEntity(
            id = 1, opponentName = "Opponent", roundNumber = 1,
            gameDateEpoch = 0, createdAt = 0
        )
        val stats = buildPlayerGameStats(1, game, emptyList())
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            BasketballTrackerTheme {
                if (visible) {
                    GameStatsBottomSheet(1, "Test Player", null, stats, { visible = false })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun pullSheet(distanceDp: Int, durationMillis: Long) {
        val distance = with(compose.density) { distanceDp.dp.toPx() }
        compose.onNodeWithText("Test Player").performTouchInput {
            swipe(center, center + Offset(0f, distance), durationMillis)
        }
        compose.waitForIdle()
    }

    @Test
    fun smallDownwardDragKeepsSheetOpen() {
        showSheet()
        pullSheet(80, 1000)
        compose.onNodeWithText("Test Player").assertIsDisplayed()
    }

    @Test
    fun shortFlickKeepsSheetOpen() {
        showSheet()
        pullSheet(40, 80)
        compose.onNodeWithText("Test Player").assertIsDisplayed()
    }

    @Test
    fun deliberateDownwardDragClosesSheet() {
        showSheet()
        pullSheet(240, 1000)
        compose.onNodeWithText("Test Player").assertDoesNotExist()
    }

    @Test
    fun statsRemainScrollable() {
        showSheet()
        compose.onNodeWithText("Turnovers").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Test Player").performScrollTo().assertIsDisplayed()
    }
}
