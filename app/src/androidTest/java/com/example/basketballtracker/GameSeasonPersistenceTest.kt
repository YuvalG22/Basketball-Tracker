package com.example.basketballtracker

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.MIGRATION_20_21
import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.core.data.remote.games.*
import com.example.basketballtracker.features.games.data.GamesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class GameSeasonPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(MIGRATION_20_21).build()
    private fun game(remote: String) = GameEntity(opponentName = "Simulation - Falcons", roundNumber = 1,
        gameDateEpoch = 0, createdAt = 0, remoteId = remote, teamScore = 102, opponentScore = 67)

    @Test fun version20MigrationPreservesGamesEventsAndRoster() = runBlocking {
        val name = "season-migration-${UUID.randomUUID()}.db"
        try {
            val db = open(name)
            val original = game("legacy").copy(season = GameSeason.CURRENT)
            val id = db.gameDao().insert(original)
            val player = db.playerDao().insert(PlayerEntity(name = "Test Player", number = 7))
            val event = EventEntity(gameId = id, playerId = player, type = "THREE_MADE",
                period = 2, clockSecRemaining = 300, createdAt = 10)
            val eventId = db.eventDao().insert(event)
            db.rosterDao().insert(RosterEntity(id, player))
            db.close()
            // Reconstruct the previous schema in this disposable database. Room
            // then runs the real 20 -> 21 upgrade and validates the entire schema.
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null,
                SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("ALTER TABLE games DROP COLUMN season")
                it.version = 20
            }
            val migrated = open(name)
            try {
                assertEquals(original.copy(id = id, season = GameSeason.LEGACY), migrated.gameDao().getById(id))
                assertEquals(listOf(event.copy(id = eventId)), migrated.eventDao().getEvents(id))
                assertEquals(listOf(player), migrated.rosterDao().observeRosterPlayerIds(id).first())
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun creationAndCloudRefreshRetainCurrentSeasonAfterReopening() = runBlocking {
        val name = "season-sync-${UUID.randomUUID()}.db"
        try {
            val db = open(name)
            val api = object : GameApi {
                override suspend fun uploadGame(game: GameUploadDto) = GameUploadResponse("new-game")
                override suspend fun updateGame(remoteId: String, body: GameUploadDto) = Unit
                override suspend fun updateGameScore(body: GameScoreUpdateDto) = Unit
                override suspend fun getGames() = emptyList<GameRemoteDto>()
                override suspend fun deleteGameFromCloud(remoteId: String) = Unit
            }
            val id = GamesRepository(db.gameDao(), api).createGame("Visitors", true, 1, 0, 600, false, null, null)
            assertEquals(GameSeason.CURRENT, db.gameDao().getById(id)!!.season)
            db.gameDao().upsertFromCloud(listOf(game("new-game"), game("downloaded")))
            db.close()
            val reopened = open(name)
            try {
                assertEquals(GameSeason.CURRENT, reopened.gameDao().getById(id)!!.season)
                assertEquals(102, reopened.gameDao().getById(id)!!.teamScore)
                val imported = reopened.gameDao().getLocalIdByRemoteId("downloaded")!!
                assertEquals(GameSeason.LEGACY, reopened.gameDao().getById(imported)!!.season)
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}
