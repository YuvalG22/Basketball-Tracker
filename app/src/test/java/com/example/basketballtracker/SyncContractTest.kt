package com.example.basketballtracker

import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.events.EventRemoteDto
import com.example.basketballtracker.core.data.remote.games.GameUploadResponse
import com.example.basketballtracker.features.livegame.data.toDomain
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class SyncContractTest {
    @Test fun uploadCarriesStableIdentityRevisionSeasonAndExplicitBooleanFields() {
        val g = GameEntity(opponentName = "Test", roundNumber = 1, gameDateEpoch = 1, createdAt = 1,
            isHomeGame = true, season = "26/27", localRevision = 7, serverVersion = 3)
        val json = Gson().toJsonTree(g.toUploadDto()).asJsonObject
        assertEquals(g.syncKey, json["syncKey"].asString)
        assertEquals(g.mutationKey, json["mutationKey"].asString)
        assertEquals(7L, json["clientRevision"].asLong)
        assertEquals(3L, json["expectedServerVersion"].asLong)
        assertEquals("26/27", json["season"].asString)
        assertTrue(json["isHomeGame"].asBoolean)
    }
    @Test fun sameCloudEntityOnDifferentInstallationsGetsDifferentMutationTokens() {
        val a = GameEntity(opponentName = "Test", roundNumber = 1, gameDateEpoch = 1, createdAt = 1, syncKey = "shared")
        val b = GameEntity(opponentName = "Test", roundNumber = 1, gameDateEpoch = 1, createdAt = 1, syncKey = "shared")
        assertEquals(a.localRevision, b.localRevision)
        assertNotEquals(a.mutationKey, b.mutationKey)
    }
    @Test fun downloadRetainsServerAssistIdAndActionGroup() {
        val e = Gson().fromJson("""{"id":"e","type":"TWO_MADE","period":1,"clock_sec_remaining":100,
            "created_at":1,"assisted_by_player_remote_id":"assist","action_group_id":"group",
            "sync_key":"key","is_deleted":true}""", EventRemoteDto::class.java)
        assertEquals("assist", e.assisted_by_player_remote_id)
        assertEquals("group", e.action_group_id)
        assertTrue(e.is_deleted)
    }
    @Test fun serverAcknowledgementIncludesAppliedRevisionAndTombstone() {
        val r = Gson().fromJson("""{"remoteId":"r","serverVersion":5,"clientRevision":2,"isDeleted":true}""",
            GameUploadResponse::class.java)
        assertEquals(5L, r.serverVersion)
        assertEquals(2L, r.clientRevision)
        assertTrue(r.isDeleted)
    }
    @Test fun eventTieBreakerIsStableAcrossDifferentLocalIds() {
        val e = EventEntity(gameId = 1, playerId = null, type = "FT_MADE", period = 1,
            clockSecRemaining = 100, createdAt = 1, remoteId = "remote")
        assertEquals(e.copy(id = 1).toDomain().orderKey, e.copy(id = 999).toDomain().orderKey)
    }
}
