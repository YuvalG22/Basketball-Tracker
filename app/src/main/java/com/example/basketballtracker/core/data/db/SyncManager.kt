package com.example.basketballtracker.core.data.db

import androidx.room.withTransaction
import com.example.basketballtracker.core.data.db.dao.*
import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.events.*
import com.example.basketballtracker.core.data.remote.games.GameApi
import com.example.basketballtracker.core.data.remote.players.PlayerApi
import com.example.basketballtracker.core.data.remote.roster.*
import com.example.basketballtracker.core.data.remote.stats.StatsApi
import com.example.basketballtracker.core.data.remote.stats.MutationQuery
import com.example.basketballtracker.core.data.remote.stats.requireSupportedSyncProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val statsApi: StatsApi,
    private val database: AppDatabase? = null
) {
    companion object { private val syncLock = Mutex() }
    private var protocolVerified = false

    private suspend fun checkProtocol() {
        if (!protocolVerified) {
            requireSupportedSyncProtocol { statsApi.capabilities() }
            protocolVerified = true
        }
    }

    // Frequent live-game triggers coalesce instead of accumulating queued requests.
    suspend fun syncPending() {
        if (!syncLock.tryLock()) return
        try { checkProtocol(); upload() } finally { syncLock.unlock() }
    }

    suspend fun synchronize() = syncLock.withLock {
        checkProtocol()
        upload()
        download()
    }

    suspend fun fetchAllFromCloud() = synchronize()

    private suspend fun upload() {
        val errors = mutableListOf<Exception>()
        suspend fun attempt(block: suspend () -> Unit): Boolean {
            return try { block(); true } catch (e: CancellationException) { throw e }
            catch (e: Exception) { errors += e; false }
        }

        // Resolve ambiguous event commits before updating their parent clock. Use
        // the version at that action's commit, never blindly rebase onto another writer.
        val uncertain = eventDao.getPendingEvents().filter { it.syncStatus == "UPLOADING" } +
            eventDao.getPendingDeleteEvents()
        for (chunk in uncertain.chunked(100)) {
            val acknowledgements = eventApi.acknowledgements(chunk.map { it.syncKey })
            val apply: suspend () -> Unit = {
                acknowledgements.forEach { ack ->
                    val local = eventDao.getBySyncKey(ack.syncKey) ?: error("Unknown acknowledgement")
                    eventDao.markSynced(local.id, ack.remoteId)
                    if (ack.isDeleted) eventDao.markDeleteSynced(local.id)
                    ack.gameServerVersion?.let { gameDao.acknowledgeEventVersion(local.gameId, it) }
                }
            }
            if (database != null) database.withTransaction { apply() } else apply()
        }

        val gameAttempts = (gameDao.getPendingGames() + gameDao.getPendingDeletion()).filter { it.remoteId != null && it.lastAttemptKey != null }
        val playerAttempts = (playerDao.getPendingPlayers() + playerDao.getPendingDeletes()).filter { it.remoteId != null && it.lastAttemptKey != null }
        val queries = gameAttempts.map { MutationQuery("games", it.remoteId!!, it.lastAttemptKey!!) } +
            playerAttempts.map { MutationQuery("players", it.remoteId!!, it.lastAttemptKey!!) }
        for (chunk in queries.chunked(100)) {
            for (ack in statsApi.mutationAcknowledgements(chunk)) {
                if (ack.table == "games") {
                    val local = gameAttempts.firstOrNull { it.remoteId == ack.remoteId && it.lastAttemptKey == ack.mutationKey } ?: error("Unknown game receipt")
                    gameDao.markSynced(local.id, ack.remoteId, ack.clientRevision, ack.clientRevision, ack.serverVersion)
                    gameDao.clearAttempt(local.id, ack.mutationKey)
                } else {
                    val local = playerAttempts.firstOrNull { it.remoteId == ack.remoteId && it.lastAttemptKey == ack.mutationKey } ?: error("Unknown player receipt")
                    playerDao.markSynced(local.id, ack.remoteId, ack.clientRevision, ack.clientRevision, ack.serverVersion)
                    playerDao.clearAttempt(local.id, ack.mutationKey)
                }
            }
        }

        // Recover an ambiguous create before deleting it: its stable key returns the original row.
        for (p in playerDao.getPendingPlayers() + playerDao.getPendingDeletes()) attempt {
            val current = playerDao.getPlayerById(p.id) ?: return@attempt
            if (current.isDeleted && current.remoteId != null) {
                playerApi.deletePlayer(current.remoteId)
                playerDao.markDeleteSynced(current.id)
            } else {
                val result = if (current.remoteId == null) playerApi.uploadPlayer(current.toUploadDto())
                    else {
                        playerDao.markAttempt(current.id, current.mutationKey)
                        playerApi.updatePlayer(current.remoteId, current.toUploadDto())
                    }
                check(result.remoteId.isNotBlank()) { "Invalid player acknowledgement" }
                playerDao.markSynced(current.id, result.remoteId, current.localRevision, result.clientRevision, result.serverVersion)
                playerDao.clearAttempt(current.id, current.mutationKey)
                if (result.isDeleted) playerDao.markDeleted(current.id)
                if (current.isDeleted) {
                    playerApi.deletePlayer(result.remoteId)
                    playerDao.markDeleteSynced(current.id)
                }
            }
        }
        val blockedGames = mutableSetOf<Long>()
        for (g in gameDao.getPendingGames() + gameDao.getPendingDeletion()) {
            val succeeded = attempt {
            val current = gameDao.getById(g.id) ?: return@attempt
            if (current.isDeleted && current.remoteId != null) {
                gameApi.deleteGameFromCloud(current.remoteId)
                gameDao.markDeleteSynced(current.id)
            } else {
                val result = if (current.remoteId == null) gameApi.uploadGame(current.toUploadDto())
                    else {
                        gameDao.markAttempt(current.id, current.mutationKey)
                        gameApi.updateGame(current.remoteId, current.toUploadDto())
                    }
                check(result.remoteId.isNotBlank()) { "Invalid game acknowledgement" }
                gameDao.markSynced(current.id, result.remoteId, current.localRevision, result.clientRevision, result.serverVersion)
                gameDao.clearAttempt(current.id, current.mutationKey)
                if (result.isDeleted) gameDao.markAsDeleted(current.id)
                if (current.isDeleted) {
                    gameApi.deleteGameFromCloud(result.remoteId)
                    gameDao.markDeleteSynced(current.id)
                }
            }
        }
            if (!succeeded) blockedGames += g.id
        }
        for (r in rosterDao.getPendingRoster()) attempt {
            check(r.gameId !in blockedGames) { "Game update failed" }
            val game = gameDao.getById(r.gameId) ?: error("Roster game missing")
            val player = playerDao.getPlayerById(r.playerId) ?: error("Roster player missing")
            if (game.isDeleted) { rosterDao.markGameDeleted(game.id); return@attempt }
            val gameId = game.remoteId ?: error("Game upload pending")
            val playerId = player.remoteId ?: error("Player upload pending")
            val result = rosterApi.uploadRoster(RosterUploadDto(r.gameId, r.playerId, gameId, playerId))
            rosterDao.markSynced(r.gameId, r.playerId, result.remoteId)
        }
        val groups = eventDao.getPendingEvents().groupBy { it.actionGroupId ?: it.syncKey }
        for (events in groups.values) attempt {
            val current = events.mapNotNull { eventDao.getById(it.id) }.filter { it.syncStatus == "PENDING" || it.syncStatus == "UPLOADING" }
            if (current.isEmpty()) return@attempt
            check(current.first().gameId !in blockedGames) { "Game update failed" }
            val game = gameDao.getById(current.first().gameId) ?: error("Event game missing")
            if (game.isDeleted) return@attempt
            val gameId = game.remoteId ?: error("Game upload pending")
            val uploads = current.map { e ->
                val player = e.playerId?.let { playerDao.getPlayerById(it)?.remoteId ?: error("Player upload pending") }
                val assist = e.assistedByPlayerId?.let { playerDao.getPlayerById(it)?.remoteId ?: error("Assist player upload pending") }
                EventUploadDto(e.id, e.gameId, e.playerId, e.assistedByPlayerId, gameId, player, assist,
                    e.type, e.period, e.clockSecRemaining, e.createdAt, e.teamScoreAtEvent, e.opponentScoreAtEvent,
                    e.shotX, e.shotY, e.shotDistance, syncKey = e.syncKey, actionGroupId = e.actionGroupId, expectedGameVersion = game.serverVersion)
            }
            eventDao.markUploading(current.map { it.id })
            val responses = eventApi.uploadEvents(uploads)
            check(responses.size == current.size) { "Incomplete event acknowledgement" }
            val acknowledge: suspend () -> Unit = {
                responses.firstOrNull()?.gameServerVersion?.let { gameDao.acknowledgeEventVersion(game.id, it) }
                current.zip(responses).forEach { (e, result) ->
                    check(result.remoteId.isNotBlank()) { "Invalid event acknowledgement" }
                    eventDao.markSynced(e.id, result.remoteId)
                    if (result.isDeleted) eventDao.markDeleteSynced(e.id)
                }
            }
            if (database != null) database.withTransaction { acknowledge() } else acknowledge()
        }
        // Do this after uploads too: Undo may happen while an HTTP request is in flight.
        for (events in eventDao.getPendingDeleteEvents().groupBy { it.actionGroupId ?: it.syncKey }.values) attempt {
            val response = eventApi.deleteEvents(events.map {
                val game = gameDao.getById(it.gameId)
                EventDeleteDto(it.syncKey, it.remoteId, game?.remoteId, game?.serverVersion)
            })
            response.gameVersions.forEach { (remoteId, version) ->
                gameDao.getLocalIdByRemoteId(remoteId)?.let { gameDao.acknowledgeEventVersion(it, version) }
            }
            val acknowledge: suspend () -> Unit = { events.forEach { eventDao.markDeleteSynced(it.id) } }
            if (database != null) database.withTransaction { acknowledge() } else acknowledge()
        }
        if (errors.isNotEmpty()) throw SyncFailure(errors)
        // A mutation during an upload remains pending and must be retried, never overwritten by a pull.
        if (!(gameDao.getPendingGames().isEmpty() && gameDao.getPendingDeletion().isEmpty() &&
            playerDao.getPendingPlayers().isEmpty() && playerDao.getPendingDeletes().isEmpty() && rosterDao.getPendingRoster().isEmpty() &&
            eventDao.getPendingEvents().isEmpty() && eventDao.getPendingDeleteEvents().isEmpty())) throw SyncPendingChanges()
    }

    private suspend fun download() {
        // Complete every request before changing Room. A failed request cannot create a partial import.
        val snapshot = statsApi.snapshot()
        val players = snapshot.players
        val games = snapshot.games
        val roster = snapshot.roster
        val events = snapshot.events
        val apply: suspend () -> Unit = {
            playerDao.upsertPlayersFromCloud(players.map { p ->
                val old = playerDao.getLocalIdByRemoteId(p.id)?.let { playerDao.getPlayerById(it) }
                    ?: p.sync_key?.let { playerDao.getBySyncKey(it) }
                PlayerEntity(id = old?.id ?: 0, name = p.name, number = p.number, remoteId = p.id,
                    syncStatus = "SYNCED", syncKey = old?.syncKey ?: p.sync_key ?: java.util.UUID.randomUUID().toString(),
                    serverVersion = p.server_version, isDeleted = p.is_deleted)
            })
            gameDao.upsertFromCloud(games.map { g ->
                val old = gameDao.getLocalIdByRemoteId(g.id)?.let { gameDao.getById(it) }
                    ?: g.sync_key?.let { gameDao.getBySyncKey(it) }
                GameEntity(opponentName = g.opponent_name, isHomeGame = g.is_home_game, roundNumber = g.round_number,
                    isPlayoff = g.is_playoff, playoffStage = g.playoff_stage, playoffGameNumber = g.playoff_game_number,
                    gameDateEpoch = g.game_date_epoch, createdAt = g.created_at, quarterLengthSec = g.quarter_length_sec,
                    quartersCount = g.quarters_count, teamScore = g.team_score, opponentScore = g.opponent_score,
                    status = g.status, currentPeriod = g.current_period, clockSecRemaining = g.clock_sec_remaining,
                    isClockRunning = g.is_clock_running, lastClockStartedAt = g.last_clock_started_at,
                    remoteId = g.id, syncStatus = "SYNCED", isDeleted = g.is_deleted,
                    syncKey = old?.syncKey ?: g.sync_key ?: java.util.UUID.randomUUID().toString(),
                    serverVersion = g.server_version, season = g.season ?: old?.season ?: GameSeason.LEGACY)
            })
            roster.forEach { r ->
                val game = gameDao.getLocalIdByRemoteId(r.game_remote_id) ?: error("Unresolved roster game")
                val player = playerDao.getLocalIdByRemoteId(r.player_remote_id) ?: error("Unresolved roster player")
                if (r.is_deleted) rosterDao.removeFromCloud(game, player)
                else rosterDao.mergeFromCloud(listOf(RosterEntity(game, player, r.id, "SYNCED")))
            }
            eventDao.upsertEventsFromCloud(events.map { e ->
                val game = gameDao.getLocalIdByRemoteId(e.game_remote_id) ?: error("Unresolved event game")
                val player = e.player_remote_id?.let { playerDao.getLocalIdByRemoteId(it) ?: error("Unresolved event player") }
                val assist = e.assisted_by_player_remote_id?.let { playerDao.getLocalIdByRemoteId(it) ?: error("Unresolved assist player") }
                val old = eventDao.getLocalIdByRemoteId(e.id)?.let { eventDao.getById(it) }
                    ?: e.sync_key?.let { eventDao.getBySyncKey(it) }
                EventEntity(gameId = game, playerId = player, assistedByPlayerId = assist, type = e.type,
                    period = e.period, clockSecRemaining = e.clock_sec_remaining, createdAt = e.created_at,
                    teamScoreAtEvent = e.team_score_at_event, opponentScoreAtEvent = e.opponent_score_at_event,
                    shotX = e.shot_x, shotY = e.shot_y, shotDistance = e.shot_distance, remoteId = e.id,
                    syncStatus = if (e.is_deleted) "DELETED" else "SYNCED",
                    syncKey = old?.syncKey ?: e.sync_key ?: java.util.UUID.randomUUID().toString(),
                    serverVersion = e.server_version, actionGroupId = e.action_group_id, sourceOrder = e.local_id ?: old?.sourceOrder)
            })
            // The complete snapshot also recovers hard deletions made by legacy servers.
            val eventIds = events.map { it.id }.toHashSet()
            eventDao.syncedRemoteIds().filterNot { it in eventIds }.chunked(500).forEach { eventDao.markRemoteMissing(it) }
            val rosterIds = roster.map { it.id }.toHashSet()
            rosterDao.syncedRemoteIds().filterNot { it in rosterIds }.chunked(500).forEach { rosterDao.markRemoteMissing(it) }
            val gameIds = games.map { it.id }.toHashSet()
            gameDao.syncedRemoteIds().filterNot { it in gameIds }.chunked(500).forEach { gameDao.markRemoteMissing(it) }
        }
        if (database != null) database.withTransaction { apply() } else apply()
    }
}

class SyncFailure(val failures: List<Exception>) : Exception(
    "Changes are saved locally; cloud sync failed (" + failures.size + "). " +
        (if (failures.any { it is retrofit2.HttpException && it.code() == 409 })
            "A server conflict needs review." else "Will retry when connected."),
    failures.firstOrNull()
)

class SyncPendingChanges : IllegalStateException("New local changes are waiting to upload.")
