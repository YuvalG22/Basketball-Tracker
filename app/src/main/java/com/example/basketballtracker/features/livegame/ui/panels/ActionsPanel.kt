package com.example.basketballtracker.features.livegame.ui.panels

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            Text("Actions", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MadeShotButton("2PT ✓", EventType.TWO_MADE, onEvent, enabled)
                    MadeShotButton("3PT ✓", EventType.THREE_MADE, onEvent, enabled)
                    MadeShotButton("FT ✓", EventType.FT_MADE, onEvent, enabled)
                    MadeShotButton("REB D", EventType.REB_DEF, onEvent, enabled)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MissedShotButton("2PT ✗", EventType.TWO_MISS, onEvent, enabled)
                    MissedShotButton("3PT ✗", EventType.THREE_MISS, onEvent, enabled)
                    MissedShotButton("FT ✗", EventType.FT_MISS, onEvent, enabled)
                    MissedShotButton("REB O", EventType.REB_OFF, onEvent, enabled)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton("AST", EventType.AST, onEvent, enabled)
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
                shape = RoundedCornerShape(50.dp)
            ) { Text("Personal Foul") }
            Spacer(Modifier.height(8.dp))
            if (!enabled) {
                Text(
                    "Select a player to enable actions",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SectionDivider()
            Spacer(Modifier.weight(1f))
            Text("Opponent Actions")
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onEvent(EventType.OPP_TWO_MADE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("2PT") }
                Button(
                    onClick = { onEvent(EventType.OPP_THREE_MADE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("3PT") }
                Button(
                    onClick = { onEvent(EventType.OPP_FT_MADE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("FT") }
                Button(
                    onClick = { onEvent(EventType.OPP_PF) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("PF") }
            }
        }
    }
}