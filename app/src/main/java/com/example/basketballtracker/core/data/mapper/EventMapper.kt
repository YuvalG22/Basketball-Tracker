package com.example.basketballtracker.core.data.mapper

import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.remote.events.EventUploadDto

fun EventEntity.toUploadDto(): EventUploadDto {
    return EventUploadDto(
        localId = id,
        gameId = gameId,
        playerId = playerId,
        type = type,
        period = period,
        clockSecRemaining = clockSecRemaining,
        createdAt = createdAt,
        teamScoreAtEvent = teamScoreAtEvent,
        opponentScoreAtEvent = opponentScoreAtEvent,
        shotX = shotX,
        shotY = shotY,
        shotDistance = shotDistance
    )
}