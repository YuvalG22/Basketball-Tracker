package com.example.basketballtracker.core.data.remote.roster

data class RosterUploadDto(
    val gameId: Long,
    val playerId: Long,
    val createdAt: Long = System.currentTimeMillis()
)