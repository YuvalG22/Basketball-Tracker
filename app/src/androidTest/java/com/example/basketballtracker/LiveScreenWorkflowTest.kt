package com.example.basketballtracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.basketballtracker.core.data.db.*
import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.domain.*
import com.example.basketballtracker.features.livegame.ui.*
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy

class LiveScreenWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private inline fun <reified T> offline(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ ->
        throw java.io.IOException("Offline test")
    } as T

    @Test fun narrowScreenRecordsAssistedBasketAndUndoesItTogether() = exercise(390)
    @Test fun tabletRecordsAssistedBasketAndUndoesItTogether() = exercise(1100)

    private fun exercise(width: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = ViewModelStore()
        val players = (1..6).map { PlayerEntity(id = it.toLong(), name = "Player $it", number = it) }
        runBlocking {
            db.gameDao().insert(GameEntity(id = 1, opponentName = "Falcons", roundNumber = 1,
                gameDateEpoch = 0, createdAt = 0, status = GameStatus.LIVE))
            db.eventDao().insertAll((1..5).map { EventEntity(gameId = 1, playerId = it.toLong(), type = "SUB_IN",
                period = 1, clockSecRemaining = 600, createdAt = it.toLong()) })
        }
        lateinit var vm: LiveGameViewModel
        compose.runOnUiThread {
            vm = LiveGameViewModel(LiveGameRepository(db.eventDao(), db.gameDao(), db.playerDao(), offline()),
                GamesRepository(db.gameDao(), offline()),
                SyncManager(db.gameDao(), db.playerDao(), db.rosterDao(), db.eventDao(), db.playerSeasonStatsDao(),
                    offline(), offline(), offline(), offline(), offline()), 1, players)
            store.put("live", vm)
        }
        try {
            compose.setContent { BasketballTrackerTheme { Box(Modifier.width(width.dp)) { LiveRecordingContent(vm, {}) } } }
            compose.waitUntil(5000) { vm.ui.value.events.size == 5 }
            if (width < 840) compose.onNodeWithText("Roster").assertExists()
            compose.runOnIdle { vm.selectPlayer(1); vm.addEvent(EventType.TWO_MADE, shotMeta = ShotMeta(7f, 3f, 3f)) }
            compose.onNodeWithText("Assist?").assertIsDisplayed()
            val pending = vm.pendingMadeShot.value!!
            compose.onNodeWithText("#2  Player 2").performClick()
            compose.waitUntil(5000) { vm.pendingMadeShot.value == null && !vm.saving.value }
            assertEquals(2, computeTeamScore(vm.ui.value.events))
            val recorded = vm.ui.value.events.filter { it.type == EventType.TWO_MADE || it.type == EventType.AST }
            assertEquals(2, recorded.size)
            assertTrue(recorded.all { it.period == pending.period && it.clockSecRemaining == pending.clockSecRemaining })
            assertEquals(1, recorded.map { it.actionGroupId }.distinct().size)
            compose.onNodeWithText(if (width < 840) "History" else "Play-by-Play").performClick()
            compose.onNodeWithContentDescription("undo event").performClick()
            compose.waitUntil(5000) { !vm.saving.value && vm.ui.value.events.size == 5 }
            assertEquals(0, computeTeamScore(vm.ui.value.events))
            compose.runOnIdle { vm.substitute(1, 6) }
            compose.waitUntil(5000) { !vm.saving.value && 6L in computeOnCourtIds(vm.ui.value.events) }
            assertFalse(1L in computeOnCourtIds(vm.ui.value.events))
            compose.onNodeWithContentDescription("undo event").performClick()
            compose.waitUntil(5000) { !vm.saving.value && 1L in computeOnCourtIds(vm.ui.value.events) }
            assertFalse(6L in computeOnCourtIds(vm.ui.value.events))
        } finally { compose.runOnUiThread { store.clear() }; db.close() }
    }
}
