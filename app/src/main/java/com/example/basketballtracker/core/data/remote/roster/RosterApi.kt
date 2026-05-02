package com.example.basketballtracker.core.data.remote.roster

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RosterApi {
    @POST("roster")
    suspend fun uploadRoster(
        @Body roster: RosterUploadDto
    ): RosterUploadResponse

    @GET("roster")
    suspend fun getRoster(): List<RosterRemoteDto>
}

data class RosterUploadResponse(
    val remoteId: String
)

data class RosterRemoteDto(
    val id: String,

    @SerializedName("local_game_id")
    val local_game_id: Long,

    @SerializedName("local_player_id")
    val local_player_id: Long,

    @SerializedName("game_remote_id")
    val game_remote_id: String?,

    @SerializedName("player_remote_id")
    val player_remote_id: String?,

    @SerializedName("created_at")
    val created_at: Long?
)