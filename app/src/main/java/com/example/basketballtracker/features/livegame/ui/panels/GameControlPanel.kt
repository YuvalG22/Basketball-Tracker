package com.example.basketballtracker.features.livegame.ui.panels

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.R
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.formatEventPBP
import com.example.basketballtracker.features.livegame.ui.EventFilter
import com.example.basketballtracker.utils.formatClock
import com.example.basketballtracker.utils.formatPlayerName
import com.example.basketballtracker.utils.periodLabel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun GameControlPanel(
    opponentName: String,
    events: List<LiveEvent>,
    playersById: Map<Long, PlayerEntity>,
    onUndo: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text("Play By Play", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            var filter by rememberSaveable { mutableStateOf(EventFilter.All) }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayByPlayFilter(
                    selected = filter,
                    onSelected = { filter = it }
                )
                IconButton(
                    onClick = onUndo,
                ) {
                    Icon(
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = "undo event",
                        modifier = Modifier.size(36.dp),
                        painter = painterResource(id = R.drawable.outline_undo_24),
                    )
                }
            }
            val filteredEvents = remember(events, filter) {
                when (filter) {
                    EventFilter.All -> events
                    EventFilter.Score -> events.filter { it.type.isScoreEvent() }
                }
            }
            if (filteredEvents.isEmpty()) {
                Text(
                    "No events yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                PlayByPlayList(
                    events = filteredEvents,
                    playersById = playersById,
                    opponentName = opponentName
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun PlayByPlayList(
    events: List<LiveEvent>,
    playersById: Map<Long, PlayerEntity>,
    opponentName: String
) {
    val listState = rememberLazyListState()
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.scrollToItem(events.lastIndex)
        }
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(events) { index, e ->
            PlayByPlayItem(
                opponentName,
                e,
                playersById,
            )
        }
    }
}

@Composable
fun PlayByPlayItem(
    opponentName: String,
    event: LiveEvent,
    playersById: Map<Long, PlayerEntity>,
) {
    val isOppEvent = event.type.isOpponentEvent()
    val isScoreEvent = event.type.isScoreEvent()
    val playerName = event.playerId?.let { id -> playersById[id]?.name }
    val formattedName = formatPlayerName(playerName)
    val time = formatClock(event.clockSecRemaining)

    if (event.type == EventType.PERIOD_START || event.type == EventType.PERIOD_END) {
        PeriodMarker(event)
        return
    }
    OutlinedCard(
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .heightIn(60.dp),
            verticalAlignment = Alignment.CenterVertically

        ) {
            Box(Modifier.weight(2f), contentAlignment = Alignment.CenterStart) {
                PlayByPlaySide(
                    visible = !isOppEvent,
                    alignEnd = false,
                    primary = formatEventPBP(event.type),
                    secondary = formattedName,
                    isScoreEvent = isScoreEvent,
                    secondaryFaded = true
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PlayByPlayCenter(
                    period = event.period,
                    time = time,
                    isScoreEvent = isScoreEvent,
                    teamScore = event.teamScoreAtEvent,
                    oppScore = event.opponentScoreAtEvent,
                    isOppEvent = isOppEvent
                )
            }
            Box(Modifier.weight(2f), contentAlignment = Alignment.CenterEnd) {
                PlayByPlaySide(
                    visible = isOppEvent,
                    alignEnd = true,
                    primary = opponentName,
                    secondary = formatEventPBP(event.type),
                    isScoreEvent = isScoreEvent,
                    secondaryFaded = true
                )
            }
        }
    }
}

@Composable
private fun PeriodMarker(event: LiveEvent) {
    val text = when (event.type) {
        EventType.PERIOD_START -> "START ${periodLabel(event.period)}"
        EventType.PERIOD_END -> "END ${periodLabel(event.period)}"
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun PlayByPlaySide(
    visible: Boolean,
    alignEnd: Boolean,
    primary: String,
    secondary: String,
    isScoreEvent: Boolean,
    secondaryFaded: Boolean
) {
    if (!visible) return

    val weight = if (isScoreEvent) FontWeight.ExtraBold else FontWeight.Normal
    val fadedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alignEnd) {
            Text(
                primary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (secondaryFaded) fadedColor else LocalContentColor.current,
            )
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (!isScoreEvent) Color.White else Color.Transparent
                )
            ) {
                Text(
                    secondary,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isScoreEvent) Color.White else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White
                )
            ) {
                Text(
                    primary,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                secondary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (secondaryFaded) fadedColor else LocalContentColor.current
            )
        }
    }
}

@Composable
private fun PlayByPlayCenter(
    period: Int,
    time: String,
    isScoreEvent: Boolean,
    teamScore: Int?,
    oppScore: Int?,
    isOppEvent: Boolean
) {
    val fadedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${periodLabel(period)} $time",
            style = MaterialTheme.typography.bodyMedium,
            color = fadedColor,
            textAlign = TextAlign.Center
        )
        if (isScoreEvent) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$teamScore",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOppEvent) fadedColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    " - ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = fadedColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    "$oppScore",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOppEvent) MaterialTheme.colorScheme.onSurface else fadedColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PlayByPlayFilter(
    selected: EventFilter,
    onSelected: (EventFilter) -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = selected == EventFilter.All,
            onClick = { onSelected(EventFilter.All) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                activeContentColor = Color.White,
                activeBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                inactiveBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("All")
        }

        SegmentedButton(
            selected = selected == EventFilter.Score,
            onClick = { onSelected(EventFilter.Score) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                activeContentColor = Color.White,
                activeBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                inactiveBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("Score")
        }
    }
}