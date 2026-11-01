package com.example.basketballtracker.features.players.data

import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.dao.PlayerStatSummary
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.mapper.toUploadDto
import com.example.basketballtracker.core.data.remote.RetrofitClient.playerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

class PlayersRepository(private val playerDao: PlayerDao) {
    fun observePlayers(): Flow<List<PlayerEntity>> = playerDao.observePlayers()

    suspend fun addPlayer(name: String, number: Int, photoUri: String? = null): Long {

        val player = PlayerEntity(
            name = name,
            number = number,
            syncStatus = "PENDING",
            photoUri = photoUri
        )

        val localId = playerDao.insert(player)

        try {
            val response = playerApi.uploadPlayer(
                player.copy(id = localId).toUploadDto()
            )

            playerDao.markSynced(
                localId = localId,
                remoteId = response.remoteId
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return localId
    }

    suspend fun updatePlayer(id: Long, name: String, number: Int, photoUri: String?) {
        playerDao.updateDetailsById(id, name, number, photoUri)
    }

    suspend fun deletePlayer(id: Long) {
        playerDao.deleteById(id)
    }
}
