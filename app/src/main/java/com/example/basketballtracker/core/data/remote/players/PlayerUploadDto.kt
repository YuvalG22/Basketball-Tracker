package com.example.basketballtracker.core.data.remote.players

data class PlayerUploadDto(
    val localId: Long?,
    val name: String,
    val number: Int,
    val createdAt: Long,
    val syncKey: String? = null,
    val clientRevision: Long = 0,
    val expectedServerVersion: Long? = null,
    val mutationKey: String? = null,
)