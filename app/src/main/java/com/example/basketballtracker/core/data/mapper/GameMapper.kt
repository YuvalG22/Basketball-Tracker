package com.example.basketballtracker.core.data.mapper

import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.remote.games.GameRemoteDto
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

fun GameRemoteDto.toEntity(): GameEntity {
    return GameEntity(
        id = local_id ?: 0,
        opponentName = opponent_name,
        isHomeGame = is_home_game,
        roundNumber = round_number,
        gameDateEpoch = game_date_epoch,
        createdAt = created_at,
        quarterLengthSec = quarter_length_sec,
        quartersCount = quarters_count,
        teamScore = team_score,
        opponentScore = opponent_score,
        remoteId = id,
        syncStatus = "SYNCED"
    )
}