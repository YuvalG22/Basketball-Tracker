package com.example.basketballtracker.core.data.remote.events

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EventApi {

    @POST("events")
    suspend fun uploadEvent(
        @Body event: EventUploadDto
    ): EventUploadResponse

    @GET("events")
    suspend fun getEvents(): List<EventRemoteDto>
}

data class EventUploadResponse(
    val remoteId: String
)

data class EventRemoteDto(
    val id: String,

    @SerializedName("local_id")
    val local_id: Long?,

    @SerializedName("local_game_id")
    val local_game_id: Long?,

    @SerializedName("local_player_id")
    val local_player_id: Long?,

    @SerializedName("game_remote_id")
    val game_remote_id: String?,

    @SerializedName("player_remote_id")
    val player_remote_id: String?,

    val type: String,
    val period: Int,

    @SerializedName("clock_sec_remaining")
    val clock_sec_remaining: Int,

    @SerializedName("created_at")
    val created_at: Long,

    @SerializedName("team_score_at_event")
    val team_score_at_event: Int?,

    @SerializedName("opponent_score_at_event")
    val opponent_score_at_event: Int?,

    @SerializedName("shot_x")
    val shot_x: Float?,

    @SerializedName("shot_y")
    val shot_y: Float?,

    @SerializedName("shot_distance")
    val shot_distance: Float?
)