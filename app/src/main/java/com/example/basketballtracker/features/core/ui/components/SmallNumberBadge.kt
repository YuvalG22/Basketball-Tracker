package com.example.basketballtracker.features.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SmallNumberBadge(
    number: Int,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$number",
            color = Color(0xFF2ECC71),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black
        )
    }
}