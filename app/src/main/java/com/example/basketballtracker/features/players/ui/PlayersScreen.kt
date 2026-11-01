package com.example.basketballtracker.features.players.ui

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.text.style.TextOverflow
import com.example.basketballtracker.ui.theme.SportsColors
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.core.data.db.entities.PlayerEntity
import com.example.basketballtracker.features.newgame.ui.StyledTextField
import com.example.basketballtracker.features.summary.ui.SummaryTopBar
import kotlinx.coroutines.launch

@Composable
fun RosterScreen(
    players: List<PlayerEntity>,
    onPlayerClick: (Long) -> Unit,
    onAddPlayer: (String, Int, String?) -> Unit,
    onEditPlayer: (Long, String, Int, String?) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingPlayerId by rememberSaveable { mutableStateOf<Long?>(null) }
    if (showAddDialog) {
        PlayerEditorDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, number, photoUri ->
                onAddPlayer(name, number, photoUri)
                showAddDialog = false
            }
        )
    }
    players.find { it.id == editingPlayerId }?.let { player ->
        PlayerEditorDialog(
            player = player,
            onDismiss = { editingPlayerId = null },
            onSave = { name, number, photoUri ->
                onEditPlayer(player.id, name, number, photoUri)
                editingPlayerId = null
            }
        )
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SummaryTopBar("ROSTER", "Team directory · ${players.size} players", onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PLAYERS", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = SportsColors.Muted)
            Button(onClick = { showAddDialog = true }, shape = MaterialTheme.shapes.small) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add player")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (players.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(SportsColors.Surface)
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerPhoto(null, "", modifier = Modifier.size(72.dp))
                        Text("Build your roster", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Add your first player to get started.", color = SportsColors.Muted)
                    }
                }
            } else {
                items(players, key = { it.id }) { player ->
                    RosterPlayerCard(
                        player,
                        onClick = { onPlayerClick(player.id) },
                        onEdit = { editingPlayerId = player.id }
                    )
                    HorizontalDivider(color = SportsColors.Line)
                }
            }
        }
    }
}

@Composable
private fun RosterPlayerCard(player: PlayerEntity, onClick: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SportsColors.Surface)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PlayerPhoto(player.photoUri, player.name, modifier = Modifier.size(52.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(player.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = SportsColors.Text,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("JERSEY #${player.number}", style = MaterialTheme.typography.labelSmall,
                color = SportsColors.Blue)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit ${player.name}",
                tint = SportsColors.Blue, modifier = Modifier.size(20.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = SportsColors.Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PlayerEditorDialog(
    player: PlayerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, Int, String?) -> Unit
) {
    var name by rememberSaveable(player?.id) { mutableStateOf(player?.name.orEmpty()) }
    var numberText by rememberSaveable(player?.id) { mutableStateOf(player?.number?.toString().orEmpty()) }
    var photoUri by rememberSaveable(player?.id) { mutableStateOf(player?.photoUri) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var loadingPhoto by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                loadingPhoto = true
                photoError = null
                try {
                    if (loadPlayerPhoto(context, uri) == null) {
                        photoError = "Couldn't read this photo. Please choose another."
                    } else {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        photoUri = uri.toString()
                    }
                } catch (_: SecurityException) {
                    photoError = "Couldn't keep access to this photo. Please choose another."
                } finally {
                    loadingPhoto = false
                }
            }
        }
    }

    val number = numberText.toIntOrNull()
    val canSave = name.isNotBlank() && number != null && !loadingPhoto

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Text(if (player == null) "Add Player" else "Edit Player")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerPhoto(photoUri, name, modifier = Modifier.size(88.dp))
                    TextButton(
                        enabled = !loadingPhoto,
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text(
                            if (loadingPhoto) "Loading photo…"
                            else if (photoUri == null) "Add photo (optional)"
                            else "Change photo"
                        )
                    }
                    if (photoUri != null) {
                        TextButton(
                            enabled = !loadingPhoto,
                            onClick = { photoUri = null; photoError = null }
                        ) { Text("Remove photo") }
                    }
                    photoError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
                StyledTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Player name",
                    placeholder = "Enter player name"
                )
                StyledTextField(
                    value = numberText,
                    onValueChange = { value ->
                        numberText = value.filter { it.isDigit() }
                    },
                    label = "Player number",
                    placeholder = "Enter player number"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(name.trim(), number!!, photoUri)
                }
            ) {
                Text(if (player == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
