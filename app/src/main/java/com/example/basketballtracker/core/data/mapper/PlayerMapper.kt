package com.example.basketballtracker.core.data.mapper

import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.remote.players.PlayerUploadDto

fun PlayerEntity.toUploadDto(): PlayerUploadDto {
    return PlayerUploadDto(
        localId = id,
        name = name,
        number = number,
        createdAt = System.currentTimeMillis()
    )
}