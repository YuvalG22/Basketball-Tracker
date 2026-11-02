package com.example.basketballtracker.core.data.remote.players

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.GET
import retrofit2.http.POST

interface PlayerApi {
    @retrofit2.http.PUT("players/{remoteId}")
    suspend fun updatePlayer(@retrofit2.http.Path("remoteId") id: String, @Body body: PlayerUploadDto): PlayerUploadResponse
    @retrofit2.http.DELETE("players/{remoteId}")
    suspend fun deletePlayer(@retrofit2.http.Path("remoteId") id: String)


    @POST("players")
    suspend fun uploadPlayer(
        @Body player: PlayerUploadDto
    ): PlayerUploadResponse

    @GET("players")
    suspend fun getPlayers(@Query("includeDeleted") includeDeleted: Boolean = true): List<PlayerRemoteDto>
}

data class PlayerUploadResponse(
    val remoteId: String,
    val serverVersion: Long = 0,
    val clientRevision: Long = 0,
    val isDeleted: Boolean = false
)

data class PlayerRemoteDto(
    val id: String,

    @SerializedName("local_id")
    val local_id: Long?,

    val name: String,
    val number: Int,

    @SerializedName("created_at")
    val created_at: Long?,
    val sync_key: String? = null,
    val server_version: Long = 0,
    val is_deleted: Boolean = false,)