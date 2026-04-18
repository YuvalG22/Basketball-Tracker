package com.example.basketballtracker.features.livegame.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.remote.core.serialize.SerializeTags
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.room.util.TableInfo
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.core.ui.components.SectionDivider
import com.example.basketballtracker.features.livegame.domain.PlayerBox
import com.example.basketballtracker.features.livegame.domain.formatMinutes
import com.example.basketballtracker.features.livegame.ui.components.FoulDots
import com.example.basketballtracker.features.livegame.ui.components.StatRow
import com.example.basketballtracker.utils.formatPlayerName

enum class PlayerCardMode {
    ON_COURT,
    BENCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersPanel(
    onCourtPlayers: List<PlayerEntity>,
    benchPlayers: List<PlayerEntity>,
    selectedId: Long?,
    isEnded: Boolean,
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
//    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (sheetPlayerId != null) {
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
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Text(
                    text = "#${p?.number} ${p?.name}",
                    style = MaterialTheme.typography.titleLarge
                )
            },
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
                        PlayerDetailsSheetContent(
                            player = p,
                            box = b,
                            plusMinus = pm,
                            secondsPlayed = secPlayed,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                style = MaterialTheme.typography.titleSmall
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
            Text("Bench", style = MaterialTheme.typography.titleSmall)
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
private fun PlayerDetailsSheetContent(
    player: PlayerEntity?,
    box: PlayerBox?,
    secondsPlayed: Int,
    plusMinus: Int,
    modifier: Modifier = Modifier
) {
    val p = player
    if (p == null) {
        Text("Player not found", modifier = modifier)
        return
    }

    val pts = box?.pts ?: 0
    val reb = box?.rebTotal ?: 0
    val dreb = box?.rebDef ?: 0
    val oreb = box?.rebOff ?: 0
    val ast = box?.ast ?: 0
    val stl = box?.stl ?: 0
    val blk = box?.blk ?: 0
    val tov = box?.tov ?: 0
    val pf = box?.pf ?: 0

    val twofg = box?.let { "${it.twom}/${it.twoa} (${it.twoPct}%)" } ?: "0/0 (0%)"
    val fg = box?.let { "${it.fgm}/${it.fga} (${it.fgPct}%)" } ?: "0/0 (0%)"
    val tp = box?.let { "${it.threem}/${it.threea} (${it.threePct}%)" } ?: "0/0 (0%)"
    val ft = box?.let { "${it.ftm}/${it.fta} (${it.ftPct}%)" } ?: "0/0 (0%)"

    val plusMinusText = if (plusMinus > 0) "+$plusMinus" else plusMinus

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatRow("Minutes", formatMinutes(secondsPlayed))
        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        StatRow("Points", pts.toString())
        StatRow("Free throws", ft)
        StatRow("2 pointers", twofg)
        StatRow("3 pointers", tp)
        StatRow("Field goals", fg)

        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        StatRow("Rebounds", reb.toString())
        StatRow("Defensive rebounds", dreb.toString())
        StatRow("Offensive rebounds", oreb.toString())

        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        StatRow("Assists", ast.toString())
        StatRow("Steals", stl.toString())
        StatRow("Blocks", blk.toString())
        StatRow("Turnovers", tov.toString())
        StatRow("Personal fouls", pf.toString())
        StatRow("+/-", plusMinusText.toString())
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
    val numberWidth = 36.dp
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) Color.White
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedCard(
                        modifier = Modifier.width(numberWidth),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${player.number}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(spacing))

                    Text(
                        text = player.name,
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
                        text = "MIN $minText  •  PTS $pts  •  REB $reb  •  AST $ast",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
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