package com.example.basketballtracker.core.data.remote.players

data class PlayerUploadDto(
    val localId: Long?,
    val name: String,
    val number: Int,
    val createdAt: Long
)