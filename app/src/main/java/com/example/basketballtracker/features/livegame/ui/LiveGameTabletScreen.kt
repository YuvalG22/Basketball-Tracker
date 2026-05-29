package com.example.basketballtracker.features.livegame.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.livegame.domain.computeBoxByPlayer
import com.example.basketballtracker.features.livegame.domain.computeOnCourtIds
import com.example.basketballtracker.features.livegame.domain.computeOppScore
import com.example.basketballtracker.features.livegame.domain.computeTeamScore
import com.example.basketballtracker.features.livegame.domain.computeOpponentFoulsThisPeriod
import com.example.basketballtracker.features.livegame.domain.computeTeamFoulsThisPeriod
import com.example.basketballtracker.features.livegame.ui.panels.ActionsPanel
import com.example.basketballtracker.features.livegame.ui.panels.GameControlPanel
import com.example.basketballtracker.features.livegame.ui.panels.PlayersPanel
import com.example.basketballtracker.features.livegame.ui.panels.ScoreBoardPanel

enum class EventFilter { All, Score }

enum class PeriodFilter { All, Q1, Q2, Q3, Q4, OT, OT1, OT2, OT3, OT4 }

@Composable
fun LiveGameTabletScreen(
    vm: LiveGameViewModel,
    onEndGameNavigate: () -> Unit
) {
    val s by vm.ui.collectAsState()
    val pendingMadeShot by vm.pendingMadeShot.collectAsState()
    val box = remember(s.events) {
        computeBoxByPlayer(
            s.events,
            quarterLengthSec = 600,
            s.clock.period,
            s.clock.secRemaining
        )
    }
    val teamScore = remember(s.events) { computeTeamScore(s.events) }
    val opponentScore = remember(s.events) { computeOppScore(s.events) }
    val playersById = remember(s.players) { s.players.associateBy { it.id } }

    val teamFoulsThisQ = remember(s.events, s.clock.period, s.gameId) {
        computeTeamFoulsThisPeriod(
            events = s.events,
            gameId = s.gameId,
            period = s.clock.period
        )
    }
    val opponentFoulsThisQ = remember(s.events, s.clock.period, s.gameId) {
        computeOpponentFoulsThisPeriod(
            events = s.events,
            gameId = s.gameId,
            period = s.clock.period
        )
    }

    val selectedPf = remember(s.selectedPlayerId, box) {
        val id = s.selectedPlayerId ?: return@remember 0
        box[id]?.pf ?: 0
    }

    val actionsEnabled = s.selectedPlayerId != null && !s.isEnded && selectedPf < 5

    val onCourtIds = remember(s.events) { computeOnCourtIds(s.events) }

    val onCourtPlayers = remember(s.players, onCourtIds) {
        s.players.filter { it.id in onCourtIds }
    }
    val benchPlayers = remember(s.players, onCourtIds) {
        s.players.filter { it.id !in onCourtIds }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ScoreBoardPanel(
                gameDateEpoch = s.gameDateEpoch,
                roundNumber = s.roundNumber,
                clock = s.clock,
                teamScore = teamScore,
                opponentName = s.opponentName,
                opponentScore = opponentScore,
                isHomeGame = s.isHomeGame,
                onToggleClock = vm::toggleClock,
                onNextQuarter = vm::nextQuarter,
                onEvent = { type, shotMeta -> vm.addEvent(type, shotMeta = shotMeta) },
                enabled = true,
                isEnded = s.isEnded,
                onEndGame = onEndGameNavigate,
                adjustClock = vm::adjustClock,
                onAdjustPeriod = vm::adjustPeriod,
                homeFouls = teamFoulsThisQ,
                awayFouls = opponentFoulsThisQ,
                modifier = Modifier
                    .weight(0.1f)
                    .fillMaxWidth()
            )
            Row(
                modifier = Modifier.weight(0.9f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayersPanel(
                    gameDate = s.gameDateEpoch,
                    onCourtPlayers = onCourtPlayers,
                    benchPlayers = benchPlayers,
                    selectedId = s.selectedPlayerId,
                    isEnded = s.isEnded,
                    box = box,
                    events = s.events,
                    opponentName = s.opponentName,
                    plusMinusById = s.plusMinusById,
                    secondsPlayedById = s.secondsPlayedById,
                    onSelect = vm::selectPlayer,
                    onSubIn = vm::subIn,
                    onSubOut = vm::subOut,
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                        .padding(start = 4.dp, bottom = 4.dp)
                )
                ActionsPanel(
                    enabled = actionsEnabled && pendingMadeShot == null,
                    box = box,
                    players = playersById,
                    selectedId = s.selectedPlayerId,
                    events = s.events,
                    onEvent = { type, shotMeta -> vm.addEvent(type, shotMeta = shotMeta) },
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .padding(end = 4.dp, bottom = 4.dp)
                )
                GameControlPanel(
                    opponentName = s.opponentName,
                    events = s.events,
                    playersById = playersById,
                    isHomeGame = s.isHomeGame,
                    onUndo = vm::undoLast,
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .padding(bottom = 4.dp)
                )
            }
        }
        if (pendingMadeShot != null) {
            AssistPickerDialog(
                players = onCourtPlayers.filter { it.id != pendingMadeShot!!.playerId },
                onNoAssist = {
                    vm.confirmMadeShot(null)
                },
                onAssistSelected = { playerId ->
                    vm.confirmMadeShot(playerId)
                },
                onDismiss = {
                    vm.dismissAssistPicker()
                }
            )
        }
    }
}

@Composable
fun AssistPickerDialog(
    players: List<PlayerEntity>,
    onNoAssist: () -> Unit,
    onAssistSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = "Assist?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Who passed the ball?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNoAssist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F1D1D),
                        contentColor = Color.White
                    )
                ) {
                    Text("No assist")
                }

                players.forEach { player ->
                    OutlinedButton(
                        onClick = { onAssistSelected(player.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.12f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1F1D1D),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "#${player.number}  ${player.name}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
