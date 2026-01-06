package com.example.basketballtracker.features.players.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.basketballtracker.features.players.state.PlayersViewModel

@Composable
fun PlayersScreen(
    vm: PlayersViewModel,
    onBack: () -> Unit
) {
    val players by vm.players.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var numberText by rememberSaveable { mutableStateOf("") }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add player") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = numberText,
                        onValueChange = { input ->
                            // מאפשר רק ספרות
                            numberText = input.filter { it.isDigit() }.take(2)
                        },
                        label = { Text("Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = numberText.toIntOrNull() ?: 0
                        vm.add(name, num)
                        name = ""
                        numberText = ""
                        showAdd = false
                    },
                    enabled = name.trim().isNotEmpty()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdd = false
                }) { Text("Cancel") }
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Roster Management", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onBack) { Text("Back") }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.height(56.dp)
                ) { Text("Add Player") }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(players, key = { it.id }) { p ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                           Box() {
                                    Text(
                                        "#${p.number}     ${p.name}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Box() {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                        IconButton(onClick = { vm.delete(p.id) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Delete")
                                        }
                                        IconButton(onClick = { vm.delete(p.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete"
                                            )
                                        }
                                    }
                                    }
                        }
                        }
                    }
                }
            }
        }
    }
}
