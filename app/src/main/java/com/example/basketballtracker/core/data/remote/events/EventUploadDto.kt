package com.example.basketballtracker.core.data.remote.events

data class EventUploadDto(
    val localId: Long?,
    val gameId: Long,
    val playerId: Long?,
    val gameRemoteId: String,
    val playerRemoteId: String?,
    val type: String,
    val period: Int,
    val clockSecRemaining: Int,
    val createdAt: Long,
    val teamScoreAtEvent: Int?,
    val opponentScoreAtEvent: Int?,
    val shotX: Float?,
    val shotY: Float?,
    val shotDistance: Float?
)