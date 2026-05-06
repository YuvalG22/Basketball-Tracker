package com.example.basketballtracker.core.data.db

import android.util.Log
import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.dao.RosterDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.RetrofitClient
import com.example.basketballtracker.core.data.remote.events.EventApi
import com.example.basketballtracker.core.data.remote.events.EventUploadDto
import com.example.basketballtracker.core.data.remote.games.GameApi
import com.example.basketballtracker.core.data.remote.players.PlayerApi
import com.example.basketballtracker.core.data.remote.roster.RosterApi
import com.example.basketballtracker.core.data.remote.roster.RosterUploadDto

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

    suspend fun fetchAllFromCloud() {
        fetchPlayersFromCloud()
        fetchGamesFromCloud()
        fetchRosterFromCloud()
        fetchEventsFromCloud()
    }

    private suspend fun syncPendingGames() {
        val pendingGames = gameDao.getPendingGames()

        pendingGames.forEach { game ->
            try {
                val response = gameApi.uploadGame(game.toUploadDto())
                gameDao.markSynced(game.id, response.remoteId)
            } catch (e: Exception) {
                Log.e("SYNC", "Fetch failed", e)
            }
        }
    }

    private suspend fun syncPendingPlayers() {
        val pendingPlayers = playerDao.getPendingPlayers()

        pendingPlayers.forEach { player ->
            try {
                val response = playerApi.uploadPlayer(player.toUploadDto())
                playerDao.markSynced(player.id, response.remoteId)
            } catch (e: Exception) {
                Log.e("SYNC", "Fetch failed", e)
            }
        }
    }

    private suspend fun syncPendingRoster() {
        val pendingRoster = rosterDao.getPendingRoster()

        pendingRoster.forEach { roster ->
            try {
                val game = gameDao.getById(roster.gameId)
                val gameRemoteId = game?.remoteId ?: return@forEach

                val player = playerDao.getPlayerById(roster.playerId)
                val playerRemoteId = player?.remoteId ?: return@forEach
                Log.d("ROSTER", "dffdfds")
                val response = rosterApi.uploadRoster(
                    RosterUploadDto(
                        gameId = roster.gameId,
                        playerId = roster.playerId,
                        gameRemoteId = gameRemoteId,
                        playerRemoteId = playerRemoteId,
                    )
                )
                rosterDao.markSynced(
                    gameId = roster.gameId,
                    playerId = roster.playerId,
                    remoteId = response.remoteId
                )
            } catch (e: Exception) {
                Log.e("SYNC", "Fetch failed", e)
            }
        }
    }

    private suspend fun syncPendingEvents() {
        val pendingEvents = eventDao.getPendingEvents()

        pendingEvents.forEach { event ->
            try {
                val game = gameDao.getById(event.gameId)
                val gameRemoteId = game?.remoteId ?: return@forEach

                val playerRemoteId = event.playerId?.let { playerId ->
                    playerDao.getPlayerById(playerId)?.remoteId
                }

                val response = eventApi.uploadEvent(
                    EventUploadDto(
                        localId = event.id,
                        gameId = event.gameId,
                        playerId = event.playerId,
                        gameRemoteId = gameRemoteId,
                        playerRemoteId = playerRemoteId,
                        type = event.type,
                        period = event.period,
                        clockSecRemaining = event.clockSecRemaining,
                        createdAt = event.createdAt,
                        teamScoreAtEvent = event.teamScoreAtEvent,
                        opponentScoreAtEvent = event.opponentScoreAtEvent,
                        shotX = event.shotX,
                        shotY = event.shotY,
                        shotDistance = event.shotDistance
                    )
                )
                eventDao.markSynced(event.id, response.remoteId)
            } catch (e: Exception) {
                Log.e("SYNC", "Fetch failed", e)
            }
        }
    }

    private suspend fun fetchGamesFromCloud() {
        try {
            val remoteGames = RetrofitClient.gameApi.getGames()

            remoteGames.forEach { dto ->

                val existingLocalId = gameDao.getLocalIdByRemoteId(dto.id)

                val entity = GameEntity(
                    id = existingLocalId ?: 0,
                    opponentName = dto.opponent_name ?: "Unknown",
                    isHomeGame = dto.is_home_game,
                    roundNumber = dto.round_number,
                    gameDateEpoch = dto.game_date_epoch,
                    createdAt = dto.created_at,
                    quarterLengthSec = dto.quarter_length_sec,
                    quartersCount = dto.quarters_count,
                    teamScore = dto.team_score,
                    opponentScore = dto.opponent_score,
                    remoteId = dto.id,
                    syncStatus = "SYNCED"
                )

                if (existingLocalId == null) {
                    gameDao.insert(entity)
                } else {
                    gameDao.update(entity)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchPlayersFromCloud() {
        try {
            val remotePlayers = playerApi.getPlayers()

            remotePlayers.forEach { dto ->

                val existingLocalId =
                    playerDao.getLocalIdByRemoteId(dto.id)

                val entity = PlayerEntity(
                    id = existingLocalId ?: 0,
                    name = dto.name,
                    number = dto.number,
                    remoteId = dto.id,
                    syncStatus = "SYNCED"
                )

                if (existingLocalId == null) {
                    playerDao.insert(entity)
                } else {
                    playerDao.update(entity)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchRosterFromCloud() {
        try {
            val remoteRoster = rosterApi.getRoster()

            remoteRoster.forEach { dto ->
                val gameRemoteId = dto.game_remote_id ?: return@forEach
                val playerRemoteId = dto.player_remote_id ?: return@forEach

                val localGameId = gameDao.getLocalIdByRemoteId(gameRemoteId)
                    ?: return@forEach

                val localPlayerId = playerDao.getLocalIdByRemoteId(playerRemoteId)
                    ?: return@forEach

                rosterDao.insert(
                    RosterEntity(
                        gameId = localGameId,
                        playerId = localPlayerId,
                        remoteId = dto.id,
                        syncStatus = "SYNCED"
                    )
                )
            }

        } catch (e: Exception) {
            Log.e("SYNC", "Roster fetch failed", e)
        }
    }

    private suspend fun fetchEventsFromCloud() {
        try {
            Log.d("SYNC", "Fetching events")
            val remoteEvents = eventApi.getEvents()

            remoteEvents.forEach { dto ->
                val gameRemoteId = dto.game_remote_id ?: return@forEach

                val localGameId = gameDao.getLocalIdByRemoteId(gameRemoteId)
                    ?: return@forEach

                val localPlayerId = dto.player_remote_id?.let {
                    playerDao.getLocalIdByRemoteId(it)
                }

                val existingLocalId = eventDao.getLocalIdByRemoteId(dto.id)

                val entity = EventEntity(
                    id = existingLocalId ?: 0,
                    gameId = localGameId,
                    playerId = localPlayerId,
                    type = dto.type,
                    period = dto.period,
                    clockSecRemaining = dto.clock_sec_remaining,
                    createdAt = dto.created_at,
                    teamScoreAtEvent = dto.team_score_at_event,
                    opponentScoreAtEvent = dto.opponent_score_at_event,
                    shotX = dto.shot_x,
                    shotY = dto.shot_y,
                    shotDistance = dto.shot_distance,
                    remoteId = dto.id,
                    syncStatus = "SYNCED"
                )

                if (existingLocalId == null) {
                    eventDao.insert(entity)
                } else {
                    eventDao.update(entity)
                }
            }

        } catch (e: Exception) {
            Log.e("SYNC", "Events fetch failed", e)
        }
    }
}