package com.example.basketballtracker.features.livegame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.livegame.domain.*
import com.example.basketballtracker.features.livegame.ui.panels.*
import com.example.basketballtracker.utils.formatClock

@Composable
fun LiveRecordingContent(vm: LiveGameViewModel, onEnd: () -> Unit) {
    val s by vm.ui.collectAsState()
    val pending by vm.pendingMadeShot.collectAsState()
    val saving by vm.saving.collectAsState()
    val error by vm.error.collectAsState()
    val box = computeBoxByPlayer(s.events, vm.quarterLengthSec, s.clock.period, s.clock.secRemaining)
    val court = computeOnCourtIds(s.events)
    val players = s.players.associateBy { it.id }
    val selected = players[s.selectedPlayerId]
    val available = !saving && !s.isEnded && pending == null
    val playerEnabled = available && selected != null && (box[selected.id]?.pf ?: 0) < 5
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var substitutes by rememberSaveable { mutableStateOf(false) }
    var outgoing by rememberSaveable { mutableStateOf<Long?>(null) }
    val record: (EventType, ShotMeta?) -> Unit = { type, meta ->
        if (available && (type.isOpponentEvent() || playerEnabled)) vm.addEvent(type, shotMeta = meta)
    }
    val roster: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("On court: ${court.size}/5", Modifier.weight(1f))
                TextButton(onClick = { outgoing = null; substitutes = true }, enabled = available) { Text("Substitute") }
            }
            PlayersPanel(gameDate = s.gameDateEpoch, onCourtPlayers = s.players.filter { it.id in court }, benchPlayers = s.players.filter { it.id !in court },
                selectedId = s.selectedPlayerId, box = box, events = s.events, clock = s.clock, quarterLengthSec = vm.quarterLengthSec, opponentName = s.opponentName,
                plusMinusById = s.plusMinusById, secondsPlayedById = s.secondsPlayedById,
                onSelect = { if (available) vm.selectPlayer(it) },
                onSubIn = { if (available) { outgoing = null; substitutes = true } },
                onSubOut = { if (available) { outgoing = it; substitutes = true } }, modifier = Modifier.weight(1f))
        }
    }
    val buttons: @Composable (Modifier) -> Unit = { modifier ->
        ActionButtonsPanel(record, s.events.filter { it.type.isOpponentEvent() }, playerEnabled, modifier,
            opponentEnabled = available, selectedPlayerName = selected?.let { "#${it.number} ${it.name}" } ?: "Select a player")
    }
    Surface(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            val wide = maxWidth >= 840.dp
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (wide) ScoreBoardPanel(s.gameDateEpoch, s.roundNumber, computeTeamScore(s.events), s.opponentName,
                    computeOppScore(s.events), s.isHomeGame, s.clock, vm::toggleClock, vm::nextQuarter, record,
                    available, s.isEnded, { if (available) onEnd() }, vm::adjustClock, vm::adjustPeriod,
                    computeTeamFoulsThisPeriod(s.events, s.gameId, s.clock.period),
                    computeOpponentFoulsThisPeriod(s.events, s.gameId, s.clock.period), Modifier.fillMaxWidth().heightIn(min = 88.dp))
                else Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Text("Our team ${computeTeamScore(s.events)} - ${computeOppScore(s.events)} ${s.opponentName}", style = MaterialTheme.typography.titleLarge)
                    Text("Period ${s.clock.period} | ${formatClock(s.clock.secRemaining)} | ${if (s.clock.isRunning) "Running" else "Paused"}")
                    Row {
                        TextButton(onClick = vm::toggleClock, enabled = available) { Text(if (s.clock.isRunning) "Pause" else "Start") }
                        TextButton(onClick = vm::nextQuarter, enabled = available && !s.clock.isRunning) { Text("Next period") }
                        var end by remember { mutableStateOf(false) }
                        TextButton(onClick = { end = true }, enabled = available) { Text("End game") }
                        if (end) AlertDialog(onDismissRequest = { end = false }, title = { Text("End game?") },
                            confirmButton = { TextButton(onClick = { end = false; onEnd() }) { Text("End") } },
                            dismissButton = { TextButton(onClick = { end = false }) { Text("Cancel") } })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (wide) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roster(Modifier.weight(.3f).fillMaxHeight())
                    ActionsPanel(s.opponentName, s.isHomeGame, box, players, s.events, s.selectedPlayerId, record, vm::undoLast, Modifier.weight(.4f).fillMaxHeight())
                    buttons(Modifier.weight(.3f).fillMaxHeight().verticalScroll(rememberScrollState()))
                } else {
                    Text(selected?.let { "Selected: #${it.number} ${it.name}" } ?: "Choose a player in Roster")
                    TabRow(tab) { listOf("Record", "Roster", "History").forEachIndexed { index, label ->
                        Tab(tab == index, onClick = { tab = index }, text = { Text(label) })
                    } }
                    when (tab) {
                        0 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            PlayerGameStatsCard(box[s.selectedPlayerId], selected, record, emptyList())
                            buttons(Modifier.fillMaxWidth())
                        }
                        1 -> roster(Modifier.weight(1f))
                        else -> PlayByPlayPanel(s.opponentName, s.events, players, s.isHomeGame, vm::undoLast, Modifier.weight(1f))
                    }
                }
            }
        }
        pending?.let { shot -> AssistPickerDialog(s.players.filter { it.id in shot.eligibleIds },
            { vm.confirmMadeShot(null) }, vm::confirmMadeShot, vm::dismissAssistPicker, saving, error) }
        if (substitutes) AlertDialog(onDismissRequest = { substitutes = false },
            title = { Text(if (outgoing == null && court.size == 5) "Choose outgoing player" else "Choose incoming player") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                if (outgoing == null && court.size == 5) s.players.filter { it.id in court }.forEach { p ->
                    TextButton(onClick = { outgoing = p.id }, enabled = available) { Text("#${p.number} ${p.name}") }
                } else {
                    if (outgoing != null) TextButton(onClick = { vm.subOut(outgoing!!); substitutes = false }, enabled = available) { Text("Remove player (play short)") }
                    val eligible = s.players.filter { it.id !in court && (box[it.id]?.pf ?: 0) < 5 }
                    if (eligible.isEmpty()) Text("No eligible bench players")
                    eligible.forEach { p -> TextButton(onClick = { vm.substitute(outgoing, p.id); substitutes = false }, enabled = available) { Text("Add #${p.number} ${p.name}") } }
                }
            } }, confirmButton = {}, dismissButton = { TextButton(onClick = { substitutes = false }) { Text("Cancel") } })
    }
}
