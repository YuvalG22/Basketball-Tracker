package com.example.basketballtracker.features.newgame.ui

import com.example.basketballtracker.utils.englishTeamName
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.basketballtracker.core.data.db.dao.*
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.core.data.db.entities.RosterEntity
import com.example.basketballtracker.core.data.remote.RetrofitClient.rosterApi
import com.example.basketballtracker.core.data.remote.roster.RosterUploadDto
import com.example.basketballtracker.features.games.data.GamesRepository
import com.example.basketballtracker.features.players.data.PlayersRepository
import com.example.basketballtracker.ui.theme.BasketballTrackerTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class NewGameDraft(val opponent: String, val home: Boolean, val round: Int,
    val playoff: Boolean, val stage: String, val gameNumber: Int?, val playerIds: List<Long>)

@Composable
fun NewGameScreen(defaultQuarterLengthSec: Int, gamesRepo: GamesRepository,
    playersRepo: PlayersRepository, rosterDao: RosterDao, playerDao: PlayerDao,
    gameDao: GameDao, onStart: (Long) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val players by produceState<List<PlayerEntity>?>(null, playersRepo) {
        playersRepo.observePlayers().collect { value = it }
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val draftKey = rememberSaveable { java.util.UUID.randomUUID().toString() }
    var createdId by rememberSaveable { mutableStateOf<Long?>(null) }
    var rosterSaved by rememberSaveable { mutableStateOf(false) }
    val date = rememberSaveable { System.currentTimeMillis() }
    NewGameSetupContent(players, saving, error, createdId != null, onBack) { draft ->
        if (!saving) {
            saving = true
            error = null
            scope.launch {
                try {
                    val id = createdId ?: gamesRepo.createGame(draft.opponent, draft.home,
                        draft.round, date, defaultQuarterLengthSec, draft.playoff,
                        draft.stage.takeIf { draft.playoff }, draft.gameNumber.takeIf { draft.playoff },
                        draft.playerIds, draftKey
                    ).also { createdId = it }
                    onStart(id)
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) {
                    error = "Could not save the game. Your setup is kept. Try again."
                } finally { saving = false }
            }
        }
    }
}

@Composable
fun NewGameSetupContent(players: List<PlayerEntity>?, saving: Boolean = false,
    error: String? = null, setupLocked: Boolean = false,
    onBack: () -> Unit, scheduledGame: com.example.basketballtracker.core.data.db.entities.GameEntity? = null,
    onSubmit: (NewGameDraft) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(if (scheduledGame == null) 0 else 1) }
    var opponent by rememberSaveable { mutableStateOf(scheduledGame?.let { englishTeamName(it.opponentName) } ?: "") }
    var home by rememberSaveable { mutableStateOf(scheduledGame?.isHomeGame ?: false) }
    var playoff by rememberSaveable { mutableStateOf(false) }
    var round by rememberSaveable { mutableStateOf(scheduledGame?.roundNumber?.toString() ?: "") }
    var stage by rememberSaveable { mutableStateOf("SEMI_FINAL") }
    var gameNumber by rememberSaveable { mutableStateOf("1") }
    var selected by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var query by rememberSaveable { mutableStateOf("") }
    val available = players.orEmpty().map { it.id }.toSet()
    val validSelected = selected.filter { it in available }
    val detailsValid = opponent.isNotBlank() && (!playoff || (gameNumber.toIntOrNull() ?: 0) > 0)
    val locked = saving || setupLocked
    val focusManager = LocalFocusManager.current
    val back = { if (!saving) { if (step == 1 && !setupLocked && scheduledGame == null) step = 0 else onBack() } }
    BackHandler { back() }
    Scaffold(modifier = Modifier.fillMaxSize().imePadding(), bottomBar = {
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (step == 1) Text(if (validSelected.size < 5) "Select ${5 - validSelected.size} more players to start"
                    else "${validSelected.size} players ready to play", style = MaterialTheme.typography.bodyMedium)
                Button(enabled = !saving && detailsValid && (step == 0 || (players != null && validSelected.size >= 5)),
                    onClick = {
                        focusManager.clearFocus()
                        if (step == 0) step = 1
                        else onSubmit(NewGameDraft(opponent.trim(), home, round.toIntOrNull() ?: 0,
                            playoff, stage, gameNumber.toIntOrNull(), validSelected))
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (saving) "Starting game…" else if (step == 0) "Continue to roster"
                        else if (error != null) "Retry start game" else "Start game")
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = back, enabled = !saving) { Text(if (step == 1 && !setupLocked && scheduledGame == null) "Back to details" else "Back") }
                Spacer(Modifier.weight(1f))
                Text(if (scheduledGame == null) "STEP ${step + 1} OF 2" else "SELECT ROSTER", style = MaterialTheme.typography.labelMedium)
            }
            Text(if (scheduledGame == null) "New game" else "Scheduled game", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(if (step == 0) "Set the matchup. Then choose your team." else "Choose the players taking the court.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (scheduledGame == null) Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("1  Game details", "2  Select roster").forEachIndexed { index, title ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        color = if (step == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                        Text(title, Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (step == 0) {
                LazyColumn(Modifier.weight(1f).widthIn(max = 720.dp).align(Alignment.CenterHorizontally).testTag("setup-details"),
                    verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { SetupCard("The matchup") {
                        OutlinedTextField(opponent, { opponent = it }, label = { Text("Opponent") }, supportingText = { Text("Required") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true, enabled = !locked, modifier = Modifier.fillMaxWidth())
                        ChoiceRow("Venue", listOf(false to "Away", true to "Home"), home, !locked) { home = it }
                    } }
                    item { SetupCard("Competition") {
                        ChoiceRow("Game type", listOf(false to "Season", true to "Playoff"), playoff, !locked) { playoff = it }
                        if (playoff) {
                            ChoiceRow("Playoff stage", listOf("QUARTER_FINAL" to "Quarter final", "SEMI_FINAL" to "Semi final", "FINAL" to "Final"), stage, !locked) { stage = it }
                            OutlinedTextField(gameNumber, { gameNumber = it.filter(Char::isDigit).take(3) }, label = { Text("Game number") },
                                singleLine = true, enabled = !locked, modifier = Modifier.fillMaxWidth(), isError = (gameNumber.toIntOrNull() ?: 0) < 1,
                                supportingText = { Text("Enter a number greater than zero") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        } else {
                            OutlinedTextField(round, { round = it.filter(Char::isDigit).take(3) }, label = { Text("Round (optional)") },
                                singleLine = true, enabled = !locked, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    } }
                }
            } else {
                val summary: @Composable () -> Unit = {
                    SetupCard("Your matchup") {
                        Text(opponent.trim(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${if (home) "Home" else "Away"} · ${if (playoff) "${stage.lowercase().replace('_', ' ')} · Game $gameNumber" else "Season${if (round.isNotBlank()) " · Round $round" else ""}"}")
                        scheduledGame?.let { Text(com.example.basketballtracker.features.schedule.ui.fixtureDate(it.gameDateEpoch) + " · " + it.season) }
                        if (scheduledGame == null) TextButton(onClick = { step = 0 }, enabled = !locked) { Text("Edit details") }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 792.dp
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (wide) Column(Modifier.width(280.dp)) { summary() }
                        LazyColumn(Modifier.weight(1f).testTag("setup-roster"), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                            if (!wide) item { summary() }
                            item {
                                Text("Select roster", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${validSelected.size} selected · ${players?.size ?: 0} available")
                            }
                            item { OutlinedTextField(query, { query = it }, label = { Text("Search name or jersey number") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                            item { Row {
                                TextButton(onClick = { selected = available.toList() }, enabled = !locked && available.isNotEmpty()) { Text("Select all") }
                                TextButton(onClick = { selected = emptyList() }, enabled = !locked && selected.isNotEmpty()) { Text("Clear selection") }
                            } }
                            when {
                                players == null -> item { Text("Loading players…") }
                                players.isEmpty() -> item { Text("No players yet. Add players from the Players screen, then return to set up a game.") }
                                else -> {
                                    val matching = players.filter { it.name.contains(query.trim(), true) || it.number.toString().contains(query.trim().removePrefix("#")) }
                                    if (matching.isEmpty()) item { Text("No players match your search.") }
                                    items(matching, key = { it.id }) { player ->
                                        val checked = player.id in validSelected
                                        Surface(shape = RoundedCornerShape(14.dp), color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                                            Row(Modifier.fillMaxWidth().toggleable(checked, enabled = !locked, role = Role.Checkbox) {
                                                selected = if (checked) selected - player.id else selected + player.id
                                            }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text("#${player.number}", Modifier.widthIn(min = 48.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                                Checkbox(checked, onCheckedChange = null, enabled = !locked)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun <T> ChoiceRow(label: String, options: List<Pair<T, String>>, selected: T, enabled: Boolean, onSelect: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) -> FilterChip(selected == value, { onSelect(value) }, label = { Text(text) }, enabled = enabled, modifier = Modifier.testTag("choice-$text")) }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, showBackground = true)
@Preview(widthDp = 1100, heightDp = 800, showBackground = true)
@Composable
private fun NewGameSetupPreview() {
    BasketballTrackerTheme { NewGameSetupContent(emptyList(), onBack = {}, onSubmit = {}) }
}
