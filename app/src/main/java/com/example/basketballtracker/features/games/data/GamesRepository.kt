package com.example.basketballtracker.features.games.data

import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.entities.GameEntity
import kotlinx.coroutines.flow.Flow

class GamesRepository(private val gameDao: GameDao) {
    fun observeLastGameId(): Flow<Long?> = gameDao.observeLastGameId()
    suspend fun createGame(opponentName: String, quarterLengthSec: Int): Long {
        return gameDao.insert(
            GameEntity(
                opponentName = opponentName,
                createdAt = System.currentTimeMillis(),
                quarterLengthSec = quarterLengthSec,
                quartersCount = 4
            )
        )
    }
}