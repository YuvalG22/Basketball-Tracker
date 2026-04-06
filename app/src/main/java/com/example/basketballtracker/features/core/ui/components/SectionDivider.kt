package com.example.basketballtracker.features.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SectionDivider() {
    HorizontalDivider(
        Modifier.padding(vertical = 8.dp),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}