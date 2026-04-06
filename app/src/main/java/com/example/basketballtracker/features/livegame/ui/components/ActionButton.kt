package com.example.basketballtracker.features.livegame.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.livegame.domain.EventType

@Composable
fun MissedShotButton(
    label: String,
    type: EventType,
    onEvent: (EventType) -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = { onEvent(type) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(0.dp, 50.dp, 50.dp, 0.dp)
    ) { Text(label) }
}

@Composable
fun MadeShotButton(label: String, type: EventType, onEvent: (EventType) -> Unit, enabled: Boolean) {
    Button(
        onClick = { onEvent(type) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50.dp, 0.dp, 0.dp, 50.dp)
    ) { Text(label) }
}

@Composable
fun RowScope.ActionButton(
    label: String,
    type: EventType,
    onEvent: (EventType) -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = { onEvent(type) },
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .height(56.dp),
        shape = RoundedCornerShape(50.dp)
    ) { Text(text = label) }
}