package com.example.basketballtracker.features.livegame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FoulDots(
    fouls: Int,
    maxFouls: Int = 5,
    dotSize: Dp = 8.dp,
    spacing: Dp = 4.dp,
    activeColor: Color = Color.Red,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.3f)
) {
    val displayFouls = fouls.coerceAtMost(5)
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxFouls) { index ->
            Box(
                modifier = Modifier
                    .size(height = 4.dp, width = 12.dp)
                    .clip(RectangleShape)
                    .background(
                        if (index < displayFouls) activeColor else inactiveColor
                    )
            )
        }
    }
}