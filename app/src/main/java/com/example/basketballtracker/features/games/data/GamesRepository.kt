package com.example.basketballtracker.features.games.data

import androidx.room.Query
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.entities.GameEntity
import kotlinx.coroutines.flow.Flow

class GamesRepository(private val gameDao: GameDao) {
    fun observeLastGameId(): Flow<Long?> = gameDao.observeLastGameId()
    suspend fun createGame(
        opponentName: String,
        roundNumber: Int,
        gameDateEpoch: Long,
        quarterLengthSec: Int
    ): Long {
        return gameDao.insert(
            GameEntity(
                opponentName = opponentName,
                roundNumber = roundNumber,
                gameDateEpoch = gameDateEpoch,
                createdAt = System.currentTimeMillis(),
                quarterLengthSec = quarterLengthSec,
                quartersCount = 4
            )
        )
    }

    suspend fun getById(id: Long): GameEntity? = gameDao.getById(id)

    fun observeGames(): Flow<List<GameEntity>> =
        gameDao.observeAllGames()

    fun observeGame(id: Long): Flow<GameEntity?> = gameDao.observeGame(id)

    suspend fun updateGameResult(gameId: Long, teamScore: Int, opponentScore: Int) {
        gameDao.updateGameResult(gameId, teamScore, opponentScore)
    }

    suspend fun deleteGame(gameId: Long) {
        gameDao.deleteById(gameId)
    }
}