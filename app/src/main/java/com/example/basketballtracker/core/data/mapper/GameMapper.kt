package com.example.basketballtracker.core.data.mapper

import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.remote.games.GameUploadDto

fun GameEntity.toUploadDto(): GameUploadDto {
    return GameUploadDto(
        localId = id,
        opponentName = opponentName,
        isHomeGame = isHomeGame,
        roundNumber = roundNumber,
        gameDateEpoch = gameDateEpoch,
        createdAt = createdAt,
        quarterLengthSec = quarterLengthSec,
        quartersCount = quartersCount,
        teamScore = teamScore,
        opponentScore = opponentScore
    )
}