package com.example.basketballtracker

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.basketballtracker.core.data.db.*
import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.core.data.remote.events.*
import com.example.basketballtracker.core.data.remote.games.*
import com.example.basketballtracker.core.data.remote.players.*
import com.example.basketballtracker.core.data.remote.roster.*
import com.example.basketballtracker.core.data.remote.stats.*
import com.example.basketballtracker.features.livegame.data.toDomain
import com.example.basketballtracker.features.livegame.domain.computeTeamScore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.UUID

/** Run only against scripts/serve-sync-test.js, which uses private temporary tables. */
class SyncEndToEndTest {
    @Test fun twoInstallationsRecoverRetriesMapReferencesAndPropagateUndo() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val url = args.getString("syncTestUrl")
        assumeTrue("Requires isolated temporary-table API", url != null && args.getString("syncTestIsolated") == "true")
        val retrofit = Retrofit.Builder().baseUrl(url!!).addConverterFactory(GsonConverterFactory.create()).build()
        val events = retrofit.create(EventApi::class.java)
        val games = retrofit.create(GameApi::class.java)
        val players = retrofit.create(PlayerApi::class.java)
        val roster = retrofit.create(RosterApi::class.java)
        val stats = retrofit.create(StatsApi::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "sync-e2e-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var a = open()
        val b = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        fun sync(db: AppDatabase, eventApi: EventApi = events, gameApi: GameApi = games) = SyncManager(db.gameDao(), db.playerDao(), db.rosterDao(),
            db.eventDao(), db.playerSeasonStatsDao(), gameApi, players, roster, eventApi, stats, db)
        try {
            val shooter = a.playerDao().insert(PlayerEntity(name = "Shooter", number = 7))
            val assist = a.playerDao().insert(PlayerEntity(name = "Assist", number = 8))
            val game = a.gameDao().insert(GameEntity(opponentName = "E2E", roundNumber = 1, gameDateEpoch = 1,
                createdAt = 1, status = GameStatus.LIVE, season = GameSeason.CURRENT))
            a.rosterDao().insertAll(listOf(RosterEntity(game, shooter), RosterEntity(game, assist)))
            fun event(type: String, player: Long?, assisted: Long? = null) = EventEntity(gameId = game, playerId = player,
                assistedByPlayerId = assisted, type = type, period = 1, clockSecRemaining = 500, createdAt = 100)
            a.eventDao().recordAction(listOf(event("SUB_IN", shooter), event("SUB_IN", assist)))
            a.eventDao().recordAction(listOf(event("TWO_MADE", shooter, assist), event("AST", assist)))
            sync(a).synchronize()
            val remoteGame = a.gameDao().getById(game)!!.remoteId!!
            val remoteAssist = a.playerDao().getPlayerById(assist)!!.remoteId!!
            assertEquals(2, stats.snapshot().games.first { it.id == remoteGame }.team_score)

            // Force different local integer IDs on the second installation.
            repeat(10) { b.playerDao().insert(PlayerEntity(name = "Local only $it", number = 99, syncStatus = "SYNCED")) }
            b.gameDao().insert(GameEntity(opponentName = "Local only", roundNumber = 1, gameDateEpoch = 1,
                createdAt = 1, syncStatus = "SYNCED"))
            sync(b).synchronize()
            val gameB = b.gameDao().getLocalIdByRemoteId(remoteGame)!!
            val assistB = b.playerDao().getLocalIdByRemoteId(remoteAssist)!!
            assertNotEquals(game, gameB)
            assertNotEquals(assist, assistB)
            assertEquals(assistB, b.eventDao().getEvents(gameB).first { it.type == "TWO_MADE" }.assistedByPlayerId)
            assertEquals(GameSeason.CURRENT, b.gameDao().getById(gameB)!!.season)
            b.eventDao().undoAction(gameB)
            sync(b).synchronize()
            sync(a).synchronize()
            assertEquals(0, computeTeamScore(a.eventDao().getEvents(game).map { it.toDomain() }))
            assertFalse(a.eventDao().getEvents(game).any { it.type == "AST" })

            // A real HTTP commit succeeds, but its response is lost; the clock then changes.
            a.eventDao().recordAction(listOf(event("FT_MADE", shooter)))
            val pending = a.eventDao().getPendingEvents().single()
            val loseResponse = object : EventApi by events {
                override suspend fun uploadEvents(batch: List<EventUploadDto>): List<EventUploadResponse> {
                    events.uploadEvents(batch)
                    throw IOException("Acknowledgement lost after server commit")
                }
            }
            try { sync(a, loseResponse).synchronize(); fail("Expected lost response") } catch (_: SyncFailure) {}
            a.gameDao().updateLiveState(game, 1, 400, false, null)
            a.close()
            a = open()
            sync(a).synchronize()
            assertEquals(1, stats.snapshot().events.count { it.sync_key == pending.syncKey })
            assertEquals(400, stats.snapshot().games.first { it.id == remoteGame }.clock_sec_remaining)

            // A failed request leaves a durable queue, which recovers after reopening Room.
            a.eventDao().recordAction(listOf(event("FT_MADE", shooter)))
            val offline = object : EventApi by events {
                override suspend fun uploadEvents(events: List<EventUploadDto>): List<EventUploadResponse> = throw IOException("Offline")
            }
            try { sync(a, offline).synchronize(); fail("Expected offline failure") } catch (_: SyncFailure) {}
            assertEquals(1, a.eventDao().getPendingEvents().size)
            a.close()
            a = open()
            sync(a).synchronize()
            assertTrue(a.eventDao().getPendingEvents().isEmpty())
            assertEquals(2, a.gameDao().getById(game)!!.teamScore)
            assertEquals(2, stats.snapshot().games.first { it.id == remoteGame }.team_score)
            // A mutable update commits, its response is lost, and a newer clock edit is saved.
            a.gameDao().updateLiveState(game, 2, 300, false, null)
            val loseGameResponse = object : GameApi by games {
                override suspend fun updateGame(remoteId: String, body: GameUploadDto): GameUploadResponse {
                    games.updateGame(remoteId, body)
                    throw IOException("Game acknowledgement lost")
                }
            }
            try { sync(a, gameApi = loseGameResponse).synchronize(); fail("Expected lost game response") } catch (_: SyncFailure) {}
            a.gameDao().updateLiveState(game, 2, 250, false, null)
            a.close()
            a = open()
            sync(a).synchronize()
            assertEquals(250, stats.snapshot().games.first { it.id == remoteGame }.clock_sec_remaining)
            assertEquals("SYNCED", a.gameDao().getById(game)!!.syncStatus)
        } finally {
            a.close(); b.close(); context.deleteDatabase(name)
        }
    }
}
