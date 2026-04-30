package com.example.basketballtracker.core.data.remote.events

import retrofit2.http.Body
import retrofit2.http.POST

interface EventApi {

    @POST("events")
    suspend fun uploadEvent(
        @Body event: EventUploadDto
    ): EventUploadResponse
}

data class EventUploadResponse(
    val remoteId: String
)