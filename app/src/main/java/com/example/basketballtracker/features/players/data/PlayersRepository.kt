package com.example.basketballtracker.features.players.data

import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

class PlayersRepository(private val playerDao: PlayerDao) {
    fun observePlayers(): Flow<List<PlayerEntity>> = playerDao.observePlayers()

    suspend fun addPlayer(name: String, number: Int) {
        playerDao.insert(PlayerEntity(name = name, number = number))
    }

    suspend fun updatePlayer(id: Long, name: String, number: Int) {
        playerDao.updateById(id, name, number)
    }

    suspend fun deletePlayer(id: Long) {
        playerDao.deleteById(id)
    }
}