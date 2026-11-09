package com.example.basketballtracker

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.core.data.remote.games.*
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.newgame.ui.NewGameSetupContent
import com.example.basketballtracker.features.newgame.ui.NewGameDraft
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScheduledGameInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val fixture = GameEntity(opponentName = "Future opponent", roundNumber = 3,
        gameDateEpoch = 1796853600000L, createdAt = 0, isHomeGame = true,
        season = GameSeason.CURRENT, status = GameStatus.SCHEDULED,
        syncKey = "easycoach:26/27:test", syncStatus = "SYNCED", remoteId = "fixture")

    @Test fun rosterStartsDirectlyWithLockedDetailsAndCanCancel() {
        var draft: NewGameDraft? = null
        var back = false
        val players = (1L..6L).map { PlayerEntity(id = it, name = "Player $it", number = it.toInt()) }
        compose.setContent { BasketballTrackerTheme {
            NewGameSetupContent(players, onBack = { back = true }, scheduledGame = fixture) { draft = it }
        } }
        compose.onNodeWithTag("setup-details").assertDoesNotExist()
        compose.onNodeWithText("Edit details").assertDoesNotExist()
        compose.onNodeWithText("Start game").assertIsNotEnabled()
        compose.onNodeWithTag("setup-roster").performScrollToNode(hasText("Select all"))
        compose.onNodeWithText("Select all").performClick()
        compose.onNodeWithText("Start game").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals("Future opponent", draft?.opponent)
            assertEquals(true, draft?.home)
            assertEquals(3, draft?.round)
            assertEquals(6, draft?.playerIds?.size)
        }
        compose.onNodeWithText("Back", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertTrue(back) }
    }

    @Test fun startIsAtomicOfflineAndRetryPreservesClockAndRoster() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java).build()
        val api = object : GameApi {
            override suspend fun uploadGame(game: GameUploadDto): GameUploadResponse = error("Network must not be used")
            override suspend fun updateGame(remoteId: String, body: GameUploadDto): GameUploadResponse = error("Network must not be used")
            override suspend fun updateGameScore(body: GameScoreUpdateDto) = error("Network must not be used")
            override suspend fun getGames(includeDeleted: Boolean): List<GameRemoteDto> = error("Network must not be used")
            override suspend fun deleteGameFromCloud(remoteId: String) = error("Network must not be used")
        }
        val repo = GamesRepository(db.gameDao(), api, database = db)
        try {
            val id = db.gameDao().insert(fixture)
            val ids = (1..6).map { db.playerDao().insert(PlayerEntity(name = "Player $it", number = it)) }
            try { repo.startScheduledGame(id, ids.take(4), 480); fail("Expected invalid roster") }
            catch (_: IllegalArgumentException) { }
            assertEquals(GameStatus.SCHEDULED, db.gameDao().getById(id)!!.status)
            assertTrue(db.rosterDao().observeRosterPlayerIds(id).first().isEmpty())
            repo.startScheduledGame(id, ids.take(5), 480)
            val live = db.gameDao().getById(id)!!
            assertEquals(GameStatus.LIVE, live.status)
            assertEquals(480, live.clockSecRemaining)
            assertEquals(fixture.gameDateEpoch, live.gameDateEpoch)
            assertEquals(fixture.syncKey, live.syncKey)
            assertEquals("PENDING", live.syncStatus)
            db.gameDao().update(live.copy(clockSecRemaining = 123, teamScore = 7))
            repo.startScheduledGame(id, ids, 600)
            assertEquals(123, db.gameDao().getById(id)!!.clockSecRemaining)
            assertEquals(7, db.gameDao().getById(id)!!.teamScore)
            assertEquals(ids.take(5).toSet(), db.rosterDao().observeRosterPlayerIds(id).first().toSet())
            assertEquals(1, db.gameDao().observeAllGames().first().size)
            repo.finishGame(id, 7, 0)
            try { repo.startScheduledGame(id, ids, 600); fail("Finished game must not restart") }
            catch (_: IllegalStateException) { }
        } finally { db.close() }
    }
}
