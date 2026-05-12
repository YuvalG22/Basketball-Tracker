package com.example.basketballtracker.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_season_stats")
data class PlayerSeasonStatsEntity(
    @PrimaryKey
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
    val fta: Int,

    val updatedAt: Long = System.currentTimeMillis()
)