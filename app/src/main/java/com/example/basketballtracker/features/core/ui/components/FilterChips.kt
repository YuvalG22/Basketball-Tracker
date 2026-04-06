package com.example.basketballtracker.features.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults.filterChipColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.stats.state.StatsMode
import com.example.basketballtracker.features.stats.state.formatMode

@Composable
fun FilterChipsRow(
    filters: List<StatsMode>,
    selected: StatsMode,
    onSelect: (StatsMode) -> Unit,
) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            val isSelected = selected == filter
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(formatMode(filter)) },
                colors = filterChipColors(
                    selectedContainerColor = Color.Transparent,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent
                )
            )
        }
    }
}