package com.example.basketballtracker.features.livegame.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.core.ui.components.SectionDivider
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.PlayerBox
import com.example.basketballtracker.features.livegame.domain.formatMinutes
import com.example.basketballtracker.features.livegame.ui.components.FoulDots
import com.example.basketballtracker.ui.theme.inter

enum class PlayerCardMode {
    ON_COURT,
    BENCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersPanel(
    gameDate: Long,
    onCourtPlayers: List<PlayerEntity>,
    benchPlayers: List<PlayerEntity>,
    selectedId: Long?,
    isEnded: Boolean,
    events: List<LiveEvent>,
    opponentName: String,
    box: Map<Long, PlayerBox>,
    plusMinusById: Map<Long, Int>,
    secondsPlayedById: Map<Long, Int>,
    onSelect: (Long) -> Unit,
    onSubIn: (Long) -> Unit,
    onSubOut: (Long) -> Unit,
    modifier: Modifier
) {
    val canSubIn = onCourtPlayers.size < 5

    var sheetPlayerId by rememberSaveable { mutableStateOf<Long?>(null) }
    if (sheetPlayerId != null) {
        val shots = events.mapNotNull { event ->
            if (event.playerId != sheetPlayerId) return@mapNotNull null
            if (!event.type.isShotEvent()) return@mapNotNull null

            val x = event.shotX ?: return@mapNotNull null
            val y = event.shotY ?: return@mapNotNull null

            ShotUi(
                x = x,
                y = y,
                made = event.type == EventType.TWO_MADE || event.type == EventType.THREE_MADE,
                isThree = event.type == EventType.THREE_MADE || event.type == EventType.THREE_MISS
            )
        }
        val pid = sheetPlayerId!!
        val p = (onCourtPlayers + benchPlayers).firstOrNull { it.id == pid }
        val b = box[pid]
        val pm = plusMinusById[pid] ?: 0
        val secPlayed = secondsPlayedById[pid] ?: 0

        AlertDialog(
            onDismissRequest = { sheetPlayerId = null },
            confirmButton = {
                TextButton(onClick = { sheetPlayerId = null }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Box(
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PlayerGameSummaryCard(
                            player = p,
                            playerBox = b,
                            secondsPlayed = secPlayed,
                            plusMinus = pm,
                            opponentName = opponentName,
                            gameDate = gameDate,
                            shots = shots
                        )
                    }
                }
            }
        )
    }

    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                "On Court",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(onCourtPlayers, key = { it.id }) { player ->
                    val isSelected = player.id == selectedId
                    PlayerCard(
                        player = player,
                        playerBoxScore = box[player.id],
                        secondsByPlayer = secondsPlayedById[player.id],
                        isSelected = isSelected,
                        mode = PlayerCardMode.ON_COURT,
                        onSelect = onSelect,
                        onSubstitute = onSubOut,
                        onOpenSheet = { id -> sheetPlayerId = id }
                    )
                }
            }
            SectionDivider()
            Text(
                "Bench",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val sortedBench = benchPlayers.sortedBy { secondsPlayedById[it.id] }.reversed()
                items(sortedBench, key = { it.id }) { player ->
                    val isSel = player.id == selectedId
                    PlayerCard(
                        player = player,
                        playerBoxScore = box[player.id],
                        secondsByPlayer = secondsPlayedById[player.id],
                        isSelected = isSel,
                        mode = PlayerCardMode.BENCH,
                        canSubstitute = canSubIn,
                        onSelect = onSelect,
                        onSubstitute = onSubIn,
                        onOpenSheet = { id -> sheetPlayerId = id }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: PlayerEntity,
    playerBoxScore: PlayerBox?,
    secondsByPlayer: Int?,
    isSelected: Boolean,
    mode: PlayerCardMode,
    canSubstitute: Boolean = true,
    onSelect: (Long) -> Unit,
    onSubstitute: (Long) -> Unit,
    onOpenSheet: (Long) -> Unit
) {
    val pts = playerBoxScore?.pts ?: 0
    val reb = playerBoxScore?.rebTotal ?: 0
    val ast = playerBoxScore?.ast ?: 0
    val pf = playerBoxScore?.pf ?: 0

    val hapticFeedback = LocalHapticFeedback.current
    val numberWidth = 24.dp
    val spacing = 8.dp

    val icon =
        when (mode) {
            PlayerCardMode.ON_COURT -> Icons.Default.ArrowDownward
            PlayerCardMode.BENCH -> Icons.Default.ArrowUpward
        }

    val iconTint =
        when (mode) {
            PlayerCardMode.ON_COURT -> MaterialTheme.colorScheme.error
            PlayerCardMode.BENCH -> if (canSubstitute) Color(0xFF3AB47A) else Color.Transparent
        }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onSelect(player.id) },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSheet(player.id)
                }
            ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White.copy(alpha = 0.03f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.White
            else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (mode == PlayerCardMode.ON_COURT) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .padding(start = 1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF3AB47A))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${player.number}",
                            fontFamily = inter,
                            modifier = Modifier.width(numberWidth),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(spacing))
                        Text(
                            text = player.name,
                            fontFamily = inter,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(spacing))
                        FoulDots(fouls = pf)
                    }

                    Row {
                        Box(modifier = Modifier.width(numberWidth))
                        Spacer(modifier = Modifier.width(spacing))

                        val seconds = secondsByPlayer ?: 0
                        val minText = formatMinutes(seconds)
                        Text(
                            text = "MIN",
                            fontFamily = inter,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = minText,
                            fontFamily = inter,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(spacing))
                        Text(
                            text = "PTS",
                            fontFamily = inter,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pts.toString(),
                            fontFamily = inter,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(spacing))
                        Text(
                            text = "REB",
                            fontFamily = inter,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reb.toString(),
                            fontFamily = inter,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(spacing))
                        Text(
                            text = "AST",
                            fontFamily = inter,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ast.toString(),
                            fontFamily = inter,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        enabled = canSubstitute,
                        onClick = { onSubstitute(player.id) }
                    ) {
                        Icon(
                            imageVector = icon,
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Substitute",
                            tint = iconTint
                        )
                    }
                }
            }
        }
    }
}