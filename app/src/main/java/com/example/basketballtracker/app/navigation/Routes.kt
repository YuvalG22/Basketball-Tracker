package com.example.basketballtracker.app.navigation

object Routes {
    const val HOME = "home"
    const val NEW_GAME = "new_game"
    const val LIVE = "live/{gameId}"
    const val PLAYERS = "players"


    fun live(gameId: Long) = "live/$gameId"
}