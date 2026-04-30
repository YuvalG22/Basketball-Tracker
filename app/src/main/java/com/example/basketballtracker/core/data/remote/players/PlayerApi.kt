package com.example.basketballtracker.core.data.remote.players

import retrofit2.http.Body
import retrofit2.http.POST

interface PlayerApi {

    @POST("players")
    suspend fun uploadPlayer(
        @Body player: PlayerUploadDto
    ): PlayerUploadResponse
}

data class PlayerUploadResponse(
    val remoteId: String
)