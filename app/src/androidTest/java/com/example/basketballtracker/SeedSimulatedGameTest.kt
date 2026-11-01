package com.example.basketballtracker

import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.example.basketballtracker.core.data.db.AppDatabase
import com.example.basketballtracker.core.data.db.entities.EventEntity
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameStatus
import com.example.basketballtracker.core.data.db.entities.GameSeason
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.features.livegame.data.toDomain
import com.example.basketballtracker.features.livegame.domain.*
import com.example.basketballtracker.features.players.data.buildPlayerGameStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.sqrt
import kotlin.random.Random

/** Explicitly opt-in device fixture. Writes to the real app database and keeps the game. */
class SeedSimulatedGameTest {
    @Test
    fun createPersistentSimulation() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Requires -e seedRealGame true; ordinary test runs must not change user data",
            args.getString("seedRealGame") == "true")
        val seed = args.getString("simulationSeed", "20261029").toInt()
        val name = "Simulation - Falcons ($seed)"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(context)
        val reportFile = File(context.filesDir, "simulation-$seed.json")
        var gameId = 0L
        try {
            val games = db.gameDao().observeAllGames().first()
            val existing = games.firstOrNull { it.opponentName == name && !it.isDeleted }
            if (existing != null) {
                check(reportFile.exists()) { "Simulation already exists as game ${existing.id}; no duplicate created" }
                println("SIMULATION_ALREADY_EXISTS gameId=${existing.id} report=${reportFile.name}")
                return@runBlocking
            }
            val players = db.playerDao().observePlayers().first().sortedBy { it.number }.take(10)
            check(players.size >= 5) { "At least five existing roster players are required" }
            val now = System.currentTimeMillis()
            val simulation = Simulation(players, seed, now - 40 * 60_000L)
            simulation.play()
            // Use the same entities, DAO writes, score snapshots and PENDING status as
            // manual recording. One transaction prevents a partially seeded game.
            db.withTransaction {
                gameId = db.gameDao().insert(GameEntity(opponentName = name, isHomeGame = true,
                    season = GameSeason.CURRENT,
                    roundNumber = (games.maxOfOrNull { it.roundNumber } ?: 0) + 1,
                    gameDateEpoch = now, createdAt = now, status = GameStatus.LIVE))
                players.forEach { db.rosterDao().insert(RosterEntity(gameId, it.id)) }
                simulation.events.forEach { db.eventDao().insert(it.copy(gameId = gameId)) }
                db.gameDao().updateLiveState(gameId, 4, 0, false, null)
                db.gameDao().finishGame(gameId, simulation.teamScore, simulation.opponentScore, GameStatus.FINISHED)
                simulation.verify(db.eventDao().getEvents(gameId), db.gameDao().getById(gameId)!!)
                assertEquals(players.map { it.id }.toSet(),
                    db.rosterDao().observeRosterPlayerIds(gameId).first().toSet())
            }
            reportFile.writeText(simulation.report(gameId, name).toString(2))
        } finally {
            db.close()
        }
        // Reopen the actual on-disk database, rather than trusting an in-memory result.
        val reopened = AppDatabase.getDatabase(context)
        try {
            val report = JSONObject(reportFile.readText())
            val savedGame = reopened.gameDao().getById(gameId)!!
            assertEquals(GameStatus.FINISHED, savedGame.status)
            assertEquals(4, savedGame.currentPeriod)
            assertEquals(0, savedGame.clockSecRemaining)
            assertEquals(report.getInt("eventCount"), reopened.eventDao().getEvents(gameId).size)
            println("SIMULATION_SAVED gameId=$gameId opponent=$name score=${savedGame.teamScore}-${savedGame.opponentScore} events=${report.getInt("eventCount")} report=${reportFile.name}")
        } finally {
            reopened.close()
        }
    }

    private class Simulation(val players: List<PlayerEntity>, val seed: Int, val startedAt: Long) {
        val events = mutableListOf<EventEntity>()
        val random = Random(seed)
        val onCourt = linkedSetOf<Long>()
        val seconds = players.associate { it.id to IntArray(4) }
        val plusMinus = players.associate { it.id to IntArray(4) }
        val fouls = players.associate { it.id to 0 }.toMutableMap()
        val quarterTeam = IntArray(4)
        val quarterOpponent = IntArray(4)
        var teamScore = 0
        var opponentScore = 0
        var period = 1
        var clock = 600

        fun emit(type: EventType, player: Long? = null, assist: Long? = null,
                 shot: Pair<Float, Float>? = null) {
            val teamPoints = when (type) {
                EventType.FT_MADE -> 1
                EventType.TWO_MADE -> 2
                EventType.THREE_MADE -> 3
                else -> 0
            }
            val opponentPoints = when (type) {
                EventType.OPP_FT_MADE -> 1
                EventType.OPP_TWO_MADE -> 2
                EventType.OPP_THREE_MADE -> 3
                else -> 0
            }
            teamScore += teamPoints
            opponentScore += opponentPoints
            quarterTeam[period - 1] += teamPoints
            quarterOpponent[period - 1] += opponentPoints
            onCourt.forEach { plusMinus.getValue(it)[period - 1] += teamPoints - opponentPoints }
            if (type == EventType.SUB_IN) check(onCourt.add(player!!))
            if (type == EventType.SUB_OUT) check(onCourt.remove(player!!))
            if (type.requiresPlayer()) check(player in onCourt)
            if (type == EventType.PF) fouls[player!!] = fouls.getValue(player) + 1
            val attachScore = type.isScoreEvent() || type.isOpponentEvent()
            events += EventEntity(gameId = 0, playerId = player, assistedByPlayerId = assist,
                type = type.name, period = period, clockSecRemaining = clock,
                createdAt = startedAt + ((period - 1) * 600 + 600 - clock) * 1000L + events.size,
                teamScoreAtEvent = if (attachScore) teamScore else null,
                opponentScoreAtEvent = if (attachScore) opponentScore else null,
                shotX = shot?.first, shotY = shot?.second,
                shotDistance = shot?.let { (x, y) -> sqrt((x - 7.5f) * (x - 7.5f) + (y - 1.575f) * (y - 1.575f)) })
        }

        fun advance(nextClock: Int) {
            check(onCourt.size == 5)
            check(nextClock <= clock)
            onCourt.forEach { seconds.getValue(it)[period - 1] += clock - nextClock }
            clock = nextClock
        }

        fun currentPlayer() = onCourt.toList().random(random)

        fun rotate() {
            val bench = players.filter { it.id !in onCourt }
            if (bench.isEmpty()) return
            // Prefer the least-used bench player so everyone gets meaningful minutes.
            val incoming = bench.shuffled(random).minBy { seconds.getValue(it.id).sum() }
            val outgoing = onCourt.toList().shuffled(random).maxBy { seconds.getValue(it).sum() }
            emit(EventType.SUB_OUT, outgoing)
            emit(EventType.SUB_IN, incoming.id)
        }

        fun shotLocation(three: Boolean): Pair<Float, Float> = if (three) {
            listOf(0.6f to 2.0f, 14.4f to 2.2f, 3f to 7.2f, 12f to 7.2f, 7.5f to 8.6f).random(random)
        } else {
            listOf(6.8f to 2.1f, 8.3f to 2.8f, 7.2f to 4.3f, 4f to 4.8f, 10.7f to 4.5f).random(random)
        }

        fun fieldGoal(ours: Boolean, allowOffensiveRebound: Boolean = true) {
            val three = random.nextDouble() < 0.34
            val made = random.nextDouble() < if (three) 0.35 else 0.52
            val shooter = if (ours) currentPlayer() else null
            val assist = if (ours && made && random.nextDouble() < 0.65)
                onCourt.filter { it != shooter }.random(random) else null
            val type = when {
                ours && three && made -> EventType.THREE_MADE
                ours && three -> EventType.THREE_MISS
                ours && made -> EventType.TWO_MADE
                ours -> EventType.TWO_MISS
                three && made -> EventType.OPP_THREE_MADE
                three -> EventType.OPP_THREE_MISS
                made -> EventType.OPP_TWO_MADE
                else -> EventType.OPP_TWO_MISS
            }
            emit(type, shooter, assist, if (ours) shotLocation(three) else null)
            if (assist != null) emit(EventType.AST, assist)
            if (!made) {
                if (!ours) {
                    if (random.nextDouble() < 0.16) emit(EventType.BLK, currentPlayer())
                    emit(EventType.REB_DEF, currentPlayer())
                } else if (allowOffensiveRebound && random.nextDouble() < 0.24) {
                    emit(EventType.REB_OFF, currentPlayer())
                    fieldGoal(true, false)
                }
            }
        }

        fun possession(ours: Boolean) {
            when (random.nextInt(100)) {
                in 0..10 -> if (ours) emit(EventType.TOV, currentPlayer())
                    else emit(EventType.STL, currentPlayer())
                in 11..23 -> {
                    if (ours) emit(EventType.OPP_PF)
                    else {
                        val eligible = onCourt.filter { fouls.getValue(it) < 4 }
                        if (eligible.isEmpty()) { fieldGoal(false); return }
                        emit(EventType.PF, eligible.random(random))
                    }
                    val shooter = if (ours) currentPlayer() else null
                    repeat(2) { attempt ->
                        val made = random.nextDouble() < 0.76
                        emit(when {
                            ours && made -> EventType.FT_MADE
                            ours -> EventType.FT_MISS
                            made -> EventType.OPP_FT_MADE
                            else -> EventType.OPP_FT_MISS
                        }, shooter)
                        if (attempt == 1 && !made && !ours) emit(EventType.REB_DEF, currentPlayer())
                    }
                }
                else -> fieldGoal(ours)
            }
        }

        fun play() {
            for (q in 1..4) {
                period = q
                clock = 600
                emit(EventType.PERIOD_START)
                players.shuffled(random).sortedBy { seconds.getValue(it.id).sum() }.take(5)
                    .forEach { emit(EventType.SUB_IN, it.id) }
                var ours = q % 2 == 1
                for (nextClock in 585 downTo 15 step 15) {
                    advance(nextClock)
                    if (clock % 120 == 0) rotate()
                    possession(ours)
                    ours = !ours
                }
                advance(0)
                emit(EventType.PERIOD_END)
                onCourt.toList().forEach { emit(EventType.SUB_OUT, it) }
            }
        }

        fun verify(stored: List<EventEntity>, game: GameEntity) {
            val live = stored.map { it.toDomain() }
            assertEquals(events.size, stored.size)
            assertEquals(teamScore, computeTeamScore(live))
            assertEquals(opponentScore, computeOppScore(live))
            assertEquals(teamScore, game.teamScore)
            assertEquals(opponentScore, game.opponentScore)
            assertTrue(stored.all { it.syncStatus == "PENDING" && it.gameId == game.id })
            assertEquals(12_000, seconds.values.sumOf { it.sum() })
            assertEquals(5 * (teamScore - opponentScore), plusMinus.values.sumOf { it.sum() })
            players.forEach { player ->
                assertEquals(seconds.getValue(player.id).sum(),
                    computeSecondsPlayedForPlayer(player.id, live, 600, 4, 0))
                assertEquals(plusMinus.getValue(player.id).sum(), computePlusMinusForPlayer(player.id, live))
                for (q in 1..4) {
                    assertEquals(seconds.getValue(player.id)[q - 1],
                        computeSecondsPlayedForPlayerInPeriod(player.id, live, q, 600))
                    assertEquals(seconds.getValue(player.id)[q - 1],
                        buildPlayerGameStats(player.id, game, stored, q).secondsPlayed)
                    assertEquals(plusMinus.getValue(player.id)[q - 1],
                        computePlusMinusForPlayer(player.id, live, q))
                }
            }
            val boxes = computeBoxByPlayer(live, 600, 4, 0)
            assertEquals(teamScore, boxes.values.sumOf { it.pts })
            assertTrue(boxes.values.all { it.pf < 5 })
            assertTrue(computeOnCourtIds(live).isEmpty())
        }

        fun report(gameId: Long, name: String): JSONObject {
            val boxes = computeBoxByPlayer(events.mapIndexed { index, e -> e.copy(id = index + 1L).toDomain() }, 600, 4, 0)
            return JSONObject().put("gameId", gameId).put("opponent", name).put("seed", seed)
                .put("eventCount", events.size).put("teamScore", teamScore).put("opponentScore", opponentScore)
                .put("quarterTeamScores", JSONArray(quarterTeam.toList()))
                .put("quarterOpponentScores", JSONArray(quarterOpponent.toList()))
                .put("teamMinutes", 200).put("sumPlayerPlusMinus", plusMinus.values.sumOf { it.sum() })
                .put("eventCounts", JSONObject(events.groupingBy { it.type }.eachCount()))
                .put("players", JSONArray(players.map { player ->
                    val box = boxes.getValue(player.id)
                    JSONObject().put("id", player.id).put("name", player.name).put("number", player.number)
                        .put("seconds", seconds.getValue(player.id).sum())
                        .put("quarterSeconds", JSONArray(seconds.getValue(player.id).toList()))
                        .put("plusMinus", plusMinus.getValue(player.id).sum())
                        .put("quarterPlusMinus", JSONArray(plusMinus.getValue(player.id).toList()))
                        .put("points", box.pts).put("rebounds", box.rebTotal).put("assists", box.ast)
                        .put("steals", box.stl).put("blocks", box.blk).put("turnovers", box.tov).put("fouls", box.pf)
                }))
        }
    }
}
