package com.example.basketballtracker.features.stats.data
import com.example.basketballtracker.core.data.db.dao.*
import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats
import com.example.basketballtracker.features.livegame.domain.*
import com.example.basketballtracker.features.livegame.data.toDomain
import kotlinx.coroutines.flow.*
data class SeasonalData(val seasons: List<String> = listOf(GameSeason.CURRENT), val games: List<GameEntity> = emptyList(), val events: List<EventEntity> = emptyList(), val players: List<PlayerEntity> = emptyList()) {
    private fun finishedGames(season: String) = games.filter {
        it.season == season && !it.isDeleted && it.status == GameStatus.FINISHED
    }

    private fun recordedEvents(season: String): List<EventEntity> {
        val ids = finishedGames(season).map { it.id }.toSet()
        return events.filter { it.gameId in ids && it.syncStatus != "PENDING_DELETE" }
    }

    // Missing play-by-play is unknown, not a game with zero steals/rebounds/etc.
    fun recordedGameCount(season: String): Int = recordedEvents(season)
        .filter { it.type in recordedStatTypes }
        .map { it.gameId }.distinct().size

    fun teamPoints(season: String): Int = games
        .filter { it.season == season && !it.isDeleted && it.status == GameStatus.FINISHED }
        .sumOf { it.teamScore }

    fun stats(season: String): List<PlayerSeasonStats> {
        val selected = recordedEvents(season)
        val boxes = computeBoxByPlayer(selected.map { it.toDomain() }, 600, 4, 0)
        return boxes.map { (id, b) ->
            val player = players.find { it.id == id }
            PlayerSeasonStats(id, player?.name ?: "Unknown player", player?.number ?: 0,
                selected.filter { it.playerId == id }.map { it.gameId }.distinct().size,
                b.pts,b.ast,b.rebTotal,b.rebDef,b.rebOff,b.stl,b.blk,b.tov,b.pf,b.fgm,b.fga,b.threem,b.threea,b.ftm,b.fta,player?.photoUri)
        }
    }
}

private val recordedStatTypes = setOf(
    "TWO_MADE", "TWO_MISS", "THREE_MADE", "THREE_MISS", "FT_MADE", "FT_MISS",
    "REB_DEF", "REB_OFF", "AST", "STL", "BLK", "TOV", "PF"
)
class SeasonStatsRepository(private val gameDao: GameDao, private val playerDao: PlayerDao, private val eventDao: EventDao) {
    fun observe(): Flow<SeasonalData> = combine(gameDao.observeAllGames(), playerDao.observePlayers(), eventDao.getAllEvents()) { games, players, events ->
        SeasonalData((games.filterNot { it.isDeleted }.map { it.season } + GameSeason.CURRENT).distinct().sortedDescending(),
            games.filter { !it.isDeleted && it.status == GameStatus.FINISHED }, events.filter { it.syncStatus != "PENDING_DELETE" }, players)
    }
}
