package com.example.basketballtracker.core.data.remote.roster

import retrofit2.http.Body
import retrofit2.http.POST

interface RosterApi {
    @POST("roster")
    suspend fun uploadRoster(
        @Body roster: RosterUploadDto
    ): RosterUploadResponse
}

data class RosterUploadResponse(
    val remoteId: String
)