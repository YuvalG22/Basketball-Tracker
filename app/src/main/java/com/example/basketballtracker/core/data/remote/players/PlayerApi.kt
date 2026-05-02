package com.example.basketballtracker.core.data.remote.players

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PlayerApi {

    @POST("players")
    suspend fun uploadPlayer(
        @Body player: PlayerUploadDto
    ): PlayerUploadResponse

    @GET("players")
    suspend fun getPlayers(): List<PlayerRemoteDto>
}

data class PlayerUploadResponse(
    val remoteId: String
)

data class PlayerRemoteDto(
    val id: String,

    @SerializedName("local_id")
    val local_id: Long?,

    val name: String,
    val number: Int,

    @SerializedName("created_at")
    val created_at: Long?
)