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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy

class SyncReliabilityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private inline fun <reified T> fake(crossinline handle: (String, Array<out Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            handle(method.name, args ?: emptyArray())
        } as T

    private fun manager(db: AppDatabase, events: EventApi = fake { _, _ -> error("Unexpected event call") },
        games: GameApi = fake { name, args ->
            check(name == "updateGame")
            val body = args[1] as GameUploadDto
            GameUploadResponse(args[0] as String, body.expectedServerVersion!! + 1, body.clientRevision)
        },
        stats: StatsApi = fake { name, _ ->
            when(name) {
                "capabilities" -> SyncCapabilities(2)
                "snapshot" -> SyncSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
                else -> error(name)
            }
        }) = SyncManager(db.gameDao(), db.playerDao(), db.rosterDao(), db.eventDao(), db.playerSeasonStatsDao(),
            games, fake<PlayerApi> { _, _ -> error("Unexpected player call") },
            fake<RosterApi> { _, _ -> error("Unexpected roster call") }, events, stats, db)

    private suspend fun seed(db: AppDatabase, status: String = "SYNCED"): Long = db.gameDao().insert(
        GameEntity(opponentName = "Test", roundNumber = 1, gameDateEpoch = 1, createdAt = 1,
            remoteId = "game", syncStatus = status, status = GameStatus.LIVE))

    @Test fun missingRemoteGameDoesNotHidePendingChildActions() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = seed(db)
            val child = db.eventDao().insert(EventEntity(gameId = id, playerId = null, type = "SUB_IN",
                period = 1, clockSecRemaining = 600, createdAt = 1))
            db.gameDao().markRemoteMissing(listOf("game"))
            assertFalse(db.gameDao().getById(id)!!.isDeleted)
            try {
                db.gameDao().upsertFromCloud(listOf(db.gameDao().getById(id)!!.copy(isDeleted = true)))
                fail("Remote deletion must not hide pending local actions")
            } catch (_: IllegalStateException) {}
            assertFalse(db.gameDao().getById(id)!!.isDeleted)
            db.eventDao().markDeleteSynced(child)
            db.gameDao().markRemoteMissing(listOf("game"))
            assertTrue(db.gameDao().getById(id)!!.isDeleted)
            assertNotNull(db.eventDao().getById(child))
            try {
                db.eventDao().recordAction(listOf(EventEntity(gameId = id, playerId = null, type = "FT_MADE",
                    period = 1, clockSecRemaining = 100, createdAt = 2)))
                fail("A remotely deleted game must not accept a late action")
            } catch (_: IllegalStateException) {}
        } finally { db.close() }
    }
    @Test fun pendingDraftSurvivesReopenAndConfirmationClearsItAtomically() = runBlocking {
        val name = "sync-draft-" + java.util.UUID.randomUUID() + ".db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val id = seed(db)
            db.eventDao().saveDraft(id, "saved pending shot")
            val key = db.gameDao().getById(id)!!.syncKey
            db.close()
            db = open()
            assertEquals("saved pending shot", db.eventDao().getDraft(id))
            assertEquals(key, db.gameDao().getById(id)!!.syncKey)
            db.eventDao().recordAction(listOf(EventEntity(gameId = id, playerId = null, type = "TWO_MADE",
                period = 1, clockSecRemaining = 500, createdAt = 1)))
            db.close()
            db = open()
            assertNull(db.eventDao().getDraft(id))
            assertEquals(1, db.eventDao().getPendingEvents().size)
            assertEquals(2, db.gameDao().getById(id)!!.teamScore)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun lostAcknowledgementRetriesSameEventKeysAndUndoUsesThoseKeys() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = seed(db)
            db.eventDao().recordAction(listOf("TWO_MADE", "AST").map {
                EventEntity(gameId = id, playerId = null, type = it, period = 1, clockSecRemaining = 500, createdAt = 1)
            })
            val remote = linkedMapOf<String, EventUploadResponse>()
            var loseResponse = true
            val api = fake<EventApi> { name, args -> when(name) {
                "acknowledgements" -> emptyList<EventAcknowledgement>()
                "uploadEvents" -> {
                    @Suppress("UNCHECKED_CAST") val batch = args[0] as List<EventUploadDto>
                    val responses = batch.map { remote.getOrPut(it.syncKey!!) { EventUploadResponse("remote-${remote.size}") } }
                    if (loseResponse) { loseResponse = false; throw IOException("Response lost after commit") }
                    responses
                }
                "deleteEvents" -> {
                    @Suppress("UNCHECKED_CAST") val deletes = args[0] as List<EventDeleteDto>
                    assertEquals(remote.keys, deletes.map { it.syncKey }.toSet())
                    EventDeleteResponse(true)
                }
                else -> error(name)
            } }
            val sync = manager(db, api)
            try { sync.syncPending(); fail("Expected retryable failure") } catch (_: SyncFailure) {}
            assertEquals(2, db.eventDao().getPendingEvents().size)
            sync.syncPending()
            assertEquals(2, remote.size)
            assertTrue(db.eventDao().getPendingEvents().isEmpty())
            db.eventDao().undoAction(id)
            sync.syncPending()
            assertTrue(db.eventDao().getEvents(id).isEmpty())
            assertTrue(db.eventDao().getPendingDeleteEvents().isEmpty())
        } finally { db.close() }
    }

    @Test fun acknowledgementCannotClearClockEditMadeDuringRequest() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = seed(db, "PENDING")
            var requests = 0
            val api = fake<GameApi> { name, args ->
                assertEquals("updateGame", name)
                val body = args[1] as GameUploadDto
                requests++
                if (requests == 1) runBlocking { db.gameDao().updateLiveState(id, 2, 333, false, null) }
                GameUploadResponse("game", requests.toLong(), body.clientRevision)
            }
            val sync = manager(db, games = api)
            try { sync.syncPending(); fail("New local edit must remain pending") } catch (_: IllegalStateException) {}
            val pending = db.gameDao().getById(id)!!
            assertEquals("PENDING", pending.syncStatus)
            assertEquals(333, pending.clockSecRemaining)
            assertEquals(1L, pending.localRevision)
            sync.syncPending()
            assertEquals("SYNCED", db.gameDao().getById(id)!!.syncStatus)
            assertEquals(2L, db.gameDao().getById(id)!!.serverVersion)
        } finally { db.close() }
    }

    @Test fun undoWhileUploadInFlightIsNotLost() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = seed(db)
            db.eventDao().recordAction(listOf(EventEntity(gameId = id, playerId = null, type = "FT_MADE",
                period = 1, clockSecRemaining = 500, createdAt = 1)))
            var deleted = false
            val api = fake<EventApi> { name, args -> when(name) {
                "acknowledgements" -> emptyList<EventAcknowledgement>()
                "uploadEvents" -> {
                    runBlocking { db.eventDao().undoAction(id) }
                    listOf(EventUploadResponse("event"))
                }
                "deleteEvents" -> { deleted = true; EventDeleteResponse(true) }
                else -> error(name)
            } }
            val sync = manager(db, api)
            try { sync.syncPending() } catch (_: SyncPendingChanges) { sync.syncPending() }
            assertTrue(deleted)
            assertTrue(db.eventDao().getEvents(id).isEmpty())
            assertTrue(db.eventDao().getPendingDeleteEvents().isEmpty())
        } finally { db.close() }
    }

    @Test fun localWritesAreAtomicOrderedAndPendingRowsSurviveCloudRefresh() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = seed(db)
            val game = db.gameDao().getById(id)!!
            db.gameDao().updateLiveState(id, 3, 123, false, null)
            db.gameDao().upsertFromCloud(listOf(game.copy(clockSecRemaining = 600)))
            assertEquals(123, db.gameDao().getById(id)!!.clockSecRemaining)
            db.eventDao().recordAction(listOf("SUB_OUT", "SUB_IN").map {
                EventEntity(gameId = id, playerId = 1, type = it, period = 1, clockSecRemaining = 500, createdAt = 10)
            })
            val events = db.eventDao().getEvents(id)
            assertTrue(events[0].createdAt < events[1].createdAt)
            assertEquals(events[0].actionGroupId, events[1].actionGroupId)
            db.eventDao().undoAction(id)
            assertEquals(2, db.eventDao().getPendingDeleteEvents().size)
        } finally { db.close() }
    }
}
