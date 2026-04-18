package com.example.basketballtracker.features.livegame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.livegame.domain.EventType

@Composable
fun RowScope.MissedShotButton(
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
            .weight(1f)
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Outlined.Cancel,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
            )
            Text(
                label,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RowScope.MadeShotButton(
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
            .weight(1f)
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3AB47A))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
            )
            Text(
                label,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
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
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}