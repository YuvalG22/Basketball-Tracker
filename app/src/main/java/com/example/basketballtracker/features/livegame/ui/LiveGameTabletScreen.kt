package com.example.basketballtracker.features.livegame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val box = remember(s.events) { computeBoxByPlayer(s.events) }
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
                GameControlPanel(
                    opponentName = s.opponentName,
                    events = s.events,
                    playersById = playersById,
                    isHomeGame = s.isHomeGame,
                    onUndo = vm::undoLast,
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                        .padding(bottom = 4.dp)
                )
                ActionsPanel(
                    enabled = actionsEnabled,
                    box = box,
                    players = playersById,
                    selectedId = s.selectedPlayerId,
                    events = s.events,
                    onEvent = { type, shotMeta -> vm.addEvent(type, shotMeta = shotMeta) },
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                        .padding(end = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}