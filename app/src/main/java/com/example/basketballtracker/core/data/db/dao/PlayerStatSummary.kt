package com.example.basketballtracker.core.data.db.dao

data class PlayerStatSummary(
    val playerId: Long,
    val playerName: String,
    val playerNumber: Int,
    val gamesPlayed: Int,
    val minutesPlayed: Int,
    val twoMade: Int,
    val twoMiss: Int,
    val threeMade: Int,
    val threeMiss: Int,
    val ftMade: Int,
    val ftMiss: Int,
    val ast: Int,
    val rebDef: Int,
    val rebOff: Int,
    val stl: Int,
    val blk: Int,
    val tov: Int,
    val pf: Int
)
