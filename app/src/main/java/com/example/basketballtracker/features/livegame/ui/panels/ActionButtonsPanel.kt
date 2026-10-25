package com.example.basketballtracker.features.livegame.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.livegame.domain.EventType
import com.example.basketballtracker.features.livegame.domain.LiveEvent
import com.example.basketballtracker.features.livegame.domain.ShotMeta
import com.example.basketballtracker.features.livegame.ui.components.ActionButton
import com.example.basketballtracker.features.summary.ui.pct

@Composable
fun ActionButtonsPanel(
    onEvent: (EventType, ShotMeta?) -> Unit,
    opponentEvents: List<LiveEvent>,
    enabled: Boolean = true,
    modifier: Modifier
) {
    val fgMade = opponentEvents.count {
        it.type == EventType.OPP_TWO_MADE || it.type == EventType.OPP_THREE_MADE
    }

    val fgAttempts = opponentEvents.count {
        it.type == EventType.OPP_TWO_MADE ||
                it.type == EventType.OPP_TWO_MISS ||
                it.type == EventType.OPP_THREE_MADE ||
                it.type == EventType.OPP_THREE_MISS
    }

    val fgPct = pct(fgMade, fgAttempts)

    val threeMade = opponentEvents.count {
        it.type == EventType.OPP_THREE_MADE
    }

    val threeAttempts = opponentEvents.count {
        it.type == EventType.OPP_THREE_MADE || it.type == EventType.OPP_THREE_MISS
    }

    val threePct = pct(threeMade, threeAttempts)

    val ftMade = opponentEvents.count {
        it.type == EventType.OPP_FT_MADE
    }

    val ftAttempts = opponentEvents.count {
        it.type == EventType.OPP_FT_MADE || it.type == EventType.OPP_FT_MISS
    }

    val ftPct = pct(ftMade, ftAttempts)
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Opponent Actions",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn(
                            title = "FG ($fgPct%)",
                            value = "$fgMade/$fgAttempts",
                        )
                        StatColumn(
                            title = "3PT ($threePct%)",
                            value = "$threeMade/$threeAttempts",
                        )
                        StatColumn(
                            title = "FT ($ftPct%)",
                            value = "$ftMade/$ftAttempts",
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("OPP 2PT MADE", EventType.OPP_TWO_MADE, onEvent, true)
                    ActionButton("OPP 2PT MISS", EventType.OPP_TWO_MISS, onEvent, true)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("OPP 3PT MADE", EventType.OPP_THREE_MADE, onEvent, true)
                    ActionButton("OPP 3PT MISS", EventType.OPP_THREE_MISS, onEvent, true)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("OPP FT MADE", EventType.OPP_FT_MADE, onEvent, true)
                    ActionButton("OPP FT MISS", EventType.OPP_FT_MISS, onEvent, true)
                }
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("FT MADE", EventType.FT_MADE, onEvent, enabled)
                    ActionButton("FT MISS", EventType.FT_MISS, onEvent, enabled)
                }
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    ActionButton("AST", EventType.AST, onEvent, enabled)
//                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("REB D", EventType.REB_DEF, onEvent, enabled)
                    ActionButton("REB O", EventType.REB_OFF, onEvent, enabled)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("STL", EventType.STL, onEvent, enabled)
                    ActionButton("BLK", EventType.BLK, onEvent, enabled)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("TOV", EventType.TOV, onEvent, enabled)
                    ActionButton("PF", EventType.PF, onEvent, enabled)
                }
            }
        }
    }
}