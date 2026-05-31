package com.example.basketballtracker.core.data.db

import android.util.Log
import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.dao.PlayerSeasonStatsDao
import com.example.basketballtracker.core.data.db.dao.RosterDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.db.entities.PlayerSeasonStatsEntity
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.RetrofitClient
import com.example.basketballtracker.core.data.remote.events.EventApi
import com.example.basketballtracker.core.data.remote.events.EventUploadDto
import com.example.basketballtracker.core.data.remote.games.GameApi
import com.example.basketballtracker.core.data.remote.players.PlayerApi
import com.example.basketballtracker.core.data.remote.roster.RosterApi
import com.example.basketballtracker.core.data.remote.roster.RosterUploadDto
import com.example.basketballtracker.core.data.remote.stats.StatsApi

class SyncManager(
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val rosterDao: RosterDao,
    private val eventDao: EventDao,
    private val playerSeasonStatsDao: PlayerSeasonStatsDao,

    private val gameApi: GameApi,
    private val playerApi: PlayerApi,
    private val rosterApi: RosterApi,
    private val eventApi: EventApi,
    private val statsApi: StatsApi
) {

    suspend fun syncPending() {
        syncPendingEventDeletes()
        syncPendingPlayers()
        syncPendingGames()
        syncPendingRoster()
        syncPendingEvents()
    }

    suspend fun fetchAllFromCloud() {
        fetchPlayersFromCloud()
        fetchGamesFromCloud()
        fetchRosterFromCloud()
        fetchEventsFromCloud()
        fetchSeasonStatsFromCloud()
    }

    private suspend fun syncPendingGames() {
        val gamesToDelete = gameDao.getPendingDeletion()

        gamesToDelete.forEach { game ->
            try {
                game.remoteId?.let { gameApi.deleteGameFromCloud(it) }
                gameDao.deleteById(game.id)
            } catch (e: Exception) {
                Log.e("SYNC", "Failed to delete remote game ${game.id}", e)
            }
        }

        val pendingGames = gameDao.getPendingGames()

        pendingGames.forEach { game ->
            try {
                val remoteId = game.remoteId
                Log.d("SYNC_GAME_STATUS", "local status=${game.status}")
                Log.d("SYNC_GAME_STATUS", "dto status=${game.toUploadDto().status}")

                if (remoteId == null) {
                    val response = gameApi.uploadGame(game.toUploadDto())
                    gameDao.markSynced(game.id, response.remoteId)
                } else {
                    gameApi.updateGame(
                        remoteId = remoteId,
                        body = game.toUploadDto()
                    )

                    gameDao.markSynced(game.id, remoteId)
                }
            } catch (e: Exception) {
                Log.e("SYNC", "Game sync failed ${game.id}", e)
            }
        }
    }

    private suspend fun syncPendingPlayers() {
        val pendingPlayers = playerDao.getPendingPlayers()
        Log.d("Players Upload", "Pending players: ${pendingPlayers.size}")

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
        if (pendingEvents.isEmpty()) return

        val gameIds = pendingEvents.map { it.gameId }.distinct()
        val gamesMap = gameIds.associateWith { gameDao.getById(it) }

        pendingEvents.forEach { event ->
            try {
                val game = gamesMap[event.gameId]
                val gameRemoteId = game?.remoteId

                if (gameRemoteId == null) {
                    Log.w("SYNC", "Skipping event ${event.id}: Game not yet synced to cloud")
                    return@forEach
                }
                val playerRemoteId = event.playerId?.let { playerDao.getPlayerById(it)?.remoteId }
                val assistedByPlayerRemoteId = event.assistedByPlayerId?.let { playerDao.getPlayerById(it)?.remoteId }

                val response = eventApi.uploadEvent(
                    EventUploadDto(
                        localId = event.id,
                        gameId = event.gameId,
                        playerId = event.playerId,
                        assistedByPlayerId = event.assistedByPlayerId,
                        gameRemoteId = gameRemoteId,
                        playerRemoteId = playerRemoteId,
                        assistedByPlayerRemoteId = assistedByPlayerRemoteId,
                        type = event.type,
                        period = event.period,
                        clockSecRemaining = event.clockSecRemaining,
                        createdAt = event.createdAt,
                        teamScoreAtEvent = event.teamScoreAtEvent,
                        opponentScoreAtEvent = event.opponentScoreAtEvent,
                        shotX = event.shotX,
                        shotY = event.shotY,
                        shotDistance = event.shotDistance,
                    )
                )
                eventDao.markSynced(event.id, response.remoteId)

            } catch (e: Exception) {
                Log.e("SYNC", "Failed to upload event ${event.id}", e)
            }
        }
    }

    private suspend fun syncPendingEventDeletes() {
        val pendingDeletes = eventDao.getPendingDeleteEvents()

        pendingDeletes.forEach { event ->
            try {
                if (event.remoteId != null) {
                    eventApi.deleteEvent(event.remoteId)
                }

                eventDao.deleteById(event.id)
                Log.d("SYNC", "Pending event deletes: ${pendingDeletes.size}")

            } catch (e: Exception) {
                Log.e("SYNC", "Failed to delete event ${event.id}", e)
            }
        }
    }

    private suspend fun fetchGamesFromCloud() {
        try {
            val remoteGames = RetrofitClient.gameApi.getGames()
            val remoteIdsFromCloud = remoteGames.map { it.id }

            // 1. עדכון/הכנסה של משחקים קיימים
            val entities = remoteGames.map { dto ->
                GameEntity(
                    opponentName = dto.opponent_name ?: "Unknown",
                    isHomeGame = dto.is_home_game,
                    roundNumber = dto.round_number,
                    isPlayoff = dto.is_playoff,
                    playoffStage = dto.playoff_stage,
                    playoffGameNumber = dto.playoff_game_number,
                    gameDateEpoch = dto.game_date_epoch,
                    createdAt = dto.created_at,
                    quarterLengthSec = dto.quarter_length_sec,
                    quartersCount = dto.quarters_count,
                    teamScore = dto.team_score,
                    opponentScore = dto.opponent_score,
                    status = dto.status,
                    currentPeriod = dto.current_period,
                    clockSecRemaining = dto.clock_sec_remaining,
                    isClockRunning = dto.is_clock_running,
                    lastClockStartedAt = dto.last_clock_started_at,
                    remoteId = dto.id,
                    syncStatus = "SYNCED",
                    isDeleted = false,
                )
            }
            gameDao.upsertFromCloud(entities)

            // 2. זיהוי ומחיקה של משחקים שאינם בענן יותר
            val localGamesWithRemoteId = gameDao.getAllWithRemoteIdNow() // שאילתה פשוטה שמביאה הכל
            localGamesWithRemoteId.forEach { localGame ->
                if (localGame.remoteId !in remoteIdsFromCloud) {
                    // המשחק נמחק מהענן על ידי מכשיר אחר -> מחק אותו גם פה
                    gameDao.deleteById(localGame.id)
                }
            }

            Log.d("Fetch games", "Sync and Reconciliation completed")
        } catch (e: Exception) {
            Log.e("SYNC games", "Game fetch failed", e)
        }
    }

    private suspend fun fetchPlayersFromCloud() {
        try {
            val remotePlayers = playerApi.getPlayers()
            val entities = remotePlayers.map { dto ->
                val existingId = dto.id?.let { playerDao.getLocalIdByRemoteId(it) }
                PlayerEntity(
                    id = existingId ?: 0,
                    name = dto.name,
                    number = dto.number,
                    remoteId = dto.id,
                    syncStatus = "SYNCED"
                )
            }
            playerDao.upsertPlayersFromCloud(entities)
        } catch (e: Exception) {
            Log.e("SYNC", "Player fetch failed", e)
        }
    }

    private suspend fun fetchRosterFromCloud() {
        try {
            Log.d("SYNC", "Starting Roster fetch")
            val remoteRoster = rosterApi.getRoster()

            val entities = remoteRoster.mapNotNull { dto ->
                val gameRemoteId = dto.game_remote_id ?: return@mapNotNull null
                val playerRemoteId = dto.player_remote_id ?: return@mapNotNull null

                val localGameId = gameDao.getLocalIdByRemoteId(gameRemoteId)
                val localPlayerId = playerDao.getLocalIdByRemoteId(playerRemoteId)

                if (localGameId == null || localPlayerId == null) {
                    Log.w("SYNC", "Skipping roster item: Game or Player not found locally")
                    return@mapNotNull null
                }

                RosterEntity(
                    gameId = localGameId,
                    playerId = localPlayerId,
                    remoteId = dto.id,
                    syncStatus = "SYNCED"
                )
            }

            if (entities.isNotEmpty()) {
                rosterDao.insertAll(entities)
                Log.d("SYNC", "Roster sync completed: ${entities.size} items")
            }

        } catch (e: Exception) {
            Log.e("SYNC", "Roster fetch failed", e)
        }
    }

    private suspend fun fetchEventsFromCloud() {
        try {
            val remoteEvents = eventApi.getEvents()

            val entities = remoteEvents.mapNotNull { dto ->
                val localGameId = gameDao.getLocalIdByRemoteId(dto.game_remote_id)
                if (localGameId == null) {
                    Log.w(
                        "SYNC",
                        "Skipping event ${dto.id}: Game ${dto.game_remote_id} not found locally"
                    )
                    return@mapNotNull null // מדלג רק על האירוע הבעייתי
                }

                val localPlayerId = dto.player_remote_id?.let { playerDao.getLocalIdByRemoteId(it) }

                EventEntity(
                    id = eventDao.getLocalIdByRemoteId(dto.id) ?: 0,
                    gameId = localGameId,
                    playerId = localPlayerId,
                    assistedByPlayerId = dto.assisted_by_player_id,
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
            }
            eventDao.upsertEventsFromCloud(entities)
        } catch (e: Exception) {
            Log.e("SYNC", "Events fetch failed", e)
        }
    }

    private suspend fun fetchSeasonStatsFromCloud() {
        try {
            Log.d("STATS_SYNC", "Refreshing season stats")
            statsApi.refreshSeasonStats()
            Log.d("STATS_SYNC", "Refresh success")

            Log.d("STATS_SYNC", "Fetching season stats")
            val remoteStats = statsApi.getSeasonStats()
            Log.d("STATS_SYNC", "Remote stats size: $remoteStats")

            val entities = remoteStats.mapNotNull { dto ->
                Log.d("STATS_SYNC", "DTO player_id=${dto.player_id}, name=${dto.player_name}")
                val localPlayerId = playerDao.getLocalIdByRemoteId(dto.player_id)
                Log.d("STATS_SYNC", "Mapped localPlayerId=$localPlayerId for remoteId=${dto.player_id}")

                if (localPlayerId == null) {
                    Log.w("STATS_SYNC", "Skipping stats. No local player for remoteId=${dto.player_id}")
                    return@mapNotNull null
                }

                PlayerSeasonStatsEntity(
                    playerId = localPlayerId,
                    playerName = dto.player_name,
                    playerNumber = dto.player_number,
                    gp = dto.gp,
                    pts = dto.pts,
                    ast = dto.ast,
                    rebTotal = dto.reb_total,
                    rebDef = dto.reb_def,
                    rebOff = dto.reb_off,
                    stl = dto.stl,
                    blk = dto.blk,
                    tov = dto.tov,
                    pf = dto.pf,
                    fgm = dto.fgm,
                    fga = dto.fga,
                    threem = dto.threem,
                    threea = dto.threea,
                    ftm = dto.ftm,
                    fta = dto.fta
                )
            }

            playerSeasonStatsDao.replaceAll(entities)

        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "Failed to fetch season stats", e)
        }
    }
}