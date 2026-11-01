package com.example.basketballtracker

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.MIGRATION_19_20
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerPhotoPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migrationPreservesExistingPlayersAndAllowsOptionalPhotos() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .callback(object : SupportSQLiteOpenHelper.Callback(19) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE players (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "name TEXT NOT NULL, number INTEGER NOT NULL, remoteId TEXT, " +
                                "syncStatus TEXT NOT NULL)"
                        )
                        db.execSQL("INSERT INTO players VALUES (1, 'Alex', 7, 'remote-1', 'SYNCED')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            val db = helper.writableDatabase
            MIGRATION_19_20.migrate(db)
            db.query("SELECT name, number, remoteId, syncStatus, photoUri FROM players WHERE id = 1").use {
                assertTrue(it.moveToFirst())
                assertEquals("Alex", it.getString(0))
                assertEquals(7, it.getInt(1))
                assertEquals("remote-1", it.getString(2))
                assertEquals("SYNCED", it.getString(3))
                assertTrue(it.isNull(4))
            }
            db.execSQL("UPDATE players SET photoUri = 'content://photos/1' WHERE id = 1")
            db.query("SELECT photoUri FROM players WHERE id = 1").use {
                assertTrue(it.moveToFirst())
                assertEquals("content://photos/1", it.getString(0))
            }
        } finally {
            helper.close()
        }
    }

    @Test
    fun cloudRefreshAndPlayerEditsRetainLocalPhoto() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.playerDao()
            val id = dao.insert(PlayerEntity(
                name = "Alex", number = 7, remoteId = "remote-1", syncStatus = "SYNCED",
                photoUri = "content://photos/1"
            ))
            dao.upsertPlayersFromCloud(listOf(
                PlayerEntity(name = "Alex Updated", number = 8, remoteId = "remote-1", syncStatus = "SYNCED")
            ))
            assertEquals("content://photos/1", dao.getPlayerById(id)?.photoUri)
            assertEquals("Alex Updated", dao.getPlayerById(id)?.name)
            assertEquals(8, dao.getPlayerById(id)?.number)
            assertEquals(1, dao.countPlayers())
            dao.updateById(id, "Alex Edited", 9)
            assertEquals("content://photos/1", dao.getPlayerById(id)?.photoUri)

            val noPhotoId = dao.insert(PlayerEntity(name = "Sam", number = 10))
            assertNull(dao.getPlayerById(noPhotoId)?.photoUri)
        } finally {
            db.close()
        }
    }
}
