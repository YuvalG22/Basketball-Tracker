package com.example.basketballtracker.features.stats.data

import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SeasonStatsRepository(
    private val playerDao: PlayerDao,
    private val eventDao: EventDao
) {
    private fun List<EventEntity>.countType(t: EventType): Int =
        count { it.type == t.name }

    private fun List<EventEntity>.countTypes(vararg ts: EventType): Int {
        val set = ts.map { it.name }.toSet()
        return count { it.type in set }
    }

    fun seasonStats(): Flow<List<PlayerSeasonStats>> {
        return combine(
            playerDao.observePlayers(),
            eventDao.getAllEvents()
        ) { players, events ->

            players.map { player ->

                val playerEvents = events.filter { it.playerId == player.id }

                val gamesPlayed = playerEvents
                    .map { it.gameId }
                    .distinct()
                    .count()

                PlayerSeasonStats(
                    playerId = player.id,
                    playerName = player.name,
                    playerNumber = player.number,

                    gp = gamesPlayed,

                    pts =
                        playerEvents.countType(EventType.TWO_MADE) * 2 +
                                playerEvents.countType(EventType.THREE_MADE) * 3 +
                                playerEvents.countType(EventType.FT_MADE),

                    ast = playerEvents.countType(EventType.AST),
                    rebTotal = playerEvents.countType(EventType.REB_DEF) + playerEvents.countType(EventType.REB_OFF),
                    rebDef = playerEvents.countType(EventType.REB_DEF),
                    rebOff = playerEvents.countType(EventType.REB_OFF),
                    stl = playerEvents.countType(EventType.STL),
                    blk = playerEvents.countType(EventType.BLK),
                    tov = playerEvents.countType(EventType.TOV),
                    pf = playerEvents.countType(EventType.PF),

                    fgm = playerEvents.countTypes(EventType.TWO_MADE, EventType.THREE_MADE),
                    fga = playerEvents.countTypes(
                        EventType.TWO_MADE,
                        EventType.THREE_MADE,
                        EventType.TWO_MISS,
                        EventType.THREE_MISS
                    ),

                    threem = playerEvents.countType(EventType.THREE_MADE),
                    threea = playerEvents.countTypes(EventType.THREE_MADE, EventType.THREE_MISS),

                    ftm = playerEvents.countType(EventType.FT_MADE),
                    fta = playerEvents.countTypes(EventType.FT_MADE, EventType.FT_MISS)
                )
            }
        }
    }

    fun gameStats(gameId: Long): Flow<List<PlayerSeasonStats>> {
        return combine(
            playerDao.observePlayers(),
            eventDao.observeEvents(gameId)
        ) { allPlayers, events ->
            val participantIds = events.map { it.playerId }.toSet()
            allPlayers.filter { it.id in participantIds }.map { player ->
                val playerEvents = events.filter { it.playerId == player.id }
                val gamesPlayed = playerEvents
                    .map { it.gameId }
                    .distinct()
                    .count()

                PlayerSeasonStats(
                    playerId = player.id,
                    playerName = player.name,
                    playerNumber = player.number,

                    gp = gamesPlayed,

                    pts =
                        playerEvents.countType(EventType.TWO_MADE) * 2 +
                                playerEvents.countType(EventType.THREE_MADE) * 3 +
                                playerEvents.countType(EventType.FT_MADE),

                    ast = playerEvents.countType(EventType.AST),
                    rebTotal = playerEvents.countType(EventType.REB_DEF) + playerEvents.countType(EventType.REB_OFF),
                    rebDef = playerEvents.countType(EventType.REB_DEF),
                    rebOff = playerEvents.countType(EventType.REB_OFF),
                    stl = playerEvents.countType(EventType.STL),
                    blk = playerEvents.countType(EventType.BLK),
                    tov = playerEvents.countType(EventType.TOV),
                    pf = playerEvents.countType(EventType.PF),

                    fgm = playerEvents.countTypes(EventType.TWO_MADE, EventType.THREE_MADE),
                    fga = playerEvents.countTypes(
                        EventType.TWO_MADE,
                        EventType.THREE_MADE,
                        EventType.TWO_MISS,
                        EventType.THREE_MISS
                    ),

                    threem = playerEvents.countType(EventType.THREE_MADE),
                    threea = playerEvents.countTypes(EventType.THREE_MADE, EventType.THREE_MISS),

                    ftm = playerEvents.countType(EventType.FT_MADE),
                    fta = playerEvents.countTypes(EventType.FT_MADE, EventType.FT_MISS)
                )
            }
        }
    }
}