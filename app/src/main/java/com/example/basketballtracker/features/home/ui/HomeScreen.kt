package com.example.basketballtracker.features.home.ui

import com.example.basketballtracker.utils.englishTeamName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.basketballtracker.core.data.db.entities.GameEntity
import com.example.basketballtracker.core.data.db.entities.GameSeason
import com.example.basketballtracker.ui.theme.SportsColors
import com.example.basketballtracker.utils.formatClock
import com.example.basketballtracker.utils.formatDate

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewGame: () -> Unit,
    onContinue: (Long) -> Unit,
    onGameSummary: (Long) -> Unit,
    onHistory: () -> Unit,
    onSchedule: () -> Unit = {}
) {
    val state by viewModel.home.collectAsStateWithLifecycle()
    HomeContent(state, viewModel.isSyncing, viewModel.syncError, viewModel::manualSync,
        onNewGame, onContinue, onGameSummary, onHistory, onSchedule)
}

@Composable
fun HomeContent(
    state: HomeState,
    isSyncing: Boolean = false,
    syncError: String? = null,
    onSync: () -> Unit,
    onNewGame: () -> Unit,
    onContinue: (Long) -> Unit,
    onGameSummary: (Long) -> Unit,
    onHistory: () -> Unit,
    onSchedule: () -> Unit = {}
) {
    Surface(Modifier.fillMaxSize(), color = SportsColors.Background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 840.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wide) 32.dp else 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AFEKA BASKETBALL", style = MaterialTheme.typography.labelMedium,
                            color = SportsColors.Accent, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                        Text("Game day", style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black, color = SportsColors.Text)
                    }
                    TextButton(onClick = onSync, enabled = !isSyncing) {
                        if (isSyncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSyncing) "Syncing…" else "Sync")
                    }
                }
                OutlinedButton(onClick = onSchedule, modifier = Modifier.fillMaxWidth()) { Text("Team schedule") }
                syncError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Loading games…", color = SportsColors.Muted)
                    }
                } else {
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                GameDayAction(state, onNewGame, onContinue)
                                OtherLiveGames(state.liveGames.drop(1), onContinue)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                SeasonOverview(state)
                                RecentResults(state.recentGames, onGameSummary, onHistory)
                            }
                        }
                    } else {
                        GameDayAction(state, onNewGame, onContinue)
                        OtherLiveGames(state.liveGames.drop(1), onContinue)
                        SeasonOverview(state)
                        RecentResults(state.recentGames, onGameSummary, onHistory)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameDayAction(state: HomeState, onNewGame: () -> Unit, onContinue: (Long) -> Unit) {
    val live = state.liveGames.firstOrNull()
    Card(colors = CardDefaults.cardColors(containerColor = SportsColors.Text), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (live == null) Icons.Default.SportsBasketball else Icons.Default.PlayArrow,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(if (live == null) "SEASON ${GameSeason.CURRENT}" else "GAME IN PROGRESS",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            if (live == null) {
                Text("Ready for tip-off?", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("Choose your opponent and roster, then start recording the game.",
                    color = Color(0xFFCEDAF0), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("vs ${englishTeamName(live.opponentName)}", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(gameDetails(live), color = Color(0xFFCEDAF0))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${live.teamScore} – ${live.opponentScore}", color = Color.White,
                            style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                        Text("AFEKA · ${englishTeamName(live.opponentName)}", color = Color(0xFFCEDAF0),
                            style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Q${live.currentPeriod}\n${formatClock(live.clockSecRemaining)}", color = Color.White,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Button(onClick = { if (live == null) onNewGame() else onContinue(live.id) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportsColors.Accent, contentColor = Color.White)) {
                Icon(if (live == null) Icons.Default.Add else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (live == null) "Start new game" else "Resume game", fontWeight = FontWeight.Bold)
            }
            if (live != null) {
                TextButton(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
                    Text("Start a different game", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OtherLiveGames(games: List<GameEntity>, onContinue: (Long) -> Unit) {
    if (games.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Other unfinished games", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        games.forEach { game ->
            OutlinedCard(onClick = { onContinue(game.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(englishTeamName(game.opponentName), fontWeight = FontWeight.Bold)
                        Text(gameDetails(game), style = MaterialTheme.typography.bodySmall, color = SportsColors.Muted)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume ${englishTeamName(game.opponentName)}", tint = SportsColors.Accent)
                }
            }
        }
    }
}

@Composable
private fun SeasonOverview(state: HomeState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Season ${GameSeason.CURRENT}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().background(SportsColors.Surface, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            SeasonNumber("Played", state.played)
            SeasonNumber("Won", state.wins)
            SeasonNumber("Lost", state.losses)
            if (state.ties > 0) SeasonNumber("Tied", state.ties)
        }
        if (state.played == 0) Text("Your season record will appear after your first completed game.",
            style = MaterialTheme.typography.bodySmall, color = SportsColors.Muted)
    }
}

@Composable
private fun SeasonNumber(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = SportsColors.Muted)
    }
}

@Composable
private fun RecentResults(games: List<GameEntity>, onGameSummary: (Long) -> Unit, onHistory: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Recent results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onHistory) { Text("All games"); Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
        if (games.isEmpty()) {
            Text("No results yet. Finished games and box scores will appear here.", color = SportsColors.Muted)
        }
        games.forEach { game ->
            val won = game.teamScore > game.opponentScore
            val tied = game.teamScore == game.opponentScore
            val color = if (tied) SportsColors.Muted else if (won) SportsColors.Green else SportsColors.Red
            OutlinedCard(onClick = { onGameSummary(game.id) }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, SportsColors.Line),
                colors = CardDefaults.outlinedCardColors(containerColor = SportsColors.Surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (tied) "TIE" else if (won) "WIN" else "LOSS", color = color,
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(englishTeamName(game.opponentName), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${game.teamScore} – ${game.opponentScore}", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                    }
                    Text(gameDetails(game), style = MaterialTheme.typography.bodySmall, color = SportsColors.Muted)
                    Text("View box score", style = MaterialTheme.typography.labelMedium, color = SportsColors.Accent)
                }
            }
        }
    }
}

private fun gameDetails(game: GameEntity): String {
    val round = if (game.isPlayoff) "${game.playoffStage?.lowercase()?.replace('_', ' ') ?: "Playoff"} · Game ${game.playoffGameNumber ?: game.roundNumber}"
        else "Round ${game.roundNumber}"
    return "${formatDate(game.gameDateEpoch, "d MMM")} · ${game.season} · ${if (game.isHomeGame) "Home" else "Away"} · $round"
}
