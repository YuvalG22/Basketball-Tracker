package com.example.basketballtracker.features.livegame.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.R
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.core.ui.components.CustomFilterChip
import com.example.basketballtracker.features.core.ui.components.LivePanelTabs
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.PlayerBox
import com.example.basketballtracker.features.livegame.domain.ShotMeta
import com.example.basketballtracker.features.livegame.domain.formatMinutes
import com.example.basketballtracker.features.livegame.ui.PeriodFilter
import com.example.basketballtracker.utils.calculateShotDistance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.get
import kotlin.math.sqrt

@Composable
fun ActionsPanel(
    opponentName: String,
    isHomeGame: Boolean,
    box: Map<Long, PlayerBox>,
    players: Map<Long, PlayerEntity>,
    events: List<LiveEvent>,
    selectedId: Long?,
    onEvent: (EventType, ShotMeta?) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier
) {
    var periodFilter by rememberSaveable { mutableStateOf(PeriodFilter.All) }

    val filteredEvents = remember(events, periodFilter) {
        when (periodFilter) {
            PeriodFilter.All -> events
            PeriodFilter.Q1 -> events.filter { it.period == 1 }
            PeriodFilter.Q2 -> events.filter { it.period == 2 }
            PeriodFilter.Q3 -> events.filter { it.period == 3 }
            PeriodFilter.Q4 -> events.filter { it.period == 4 }
            PeriodFilter.OT -> events.filter { it.period >= 5 }
            PeriodFilter.OT1 -> events.filter { it.period == 5 }
            PeriodFilter.OT2 -> events.filter { it.period == 6 }
            PeriodFilter.OT3 -> events.filter { it.period == 7 }
            PeriodFilter.OT4 -> events.filter { it.period == 8 }
        }
    }
    val shots = filteredEvents.mapNotNull { event ->
        if (event.playerId != selectedId) return@mapNotNull null
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
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            var selectedTab by rememberSaveable { mutableStateOf(0) }

            LivePanelTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> PlayerGameStatsCard(
                    playerBox = box[selectedId],
                    player = players[selectedId],
                    onEvent = onEvent,
                    shots = shots
                )

                1 -> PlayByPlayPanel(
                    modifier = Modifier.fillMaxWidth(),
                    opponentName = opponentName,
                    events = events,
                    playersById = players,
                    isHomeGame = isHomeGame,
                    onUndo = onUndo,
                )
            }
        }
    }
}

@Composable
fun HalfCourtClickable(
    onEvent: (EventType, ShotMeta?) -> Unit,
    shots: List<ShotUi> = emptyList(),
) {
    var courtSize by remember { mutableStateOf(IntSize.Zero) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(15f / 14f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF262626)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .onSizeChanged { courtSize = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()

                            if (down.type != PointerType.Stylus) {
                                continue
                            }

                            if (courtSize.width == 0 || courtSize.height == 0) continue

                            val shotMeta = buildShotMeta(down.position, courtSize)

                            val longPress = withTimeoutOrNull(500) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }

                                    if (change == null || !change.pressed) {
                                        return@withTimeoutOrNull false
                                    }
                                }
                            } == null

                            if (longPress) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isThreePointShot(shotMeta.x, shotMeta.y)) {
                                    onEvent(EventType.THREE_MADE, shotMeta)
                                } else {
                                    onEvent(EventType.TWO_MADE, shotMeta)
                                }

                                waitForUpOrCancellation()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isThreePointShot(shotMeta.x, shotMeta.y)) {
                                    onEvent(EventType.THREE_MISS, shotMeta)
                                } else {
                                    onEvent(EventType.TWO_MISS, shotMeta)
                                }
                            }
                        }
                    }
                }
        ) {
            Image(
                painter = painterResource(R.drawable.court_15x14_original_parquet),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )

            Canvas(modifier = Modifier.matchParentSize()) {
                shots.forEach { shot ->
                    val center = Offset(
                        x = (shot.x / 15f) * size.width,
                        y = (shot.y / 14f) * size.height
                    )

                    if (shot.made) {
                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = 10f,
                            center = center,
                            style = Stroke(width = 6f)
                        )
                    } else {
                        val size = 10f

                        drawLine(
                            color = Color.Red,
                            start = Offset(center.x - size, center.y - size),
                            end = Offset(center.x + size, center.y + size),
                            strokeWidth = 6f
                        )

                        drawLine(
                            color = Color.Red,
                            start = Offset(center.x - size, center.y + size),
                            end = Offset(center.x + size, center.y - size),
                            strokeWidth = 6f
                        )
//                        drawCircle(
//                            color = Color.Red,
//                            radius = 10f,
//                            center = center
//                        )
//                        drawCircle(
//                            color = Color.White,
//                            radius = 10f,
//                            center = center,
//                            style = Stroke(width = 2f)
//                        )
                    }
                }
            }
        }
    }
}

fun isThreePointShot(x: Float, y: Float): Boolean {

    // corner three (above straight lines)
    if (y <= 2.99f) {
        return x <= 0.9f || x >= 14.1f
    }

    // hoop center
    val hoopX = 7.5f
    val hoopY = 1.575f

    val dx = x - hoopX
    val dy = y - hoopY

    val distance = sqrt(dx * dx + dy * dy)

    return distance >= 6.75f
}

data class ShotUi(
    val x: Float,
    val y: Float,
    val made: Boolean,
    val isThree: Boolean
)

private fun buildShotMeta(tap: Offset, courtSize: IntSize): ShotMeta {
    val svgX = (tap.x / courtSize.width) * 15f
    val svgY = (tap.y / courtSize.height) * 14f

    return ShotMeta(
        x = svgX,
        y = svgY,
        distance = calculateShotDistance(svgX, svgY)
    )
}

@Composable
fun PlayerGameStatsCard(
    playerBox: PlayerBox?,
    player: PlayerEntity?,
    onEvent: (EventType, ShotMeta?) -> Unit,
    shots: List<ShotUi> = emptyList(),
) {
    val playerName = player?.name ?: ""
    val playerNumber = player?.number ?: ""
    val pts = playerBox?.pts ?: 0
    val ast = playerBox?.ast ?: 0
    val reb = playerBox?.rebTotal ?: 0
    val stl = playerBox?.stl ?: 0
    val blk = playerBox?.blk ?: 0
    val tov = playerBox?.tov ?: 0
    val pf = playerBox?.pf ?: 0
    val fgm = playerBox?.fgm ?: 0
    val fga = playerBox?.fga ?: 0
    val fgpct = playerBox?.fgPct ?: 0
    val threem = playerBox?.threem ?: 0
    val threea = playerBox?.threea ?: 0
    val threePct = playerBox?.threePct ?: 0
    val ftm = playerBox?.ftm ?: 0
    val fta = playerBox?.fta ?: 0
    val ftPct = playerBox?.ftPct ?: 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
    ) {
        if (player == null) return@Card
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "#${playerNumber}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
            Text(
                playerName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                title = "PTS",
                value = pts.toString(),
            )
            StatColumn(
                title = "REB",
                value = reb.toString(),
            )
            StatColumn(
                title = "AST",
                value = ast.toString(),
            )
            StatColumn(
                title = "STL",
                value = stl.toString(),
            )
            StatColumn(
                title = "BLK",
                value = blk.toString(),
            )
            StatColumn(
                title = "TO",
                value = tov.toString(),
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                title = "FG (${fgpct}%)",
                value = "${fgm}/${fga}",
            )
            StatColumn(
                title = "3PT (${threePct}%)",
                value = "${threem}/${threea}",
            )
            StatColumn(
                title = "FT (${ftPct}%)",
                value = "${ftm}/${fta}",
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    HalfCourtClickable(
        onEvent = onEvent,
        shots = shots,
    )
}

@Composable
fun StatColumn(
    title: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PlayerGameSummaryCard(
    playerBox: PlayerBox?,
    player: PlayerEntity?,
    secondsPlayed: Int,
    plusMinus: Int,
    opponentName: String,
    gameDate: Long,
    shots: List<ShotUi> = emptyList()
) {
    val playerName = player?.name ?: ""
    val playerNumber = player?.number ?: ""
    val pts = playerBox?.pts ?: 0
    val ast = playerBox?.ast ?: 0
    val reb = playerBox?.rebTotal ?: 0
    val stl = playerBox?.stl ?: 0
    val blk = playerBox?.blk ?: 0
    val tov = playerBox?.tov ?: 0
    val pf = playerBox?.pf ?: 0
    val fgm = playerBox?.fgm ?: 0
    val fga = playerBox?.fga ?: 0
    val fgpct = playerBox?.fgPct ?: 0
    val threem = playerBox?.threem ?: 0
    val threea = playerBox?.threea ?: 0
    val threePct = playerBox?.threePct ?: 0
    val ftm = playerBox?.ftm ?: 0
    val fta = playerBox?.fta ?: 0
    val ftPct = playerBox?.ftPct ?: 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
    ) {
        if (player == null) return@Card
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "#${playerNumber}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
            Text(
                playerName,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "vs $opponentName",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
            val date = SimpleDateFormat("E, MMM d, yyyy", Locale.ENGLISH)
                .format(Date(gameDate))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                title = "MIN",
                value = formatMinutes(secondsPlayed),
            )
            StatColumn(
                title = "PTS",
                value = pts.toString(),
            )
            StatColumn(
                title = "AST",
                value = ast.toString(),
            )
            StatColumn(
                title = "REB",
                value = reb.toString(),
            )
            StatColumn(
                title = "STL",
                value = stl.toString(),
            )
            StatColumn(
                title = "BLK",
                value = blk.toString(),
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                title = "TO",
                value = tov.toString(),
            )
            StatColumn(
                title = "PF",
                value = pf.toString(),
            )
            StatColumn(
                title = "+/-",
                value = if (plusMinus > 0) "+$plusMinus" else "$plusMinus",
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                title = "FG (${fgpct}%)",
                value = "${fgm}/${fga}",
            )
            StatColumn(
                title = "3PT (${threePct}%)",
                value = "${threem}/${threea}",
            )
            StatColumn(
                title = "FT (${ftPct}%)",
                value = "${ftm}/${fta}",
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    PlayerShotChart(
        shots = shots
    )
}

@Composable
fun PlayerShotChart(
    shots: List<ShotUi> = emptyList()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(15f / 14f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF262626)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(R.drawable.half_court),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )

            Canvas(modifier = Modifier.matchParentSize()) {
                shots.forEach { shot ->
                    val center = Offset(
                        x = (shot.x / 15f) * size.width,
                        y = (shot.y / 14f) * size.height
                    )

                    if (shot.made) {
                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = 7f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 7f,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                    } else {
                        drawCircle(
                            color = Color.Red,
                            radius = 7f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 7f,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }
    }
}