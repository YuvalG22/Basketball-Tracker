package com.example.basketballtracker.core.data.remote.games

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface GameApi {

    @POST("games")
    suspend fun uploadGame(
        @Body game: GameUploadDto
    ): GameUploadResponse

    @PATCH("games/score")
    suspend fun updateGameScore(@Body body: GameScoreUpdateDto)

    @GET("games")
    suspend fun getGames(): List<GameRemoteDto>
}

data class GameUploadResponse(
    val remoteId: String
)

data class GameScoreUpdateDto(
    val remoteId: String,
    val teamScore: Int,
    val opponentScore: Int
)