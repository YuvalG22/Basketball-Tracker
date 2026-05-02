package com.example.basketballtracker.features.games.data

import android.util.Log
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.games.GameApi
import com.example.basketballtracker.core.data.remote.games.GameScoreUpdateDto
import kotlinx.coroutines.flow.Flow

class GamesRepository(private val gameDao: GameDao, private val gameApi: GameApi) {
    fun observeLastGameId(): Flow<Long?> = gameDao.observeLastGameId()

    suspend fun createGame(
        opponentName: String,
        isHomeGame: Boolean,
        roundNumber: Int,
        gameDateEpoch: Long,
        quarterLengthSec: Int
    ): Long {

        val game = GameEntity(
            opponentName = opponentName,
            isHomeGame = isHomeGame,
            roundNumber = roundNumber,
            gameDateEpoch = gameDateEpoch,
            createdAt = System.currentTimeMillis(),
            quarterLengthSec = quarterLengthSec,
            quartersCount = 4,
            syncStatus = "PENDING"
        )

        val localId = gameDao.insert(game)

        try {
            val response = gameApi.uploadGame(
                game.copy(id = localId).toUploadDto()
            )

            gameDao.markSynced(
                localId = localId,
                remoteId = response.remoteId
            )

        } catch (e: Exception) {
            e.printStackTrace()
            // נשאר PENDING → יסונכרן בהמשך
        }

        return localId
    }

    suspend fun getById(id: Long): GameEntity? = gameDao.getById(id)

    fun observeGames(): Flow<List<GameEntity>> =
        gameDao.observeAllGames()

    fun observeGame(id: Long): Flow<GameEntity?> = gameDao.observeGame(id)

    suspend fun updateGameResult(gameId: Long, teamScore: Int, opponentScore: Int) {
        gameDao.updateGameResult(gameId, teamScore, opponentScore)
        val game = gameDao.getById(gameId) ?: return
        val remoteId = game.remoteId ?: return
        Log.d("GameRepository", "Updating game $remoteId")

        try {
            gameApi.updateGameScore(
                GameScoreUpdateDto(
                    remoteId = remoteId,
                    teamScore = teamScore,
                    opponentScore = opponentScore
                )
            )
            Log.d("GameRepository", "Game score updated successfully")
        } catch (e: Exception) {
            Log.d("GameRepository", "Failed to update game score")
            e.printStackTrace()
        }
    }

    suspend fun deleteGame(gameId: Long) {
        gameDao.deleteById(gameId)
    }
}