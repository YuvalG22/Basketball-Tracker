package com.example.basketballtracker

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.MIGRATION_21_22
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.livegame.ui.AssistPickerDialog
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LiveRecordingTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun event(type: String, group: String? = null) = EventEntity(gameId = 1, playerId = 1,
        type = type, period = 1, clockSecRemaining = 500, createdAt = 1, actionGroupId = group)

    @Test fun groupedUndoAndUploadCompletionKeepDeletedActionHidden() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.eventDao()
            val legacy = dao.insert(event("FT_MADE"))
            dao.insertAll(listOf(event("TWO_MADE", "shot"), event("AST", "shot")))
            dao.undoAction(1)
            assertEquals(listOf(legacy), dao.getEvents(1).map { it.id })
            val deleted = dao.getPendingDeleteEvents()
            assertEquals(2, deleted.size)
            deleted.forEach { dao.markSynced(it.id, "remote-${it.id}") }
            assertEquals(2, dao.getPendingDeleteEvents().size)
            dao.upsertEventsFromCloud(deleted.map { it.copy(remoteId = "remote-${it.id}", syncStatus = "SYNCED", actionGroupId = null) })
            assertEquals(listOf(legacy), dao.getEvents(1).map { it.id })
            dao.undoAction(1)
            assertTrue(dao.getEvents(1).isEmpty())
        } finally { db.close() }
    }

    @Test fun swapUndoRestoresBothSidesAndCloudUpdatePreservesGroup() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.eventDao()
            val id = dao.insert(event("SUB_IN", "initial").copy(remoteId = "one"))
            dao.upsertEventsFromCloud(listOf(event("SUB_IN").copy(remoteId = "one", syncStatus = "SYNCED")))
            assertEquals("initial", dao.getById(id)?.actionGroupId)
            dao.insertAll(listOf(event("SUB_OUT", "swap"), event("SUB_IN", "swap").copy(playerId = 2)))
            dao.undoAction(1)
            assertEquals(listOf(id), dao.getEvents(1).map { it.id })
        } finally { db.close() }
    }

    @Test fun migrationPreservesExistingEvent() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY, type TEXT)") }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }).build())
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO events VALUES (7, 'TWO_MADE')")
            MIGRATION_21_22.migrate(db)
            db.query("SELECT id, type, actionGroupId FROM events").use {
                assertTrue(it.moveToFirst()); assertEquals(7, it.getInt(0)); assertEquals("TWO_MADE", it.getString(1)); assertTrue(it.isNull(2))
            }
        } finally { helper.close() }
    }

    @Test fun assistHasExplicitCancelAndNoAssistChoices() {
        var cancelled = 0
        var noAssist = 0
        compose.setContent { BasketballTrackerTheme {
            AssistPickerDialog(listOf(PlayerEntity(id = 2, name = "Alex", number = 2)),
                { noAssist++ }, {}, { cancelled++ })
        } }
        compose.onNodeWithText("No assist").performClick()
        compose.onNodeWithText("Cancel basket").performClick()
        assertEquals(1, cancelled)
        assertEquals(1, noAssist)
    }
}
