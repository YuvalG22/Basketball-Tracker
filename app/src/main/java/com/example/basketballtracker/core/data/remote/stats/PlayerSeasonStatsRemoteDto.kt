package com.example.basketballtracker.core.data.remote.stats

data class PlayerSeasonStatsRemoteDto(
    val player_id: String,
    val player_name: String,
    val player_number: Int,
    val gp: Int,
    val pts: Int,
    val ast: Int,
    val reb_total: Int,
    val reb_def: Int,
    val reb_off: Int,
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