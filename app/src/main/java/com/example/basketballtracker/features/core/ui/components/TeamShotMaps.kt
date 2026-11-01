package com.example.basketballtracker.features.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.R
import com.example.basketballtracker.core.data.db.entities.EventEntity

private val shotTypes = setOf("TWO_MADE", "TWO_MISS", "THREE_MADE", "THREE_MISS")
private fun EventEntity.made() = type.endsWith("MADE")
@Composable
fun TeamShotMap(events: List<EventEntity>) {
    val attempts = remember(events) { events.filter { it.type in shotTypes && it.syncStatus != "PENDING_DELETE" } }
    val shots = remember(attempts) { attempts.filter { it.shotX != null && it.shotY != null && it.shotX.isFinite() && it.shotY.isFinite() && it.shotX in 0f..15f && it.shotY in 0f..14f } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TEAM SHOOTING", style = MaterialTheme.typography.titleMedium)
        if (shots.isEmpty()) Text("No mapped shots yet")
        if (attempts.size > shots.size) Text("${attempts.size - shots.size} attempts without valid locations", style = MaterialTheme.typography.bodySmall)
        CourtMap(shots, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CourtMap(shots: List<EventEntity>, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("? Made ? ? Missed", style = MaterialTheme.typography.bodySmall)
            Box(Modifier.fillMaxWidth().aspectRatio(15f / 14f)) {
                Image(painterResource(R.drawable.court_15x14_original_parquet), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                Canvas(Modifier.matchParentSize()) {
                    val sx = size.width / 15f
                    val sy = size.height / 14f
                    shots.forEach {
                        val point = Offset(it.shotX!! * sx, it.shotY!! * sy)
                        val r = 4.dp.toPx()
                        if (it.made()) drawCircle(Color(0xFF42BB86), r, point, style = Stroke(2.dp.toPx()))
                        else {
                            drawLine(Color(0xFFE45C5C), point - Offset(r,r), point + Offset(r,r), 2.dp.toPx())
                            drawLine(Color(0xFFE45C5C), point + Offset(-r,r), point + Offset(r,-r), 2.dp.toPx())
                        }
                    }
                }
            }
            Text("SHOT MAP", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun TeamOverviewWithShotMap(events: List<EventEntity>, overview: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { overview() }
                Box(Modifier.weight(1f)) { TeamShotMap(events) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                overview()
                TeamShotMap(events)
            }
        }
    }
}
