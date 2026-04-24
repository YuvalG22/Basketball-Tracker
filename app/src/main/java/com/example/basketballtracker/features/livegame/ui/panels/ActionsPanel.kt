package com.example.basketballtracker.features.livegame.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.R
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.PlayerBox
import com.example.basketballtracker.features.livegame.domain.ShotMeta
import com.example.basketballtracker.features.livegame.ui.components.ActionButton
import com.example.basketballtracker.utils.calculateShotDistance
import kotlin.collections.get
import kotlin.math.sqrt

@Composable
fun ActionsPanel(
    enabled: Boolean,
    box: Map<Long, PlayerBox>,
    events: List<LiveEvent>,
    selectedId: Long?,
    onEvent: (EventType, ShotMeta?) -> Unit,
    modifier: Modifier
) {
    val shots = events.mapNotNull { event ->
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
                .padding(12.dp)
        ) {
            Text("Actions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            PlayerGameStatsCard(
                playerBox = box[selectedId],
                onEvent = onEvent,
                shots = shots
            )
//            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
//                Text(
//                    "2 POINTS",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = Color.White.copy(alpha = 0.5f)
//                )
//                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    MadeShotButton("MADE", EventType.TWO_MADE, onEvent, enabled)
//                    MissedShotButton("MISS", EventType.TWO_MISS, onEvent, enabled)
//                }
//                Spacer(Modifier.height(8.dp))
//                Text(
//                    "3 POINTS",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = Color.White.copy(alpha = 0.5f)
//                )
//                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    MadeShotButton("MADE", EventType.THREE_MADE, onEvent, enabled)
//                    MissedShotButton("MISS", EventType.THREE_MISS, onEvent, enabled)
//                }
//                Text(
//                    "FREE THROWS",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = Color.White.copy(alpha = 0.5f)
//                )
//                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    MadeShotButton("MADE", EventType.FT_MADE, onEvent, enabled)
//                    MissedShotButton("MISS", EventType.FT_MISS, onEvent, enabled)
//                }
//            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton("REB D", EventType.REB_DEF, onEvent, enabled)
                        ActionButton("REB O", EventType.REB_OFF, onEvent, enabled)
                        ActionButton("AST", EventType.AST, onEvent, enabled)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton("STL", EventType.STL, onEvent, enabled)
                        ActionButton("BLK", EventType.BLK, onEvent, enabled)
                        ActionButton("TOV", EventType.TOV, onEvent, enabled)
                    }
                }
                Button(
                    onClick = { onEvent(EventType.PF, null) },
                    enabled = enabled,
                    modifier = Modifier
                        .height(104.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "PF",
                        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Opponent Actions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("2PT", EventType.OPP_TWO_MADE, onEvent, enabled)
                ActionButton("3PT", EventType.OPP_THREE_MADE, onEvent, enabled)
                ActionButton("FT", EventType.OPP_FT_MADE, onEvent, enabled)
                ActionButton("PF", EventType.OPP_PF, onEvent, enabled)
            }
        }
    }
}

@Composable
fun HalfCourtClickable(
    onEvent: (EventType, ShotMeta?) -> Unit,
    shots: List<ShotUi> = emptyList()
) {
    var courtSize by remember { mutableStateOf(IntSize.Zero) }

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
                    detectTapGestures(
                        onTap = { tap ->
                            if (courtSize.width == 0 || courtSize.height == 0) return@detectTapGestures

                            val svgPoint = Offset(
                                x = (tap.x / courtSize.width) * 15f,
                                y = (tap.y / courtSize.height) * 14f
                            )

                            val isThreePoint = isThreePointShot(svgPoint.x, svgPoint.y)

                            val shotMeta = ShotMeta(
                                x = svgPoint.x,
                                y = svgPoint.y,
                                distance = calculateShotDistance(svgPoint.x, svgPoint.y)
                            )
                            if (isThreePoint) {
                                onEvent(EventType.THREE_MISS, shotMeta)
                            } else {
                                onEvent(EventType.TWO_MISS, shotMeta)
                            }
                        },
                        onLongPress = { press ->
                            if (courtSize.width == 0 || courtSize.height == 0) return@detectTapGestures

                            val svgPoint = Offset(
                                x = (press.x / courtSize.width) * 15f,
                                y = (press.y / courtSize.height) * 14f
                            )

                            val isThreePoint = isThreePointShot(svgPoint.x, svgPoint.y)

                            val shotMeta = ShotMeta(
                                x = svgPoint.x,
                                y = svgPoint.y,
                                distance = calculateShotDistance(svgPoint.x, svgPoint.y)
                            )
                            if (isThreePoint) {
                                onEvent(EventType.THREE_MADE, shotMeta)
                            } else {
                                onEvent(EventType.TWO_MADE, shotMeta)
                            }
                        }
                    )
                }
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
                            radius = 8f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                    } else {
                        drawCircle(
                            color = Color.Red,
                            radius = 8f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = center,
                            style = Stroke(width = 2f)
                        )
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

@Composable
fun PlayerGameStatsCard(
    playerBox: PlayerBox?,
    onEvent: (EventType, ShotMeta?) -> Unit,
    shots: List<ShotUi> = emptyList()
) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
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
            StatColumn(
                title = "TO",
                value = tov.toString(),
            )
            StatColumn(
                title = "PF",
                value = pf.toString(),
            )
        }
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
    HalfCourtClickable(
        onEvent = onEvent,
        shots = shots
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
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}