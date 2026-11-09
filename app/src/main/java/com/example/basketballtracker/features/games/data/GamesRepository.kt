package com.example.basketballtracker.features.games.data

import android.util.Log
import androidx.room.withTransaction
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.core.data.db.entities.GameSeason
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.games.DeleteGameDto
import com.example.basketballtracker.core.data.remote.games.GameApi
import com.example.basketballtracker.core.data.remote.games.GameScoreUpdateDto
import kotlinx.coroutines.flow.Flow

class GamesRepository(private val gameDao: GameDao, private val gameApi: GameApi, private val onChanged: () -> Unit = {}, private val database: AppDatabase? = null) {
    fun observeLastGameId(): Flow<Long?> = gameDao.observeLastGameId()

    suspend fun createGame(
        opponentName: String,
        isHomeGame: Boolean,
        roundNumber: Int,
        gameDateEpoch: Long,
        quarterLengthSec: Int,
        isPlayoff: Boolean,
        playoffStage: String?,
        playoffGameNumber: Int?,
        playerIds: List<Long> = emptyList(),
        syncKey: String = java.util.UUID.randomUUID().toString()
    ): Long {

        gameDao.getBySyncKey(syncKey)?.let { return it.id }
        val game = GameEntity(
            syncKey = syncKey,
            season = GameSeason.CURRENT,
            opponentName = opponentName,
            isHomeGame = isHomeGame,
            roundNumber = roundNumber,
            gameDateEpoch = gameDateEpoch,
            isPlayoff = isPlayoff,
            playoffStage = playoffStage,
            playoffGameNumber = playoffGameNumber,
            createdAt = System.currentTimeMillis(),
            quarterLengthSec = quarterLengthSec,
            clockSecRemaining = quarterLengthSec,
            quartersCount = 4,
            syncStatus = "PENDING",
            status = GameStatus.LIVE
        )

        val save: suspend () -> Long = save@ {
            val existing = gameDao.getBySyncKey(syncKey)
            val id = existing?.id ?: gameDao.insert(game)
            if (existing != null) return@save id
            database?.rosterDao()?.insertAll(playerIds.map { RosterEntity(id, it) })
            id
        }
        val localId = if (database != null) database.withTransaction { save() } else save()

        onChanged()

        return localId
    }

    suspend fun getById(id: Long): GameEntity? = gameDao.getById(id)

    suspend fun startScheduledGame(id: Long, playerIds: List<Long>, quarterLengthSec: Int): Long {
        val db = checkNotNull(database)
        db.withTransaction {
            val game = checkNotNull(gameDao.getById(id)) { "Game unavailable" }
            check(!game.isDeleted) { "Game unavailable" }
            if (game.status == GameStatus.LIVE) return@withTransaction
            check(game.status == GameStatus.SCHEDULED) { "Game already finished" }
            val ids = playerIds.distinct()
            require(ids.size >= 5 && ids.all { db.playerDao().getPlayerById(it)?.isDeleted == false }) {
                "Select at least five available players"
            }
            require(quarterLengthSec > 0)
            db.rosterDao().insertAll(ids.map { RosterEntity(id, it) })
            gameDao.update(game.copy(status = GameStatus.LIVE, quarterLengthSec = quarterLengthSec,
                quartersCount = 4, currentPeriod = 1, clockSecRemaining = quarterLengthSec,
                isClockRunning = false, lastClockStartedAt = null, syncStatus = "PENDING",
                localRevision = game.localRevision + 1, mutationKey = java.util.UUID.randomUUID().toString()))
        }
        onChanged()
        return id
    }

    fun observeGames(): Flow<List<GameEntity>> =
        gameDao.observeAllGames()

    fun observeGame(id: Long): Flow<GameEntity?> = gameDao.observeGame(id)

    suspend fun updateGameResult(gameId: Long, teamScore: Int, opponentScore: Int) {
        gameDao.updateGameResult(gameId, teamScore, opponentScore)
        onChanged()
    }

    suspend fun deleteGame(gameId: Long) {
        gameDao.markAsDeleted(gameId)
        onChanged()
    }

    suspend fun updateGameStatus(gameId: Long, status: String) {
        gameDao.updateGameStatus(gameId, status)
        onChanged()
    }

    private var lastClockRequest = 0L

    suspend fun updateLiveState(
        gameId: Long,
        period: Int,
        secRemaining: Int,
        isRunning: Boolean,
        lastStartedAt: Long?
    ) {
        gameDao.updateLiveState(
            gameId,
            period,
            secRemaining,
            isRunning,
            lastStartedAt
        )
        val now = android.os.SystemClock.elapsedRealtime()
        if (!isRunning || now - lastClockRequest >= 5_000) {
            lastClockRequest = now
            onChanged()
        }
    }

    suspend fun finishGame(
        gameId: Long,
        teamScore: Int,
        opponentScore: Int,
        period: Int? = null,
        seconds: Int? = null
    ) {
        gameDao.finishGame(
            gameId = gameId,
            teamScore = teamScore,
            opponentScore = opponentScore,
            status = GameStatus.FINISHED,
            period = period,
            seconds = seconds
        )
        onChanged()
    }
}
