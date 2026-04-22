package com.example.basketballtracker.features.livegame.ui.panels

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Fill
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.example.basketballtracker.R
import com.example.basketballtracker.features.core.ui.components.SectionDivider
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.ShotMeta
import com.example.basketballtracker.features.livegame.ui.components.ActionButton
import com.example.basketballtracker.features.livegame.ui.components.MadeShotButton
import com.example.basketballtracker.features.livegame.ui.components.MissedShotButton
import com.example.basketballtracker.utils.calculateShotDistance
import kotlin.math.sqrt

@Composable
fun ActionsPanel(
    enabled: Boolean,
    onEvent: (EventType, ShotMeta?) -> Unit,
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
            Text("Actions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "34",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MIN",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "31",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "PTS",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "12",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "REB",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "8",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AST",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "12/24",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FG (50%)",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "3/8",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "3PT (38%)",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "6/6",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FT (100%)",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HalfCourtClickable(
                onEvent = onEvent
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
//            Spacer(Modifier.height(8.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                ActionButton("REB D", EventType.REB_DEF, onEvent, enabled)
//                ActionButton("REB O", EventType.REB_OFF, onEvent, enabled)
//                ActionButton("AST", EventType.AST, onEvent, enabled)
//            }
//            Spacer(Modifier.height(8.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                ActionButton("STL", EventType.STL, onEvent, enabled)
//                ActionButton("BLK", EventType.BLK, onEvent, enabled)
//                ActionButton("TOV", EventType.TOV, onEvent, enabled)
//            }
//            Spacer(Modifier.height(8.dp))
//            Button(
//                onClick = { onEvent(EventType.PF) },
//                enabled = enabled,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(56.dp),
//                shape = RoundedCornerShape(8.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceVariant
//                ),
//            ) {
//                Text(
//                    "Personal Foul",
//                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
//                    style = MaterialTheme.typography.bodyLarge,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//            Spacer(Modifier.weight(1f))
//            Text("Opponent Actions", style = MaterialTheme.typography.titleSmall)
//            Spacer(Modifier.height(8.dp))
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//                ActionButton("2PT", EventType.OPP_TWO_MADE, onEvent, enabled)
//                ActionButton("3PT", EventType.OPP_THREE_MADE, onEvent, enabled)
//                ActionButton("FT", EventType.OPP_FT_MADE, onEvent, enabled)
//                ActionButton("PF", EventType.OPP_PF, onEvent, enabled)
//            }
        }
    }
}

data class ShotPoint(
    val px: Offset,
    val svg: Offset,
    val made: Boolean,
    val isThree: Boolean
)

@Composable
fun HalfCourtClickable(
    onEvent: (EventType, ShotMeta) -> Unit
) {
    var courtSize by remember { mutableStateOf(IntSize.Zero) }
    var shots by remember { mutableStateOf(listOf<ShotPoint>()) }

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

                            shots = shots + ShotPoint(
                                px = tap,
                                svg = svgPoint,
                                made = false,
                                isThree = isThreePoint
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

                            shots = shots + ShotPoint(
                                px = press,
                                svg = svgPoint,
                                made = true,
                                isThree = isThreePoint
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
                    if (shot.made) {
                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = 8f,
                            center = shot.px
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = shot.px,
                            style = Stroke(width = 2f)
                        )
                    } else {
                        drawCircle(
                            color = Color.Red,
                            radius = 8f,
                            center = shot.px,
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = shot.px,
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