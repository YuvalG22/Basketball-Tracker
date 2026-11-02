package com.example.basketballtracker.core.data.remote.stats

import retrofit2.http.GET
import retrofit2.http.POST

data class SyncCapabilities(val protocolVersion: Int)

data class SyncSnapshot(
    val players: List<com.example.basketballtracker.core.data.remote.players.PlayerRemoteDto>,
    val games: List<com.example.basketballtracker.core.data.remote.games.GameRemoteDto>,
    val roster: List<com.example.basketballtracker.core.data.remote.roster.RosterRemoteDto>,
    val events: List<com.example.basketballtracker.core.data.remote.events.EventRemoteDto>
)
data class MutationQuery(val table: String, val remoteId: String, val mutationKey: String)
data class MutationAcknowledgement(val table: String, val remoteId: String, val mutationKey: String,
    val serverVersion: Long, val clientRevision: Long)

interface StatsApi {
    @POST("sync/mutation-acknowledgements")
    suspend fun mutationAcknowledgements(@retrofit2.http.Body queries: List<MutationQuery>): List<MutationAcknowledgement>

    @GET("sync/snapshot")
    suspend fun snapshot(): SyncSnapshot

    @GET("sync/capabilities")
    suspend fun capabilities(): SyncCapabilities


    @POST("stats/season/refresh")
    suspend fun refreshSeasonStats()

    @GET("stats/season")
    suspend fun getSeasonStats(): List<PlayerSeasonStatsRemoteDto>
}