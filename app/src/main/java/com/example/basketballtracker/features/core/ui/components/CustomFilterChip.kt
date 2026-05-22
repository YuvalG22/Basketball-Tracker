package com.example.basketballtracker.features.core.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomFilterChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        shape = RoundedCornerShape(999.dp),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF2ECC71),
            selectedLabelColor = Color.Black,
            containerColor = Color(0xFF1F1D1D),
            labelColor = Color(0x80FFFFFF)
        )
    )
}