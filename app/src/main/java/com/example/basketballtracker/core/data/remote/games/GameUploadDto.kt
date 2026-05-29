package com.example.basketballtracker.core.data.remote.games

import com.google.gson.annotations.SerializedName

data class GameUploadDto(
    val localId: Long?,
    val opponentName: String?,
    val isHomeGame: Boolean,
    val roundNumber: Int,
    val isPlayoff: Boolean,
    val playoffStage: String?,
    val playoffGameNumber: Int?,
    val gameDateEpoch: Long,
    val createdAt: Long,
    val quarterLengthSec: Int,
    val quartersCount: Int,
    val teamScore: Int,
    val opponentScore: Int,
    val status: String,
    val currentPeriod: Int,
    val clockSecRemaining: Int,
    val isClockRunning: Boolean,
    val lastClockStartedAt: Long?
)

data class GameRemoteDto(
    val id: String,

    @SerializedName("local_id")
    val local_id: Long?,

    @SerializedName("opponent_name")
    val opponent_name: String,

    @SerializedName("is_home_game")
    val is_home_game: Boolean,

    @SerializedName("round_number")
    val round_number: Int,

    @SerializedName("is_playoff")
    val is_playoff: Boolean,

    @SerializedName("playoff_stage")
    val playoff_stage: String?,

    @SerializedName("playoff_game_number")
    val playoff_game_number: Int?,

    @SerializedName("game_date_epoch")
    val game_date_epoch: Long,

    @SerializedName("created_at")
    val created_at: Long,

    @SerializedName("quarter_length_sec")
    val quarter_length_sec: Int,

    @SerializedName("quarters_count")
    val quarters_count: Int,

    @SerializedName("team_score")
    val team_score: Int,

    @SerializedName("opponent_score")
    val opponent_score: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("current_period")
    val current_period: Int,

    @SerializedName("clock_sec_remaining")
    val clock_sec_remaining: Int,

    @SerializedName("is_clock_running")
    val is_clock_running: Boolean,

    @SerializedName("last_clock_started_at")
    val last_clock_started_at: Long?
)