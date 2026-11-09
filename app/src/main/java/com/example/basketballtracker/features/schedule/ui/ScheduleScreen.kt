package com.example.basketballtracker.features.schedule.ui

import com.example.basketballtracker.utils.englishTeamName
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.basketballtracker.core.data.db.entities.*
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.players.data.PlayersRepository
import com.example.basketballtracker.features.newgame.ui.NewGameSetupContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun fixtureDate(epoch: Long): String = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).apply {
    timeZone = TimeZone.getTimeZone("Asia/Jerusalem")
}.format(Date(epoch))

@Composable
fun ScheduleScreen(repo: GamesRepository, onSelectRoster: (Long) -> Unit,
    onResume: (Long) -> Unit, onSummary: (Long) -> Unit, onBack: () -> Unit) {
    val all by repo.observeGames().collectAsStateWithLifecycle(initialValue = null)
    val fixtures = all.orEmpty().filter { !it.isDeleted && it.season == GameSeason.CURRENT && it.syncKey.startsWith("easycoach:") }
        .sortedWith(compareBy<GameEntity> { it.gameDateEpoch }.thenBy { it.roundNumber })
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Team schedule · ${GameSeason.CURRENT}", style = MaterialTheme.typography.headlineMedium)
        when {
            all == null -> CircularProgressIndicator()
            fixtures.isEmpty() -> Text("No scheduled games yet. Sync to load your team schedule.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                items(fixtures, key = { it.id }) { game ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(englishTeamName(game.opponentName), style = MaterialTheme.typography.titleMedium)
                            Text("${fixtureDate(game.gameDateEpoch)} · Round ${game.roundNumber} · ${if (game.isHomeGame) "Home" else "Away"}")
                            Text(when (game.status) { GameStatus.SCHEDULED -> "Scheduled"; GameStatus.LIVE -> "Live"; else -> "Finished" })
                            Button(onClick = { when (game.status) {
                                GameStatus.SCHEDULED -> onSelectRoster(game.id)
                                GameStatus.LIVE -> onResume(game.id)
                                else -> onSummary(game.id)
                            } }) { Text(when (game.status) {
                                GameStatus.SCHEDULED -> "Select roster"; GameStatus.LIVE -> "Resume"; else -> "View summary"
                            }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduledRosterScreen(gameId: Long, repo: GamesRepository, playersRepo: PlayersRepository,
    quarterLengthSec: Int, onStart: (Long) -> Unit, onBack: () -> Unit) {
    val game by repo.observeGame(gameId).collectAsStateWithLifecycle(initialValue = null)
    val players by playersRepo.observePlayers().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val fixture = game
    if (fixture == null || fixture.isDeleted) {
        Column(Modifier.padding(24.dp)) {
            Text("Game unavailable")
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    if (fixture.status != GameStatus.SCHEDULED) {
        Column(Modifier.padding(24.dp)) {
            Text(if (fixture.status == GameStatus.LIVE) "This game is already in progress" else "This game has finished")
            if (fixture.status == GameStatus.LIVE) Button(onClick = { onStart(gameId) }) { Text("Resume") }
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    NewGameSetupContent(players, saving = saving, error = error, onBack = onBack, scheduledGame = fixture) { draft ->
        if (!saving) {
            saving = true
            error = null
            scope.launch {
                try { onStart(repo.startScheduledGame(gameId, draft.playerIds, quarterLengthSec)) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { error = "Could not start the game. Your selection is kept. Try again." }
                finally { saving = false }
            }
        }
    }
}
