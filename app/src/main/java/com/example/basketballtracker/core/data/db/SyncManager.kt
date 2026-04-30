package com.example.basketballtracker.core.data.db

import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.dao.RosterDao
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.events.EventApi
import com.example.basketballtracker.core.data.remote.games.GameApi
import com.example.basketballtracker.core.data.remote.players.PlayerApi
import com.example.basketballtracker.core.data.remote.roster.RosterApi

class SyncManager(
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val rosterDao: RosterDao,
    private val eventDao: EventDao,

    private val gameApi: GameApi,
    private val playerApi: PlayerApi,
    private val rosterApi: RosterApi,
    private val eventApi: EventApi
) {

    suspend fun syncPending() {
        syncPendingGames()
        syncPendingPlayers()
        syncPendingRoster()
        syncPendingEvents()
    }

    private suspend fun syncPendingGames() {
        val pendingGames = gameDao.getPendingGames()

        pendingGames.forEach { game ->
            try {
                val response = gameApi.uploadGame(game.toUploadDto())
                gameDao.markSynced(game.id, response.remoteId)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun syncPendingPlayers() {
        val pendingPlayers = playerDao.getPendingPlayers()

        pendingPlayers.forEach { player ->
            try {
                val response = playerApi.uploadPlayer(player.toUploadDto())
                playerDao.markSynced(player.id, response.remoteId)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun syncPendingRoster() {
        val pendingRoster = rosterDao.getPendingRoster()

        pendingRoster.forEach { roster ->
            try {
                val response = rosterApi.uploadRoster(roster.toUploadDto())
                rosterDao.markSynced(
                    gameId = roster.gameId,
                    playerId = roster.playerId,
                    remoteId = response.remoteId
                )
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun syncPendingEvents() {
        val pendingEvents = eventDao.getPendingEvents()

        pendingEvents.forEach { event ->
            try {
                val response = eventApi.uploadEvent(event.toUploadDto())
                eventDao.markSynced(event.id, response.remoteId)
            } catch (_: Exception) {
            }
        }
    }
}