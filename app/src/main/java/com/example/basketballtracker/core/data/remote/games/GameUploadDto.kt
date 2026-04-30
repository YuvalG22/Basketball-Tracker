package com.example.basketballtracker.core.data.remote.games

data class GameUploadDto(
    val localId: Long?,
    val opponentName: String,
    val isHomeGame: Boolean,
    val roundNumber: Int,
    val gameDateEpoch: Long,
    val createdAt: Long,
    val quarterLengthSec: Int,
    val quartersCount: Int,
    val teamScore: Int,
    val opponentScore: Int
)