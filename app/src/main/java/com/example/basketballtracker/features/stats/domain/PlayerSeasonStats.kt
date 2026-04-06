package com.example.basketballtracker.features.stats.domain

data class PlayerSeasonStats(
    val playerId: Long,
    val playerName: String,
    val playerNumber: Int,

    val gp: Int,

    val pts: Int,
    val ast: Int,
    val rebTotal: Int,
    val rebDef: Int,
    val rebOff: Int,
    val stl: Int,
    val blk: Int,
    val tov: Int,
    val pf: Int,

    val fgm: Int,
    val fga: Int,
    val threem: Int,
    val threea: Int,
    val ftm: Int,
    val fta: Int
)