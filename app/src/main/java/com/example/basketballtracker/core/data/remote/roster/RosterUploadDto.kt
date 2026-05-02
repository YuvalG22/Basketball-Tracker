package com.example.basketballtracker.core.data.remote.roster

data class RosterUploadDto(
    val gameId: Long,
    val playerId: Long,
    val gameRemoteId: String,
    val playerRemoteId: String,
    val createdAt: Long = System.currentTimeMillis()
)