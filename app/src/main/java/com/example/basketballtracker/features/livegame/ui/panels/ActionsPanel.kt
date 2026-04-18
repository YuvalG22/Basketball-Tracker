package com.example.basketballtracker.features.livegame.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.example.basketballtracker.features.core.ui.components.SectionDivider
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.ui.components.ActionButton
import com.example.basketballtracker.features.livegame.ui.components.MadeShotButton
import com.example.basketballtracker.features.livegame.ui.components.MissedShotButton

@Composable
fun ActionsPanel(
    enabled: Boolean,
    onEvent: (EventType) -> Unit,
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
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "2 POINTS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MadeShotButton("MADE", EventType.TWO_MADE, onEvent, enabled)
                    MissedShotButton("MISS", EventType.TWO_MISS, onEvent, enabled)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "3 POINTS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MadeShotButton("MADE", EventType.THREE_MADE, onEvent, enabled)
                    MissedShotButton("MISS", EventType.THREE_MISS, onEvent, enabled)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "FREE THROWS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MadeShotButton("MADE", EventType.FT_MADE, onEvent, enabled)
                    MissedShotButton("MISS", EventType.FT_MISS, onEvent, enabled)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton("REB D", EventType.REB_DEF, onEvent, enabled)
                ActionButton("REB O", EventType.REB_OFF, onEvent, enabled)
                ActionButton("AST", EventType.AST, onEvent, enabled)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton("STL", EventType.STL, onEvent, enabled)
                ActionButton("BLK", EventType.BLK, onEvent, enabled)
                ActionButton("TOV", EventType.TOV, onEvent, enabled)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onEvent(EventType.PF) },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
            ) {
                Text(
                    "Personal Foul",
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            Text("Opponent Actions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("2PT", EventType.OPP_TWO_MADE, onEvent, enabled)
                ActionButton("3PT", EventType.OPP_THREE_MADE, onEvent, enabled)
                ActionButton("FT", EventType.OPP_FT_MADE, onEvent, enabled)
                ActionButton("PF", EventType.OPP_PF, onEvent, enabled)
            }
        }
    }
}