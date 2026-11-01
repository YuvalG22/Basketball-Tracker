package com.example.basketballtracker.features.livegame.ui

import com.example.basketballtracker.ui.theme.SportsColors
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
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.computeBoxByPlayer
import com.example.basketballtracker.features.livegame.domain.computeOnCourtIds
import com.example.basketballtracker.features.livegame.domain.computeOppScore
import com.example.basketballtracker.features.livegame.domain.computeTeamScore
import com.example.basketballtracker.features.livegame.domain.computeOpponentFoulsThisPeriod
import com.example.basketballtracker.features.livegame.domain.computeTeamFoulsThisPeriod
import com.example.basketballtracker.features.livegame.ui.panels.ActionButtonsPanel
import com.example.basketballtracker.features.livegame.ui.panels.ActionsPanel
import com.example.basketballtracker.features.livegame.ui.panels.PlayersPanel
import com.example.basketballtracker.features.livegame.ui.panels.ScoreBoardPanel

enum class EventFilter { All, Score }

enum class PeriodFilter { All, Q1, Q2, Q3, Q4, OT, OT1, OT2, OT3, OT4 }

@Composable
fun LiveGameTabletScreen(
    vm: LiveGameViewModel,
    onEndGameNavigate: () -> Unit
) {
    LiveRecordingContent(vm, onEndGameNavigate)
}

@Composable
fun AssistPickerDialog(
    players: List<PlayerEntity>,
    onNoAssist: () -> Unit,
    onAssistSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean = false,
    error: String? = null
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = SportsColors.Background,
        titleContentColor = SportsColors.Text,
        textContentColor = SportsColors.Text,
        shape = RoundedCornerShape(4.dp),
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
                    color = SportsColors.Muted
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNoAssist,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SportsColors.Surface,
                        contentColor = SportsColors.Text
                    )
                ) {
                    Text("No assist")
                }

                players.forEach { player ->
                    OutlinedButton(
                        onClick = { onAssistSelected(player.id) },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(
                            1.dp,
                            SportsColors.Text.copy(alpha = 0.12f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SportsColors.Surface,
                            contentColor = SportsColors.Text
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
        confirmButton = { if (saving) CircularProgressIndicator() },
        dismissButton = { Column { error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel basket") } } }
    )
}
