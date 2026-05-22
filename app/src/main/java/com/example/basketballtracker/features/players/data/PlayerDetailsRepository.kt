package com.example.basketballtracker.features.players.data

import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.livegame.domain.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PlayerDetailsRepository(
    private val playerDao: PlayerDao,
    private val gameDao: GameDao,
    private val eventDao: EventDao
) {

    fun observePlayerDetails(playerId: Long): Flow<PlayerDetailsUiState> {
        return combine(
            playerDao.observePlayerById(playerId),
            gameDao.observeAllGames(),
            eventDao.observeEventsByPlayer(playerId)
        ) { player, games, events ->

            val gamesById = games.associateBy { it.id }

            val statsPerGame = events
                .groupBy { it.gameId }
                .mapNotNull { (gameId, gameEvents) ->
                    val game = gamesById[gameId] ?: return@mapNotNull null

                    buildPlayerGameStats(
                        game = game,
                        events = gameEvents
                    )
                }
                .sortedByDescending { it.game.gameDateEpoch }

            val averages = buildAverages(statsPerGame)

            PlayerDetailsUiState(
                isLoading = false,
                player = player,
                averages = averages,
                games = statsPerGame
            )
        }
    }

    private fun buildPlayerGameStats(
        game: GameEntity,
        events: List<EventEntity>
    ): PlayerGameStats {

        val twoMade = events.count { it.type == EventType.TWO_MADE.name }
        val twoMiss = events.count { it.type == EventType.TWO_MISS.name }
        val threeMade = events.count { it.type == EventType.THREE_MADE.name }
        val threeMiss = events.count { it.type == EventType.THREE_MISS.name }
        val ftMade = events.count { it.type == EventType.FT_MADE.name }
        val ftMiss = events.count { it.type == EventType.FT_MISS.name }

        val points = twoMade * 2 + threeMade * 3 + ftMade

        val shots = events.filter {
            it.type == EventType.TWO_MADE.name ||
                    it.type == EventType.TWO_MISS.name ||
                    it.type == EventType.THREE_MADE.name ||
                    it.type == EventType.THREE_MISS.name
        }

        return PlayerGameStats(
            game = game,
            points = points,
            rebounds = events.count { it.type == EventType.REB_DEF.name } + events.count { it.type == EventType.REB_OFF.name },
            assists = events.count { it.type == EventType.AST.name },
            steals = events.count { it.type == EventType.STL.name },
            blocks = events.count { it.type == EventType.BLK.name },
            turnovers = events.count { it.type == EventType.TOV.name },
            twoMade = twoMade,
            twoMiss = twoMiss,
            threeMade = threeMade,
            threeMiss = threeMiss,
            ftMade = ftMade,
            ftMiss = ftMiss,
            shots = shots
        )
    }

    private fun buildAverages(games: List<PlayerGameStats>): PlayerAverages {
        if (games.isEmpty()) return PlayerAverages()

        fun avg(selector: (PlayerGameStats) -> Int): Double {
            return games.map(selector).average()
        }

        return PlayerAverages(
            ppg = avg { it.points },
            rpg = avg { it.rebounds },
            apg = avg { it.assists },
            spg = avg { it.steals },
            bpg = avg { it.blocks },
            tpg = avg { it.turnovers }
        )
    }
}

data class PlayerDetailsUiState(
    val isLoading: Boolean = true,
    val player: PlayerEntity? = null,
    val averages: PlayerAverages = PlayerAverages(),
    val games: List<PlayerGameStats> = emptyList()
)

data class PlayerAverages(
    val ppg: Double = 0.0,
    val rpg: Double = 0.0,
    val apg: Double = 0.0,
    val spg: Double = 0.0,
    val bpg: Double = 0.0,
    val tpg: Double = 0.0
)

data class PlayerGameStats(
    val game: GameEntity,
    val points: Int,
    val rebounds: Int,
    val assists: Int,
    val steals: Int,
    val blocks: Int,
    val turnovers: Int,
    val twoMade: Int,
    val twoMiss: Int,
    val threeMade: Int,
    val threeMiss: Int,
    val ftMade: Int,
    val ftMiss: Int,
    val shots: List<EventEntity>
)