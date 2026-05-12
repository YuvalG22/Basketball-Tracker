package com.example.basketballtracker.core.data.remote.stats

import retrofit2.http.GET
import retrofit2.http.POST

interface StatsApi {

    @POST("stats/season/refresh")
    suspend fun refreshSeasonStats()

    @GET("stats/season")
    suspend fun getSeasonStats(): List<PlayerSeasonStatsRemoteDto>
}