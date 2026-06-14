package com.example.basketballtracker.features.players.data

import com.example.basketballtracker.core.data.db.dao.EventDao
import com.example.basketballtracker.core.data.db.dao.GameDao
import com.example.basketballtracker.core.data.db.dao.PlayerDao
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedByPlayer
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedForPlayer
import com.example.basketballtracker.features.livegame.domain.computeSecondsPlayedForPlayerAfterEnd
import com.example.basketballtracker.features.livegame.domain.formatMinutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PlayerDetailsRepository(
    private val playerDao: PlayerDao,
    private val gameDao: GameDao,
    private val eventDao: EventDao
) {
    fun observePlayerDetails(
        playerId: Long,
        selectedPeriod: Int? = null
    ): Flow<PlayerDetailsUiState> {
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
                        playerId = playerId,
                        game = game,
                        events = gameEvents,
                        selectedPeriod = selectedPeriod
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
        playerId: Long,
        game: GameEntity,
        events: List<EventEntity>,
        selectedPeriod: Int? = null
    ): PlayerGameStats {

        val filteredEvents = if (selectedPeriod == null) {
            events
        } else {
            events.filter { it.period == selectedPeriod }
        }

        val twoMade = filteredEvents.count { it.type == EventType.TWO_MADE.name }
        val twoMiss = filteredEvents.count { it.type == EventType.TWO_MISS.name }
        val threeMade = filteredEvents.count { it.type == EventType.THREE_MADE.name }
        val threeMiss = filteredEvents.count { it.type == EventType.THREE_MISS.name }
        val ftMade = filteredEvents.count { it.type == EventType.FT_MADE.name }
        val ftMiss = filteredEvents.count { it.type == EventType.FT_MISS.name }

        val points = twoMade * 2 + threeMade * 3 + ftMade
        val secondsPlayed = computeSecondsPlayedForPlayerAfterEnd(
            playerId = playerId,
            events = events,
            quarterLengthSec = 600,
            currentPeriod = 0,
            currentClockSecRemaining = 0
        )
        val shots = filteredEvents.filter {
            it.type == EventType.TWO_MADE.name ||
                    it.type == EventType.TWO_MISS.name ||
                    it.type == EventType.THREE_MADE.name ||
                    it.type == EventType.THREE_MISS.name
        }

        return PlayerGameStats(
            game = game,
            secondsPlayed = formatMinutes(secondsPlayed).toInt(),
            points = points,
            rebounds = filteredEvents.count { it.type == EventType.REB_DEF.name } +
                    filteredEvents.count { it.type == EventType.REB_OFF.name },
            rebDef = filteredEvents.count { it.type == EventType.REB_DEF.name },
            rebOff = filteredEvents.count { it.type == EventType.REB_OFF.name },
            assists = filteredEvents.count { it.type == EventType.AST.name },
            steals = filteredEvents.count { it.type == EventType.STL.name },
            blocks = filteredEvents.count { it.type == EventType.BLK.name },
            turnovers = filteredEvents.count { it.type == EventType.TOV.name },
            pf = filteredEvents.count { it.type == EventType.PF.name },

            twoMade = twoMade,
            twoMiss = twoMiss,
            threeMade = threeMade,
            threeMiss = threeMiss,
            ftMade = ftMade,
            ftMiss = ftMiss,

            shots = shots,
            events = events
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
    val secondsPlayed: Int,
    val points: Int,
    val rebounds: Int,
    val rebDef: Int,
    val rebOff: Int,
    val assists: Int,
    val steals: Int,
    val blocks: Int,
    val turnovers: Int,
    val pf: Int,
    val twoMade: Int,
    val twoMiss: Int,
    val threeMade: Int,
    val threeMiss: Int,
    val ftMade: Int,
    val ftMiss: Int,
    val shots: List<EventEntity>,
    val events: List<EventEntity>
) {
    val twoAttempts: Int
        get() = twoMade + twoMiss

    val threeAttempts: Int
        get() = threeMade + threeMiss

    val ftAttempts: Int
        get() = ftMade + ftMiss

    val fgMade: Int
        get() = twoMade + threeMade

    val fgAttempts: Int
        get() = twoAttempts + threeAttempts

    val twoPct: Double
        get() = percent(twoMade, twoAttempts)

    val threePct: Double
        get() = percent(threeMade, threeAttempts)

    val ftPct: Double
        get() = percent(ftMade, ftAttempts)

    val fgPct: Double
        get() = percent(fgMade, fgAttempts)

    private fun percent(made: Int, attempts: Int): Double {
        return if (attempts == 0) 0.0 else made * 100.0 / attempts
    }
}