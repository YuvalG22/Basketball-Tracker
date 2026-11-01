package com.example.basketballtracker

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.features.players.data.PlayersRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerEditPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun editingPreservesIdentityHistoryAndPendingChangesDuringCloudRefresh() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.playerDao()
            val original = PlayerEntity(name = "Alex", number = 7, remoteId = "remote-1",
                syncStatus = "SYNCED", photoUri = "content://photos/1")
            val id = dao.insert(original)
            db.rosterDao().insert(RosterEntity(gameId = 10, playerId = id))
            val event = EventEntity(gameId = 10, playerId = id, type = "TWO_MADE",
                period = 1, clockSecRemaining = 100, createdAt = 0)
            val eventId = db.eventDao().insert(event)

            PlayersRepository(dao).updatePlayer(id, "Alex Updated", 23, "content://photos/2")
            dao.upsertPlayersFromCloud(listOf(original))

            assertEquals(original.copy(id = id, name = "Alex Updated", number = 23,
                photoUri = "content://photos/2", syncStatus = "PENDING"), dao.getPlayerById(id))
            assertEquals(1, dao.countPlayers())
            assertEquals(listOf(id), db.rosterDao().observeRosterPlayerIds(10).first())
            assertEquals(listOf(event.copy(id = eventId)), db.eventDao().observeEvents(10).first())
            assertEquals(listOf(id), dao.getPendingPlayers().map { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun removingPhotoPersistsWithoutQueuingAnUnchangedCloudProfile() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.playerDao()
            val player = PlayerEntity(name = "Alex", number = 7, remoteId = "remote-1",
                syncStatus = "SYNCED", photoUri = "content://photos/1")
            val id = dao.insert(player)
            PlayersRepository(dao).updatePlayer(id, "Alex", 7, null)
            dao.upsertPlayersFromCloud(listOf(player.copy(photoUri = null)))
            assertEquals(player.copy(id = id, photoUri = null), dao.getPlayerById(id))
            assertEquals(emptyList<PlayerEntity>(), dao.getPendingPlayers())
        } finally {
            db.close()
        }
    }
}
