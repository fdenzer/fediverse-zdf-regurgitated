package com.example.fediversezdfregurgitated.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fediversezdfregurgitated.data.local.FilterType
import com.example.fediversezdfregurgitated.data.local.RealmFilterRule
import com.example.fediversezdfregurgitated.data.repositories.FilterRepository
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFiltersScreen(
    filterRepository: FilterRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val filters by filterRepository.getAllFiltersFlow().collectAsState(initial = emptyList())
    var showAddFilterDialog by remember { mutableStateOf(false) }
    var filterToEdit by remember { mutableStateOf<RealmFilterRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Filters") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                filterToEdit = null // Ensure we are adding, not editing
                showAddFilterDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Filter Rule")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (filters.isEmpty()) {
                item {
                    Text(
                        "No filters configured. Tap the '+' button to add one.",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            items(filters, key = { it.id.toHexString() }) { filterRule ->
                FilterListItem(
                    filterRule = filterRule,
                    onToggleEnabled = {
                        coroutineScope.launch {
                            filterRepository.updateFilter(id = filterRule.id, isEnabled = !filterRule.isEnabled)
                        }
                    },
                    onEdit = {
                        filterToEdit = filterRule
                        showAddFilterDialog = true
                    },
                    onDelete = {
                        coroutineScope.launch {
                            filterRepository.deleteFilter(filterRule.id)
                        }
                    }
                )
                Divider()
            }
        }

        if (showAddFilterDialog) {
            AddEditFilterDialog(
                existingFilterRule = filterToEdit,
                onDismiss = { showAddFilterDialog = false },
                onSave = { phrase, type, contexts, isEnabled ->
                    coroutineScope.launch {
                        if (filterToEdit == null) { // Add new
                            filterRepository.addFilter(phrase, type, contexts, isEnabled)
                        } else { // Edit existing
                            filterRepository.updateFilter(filterToEdit!!.id, phrase, type, contexts, isEnabled)
                        }
                        showAddFilterDialog = false
                        filterToEdit = null
                    }
                }
            )
        }
    }
}

@Composable
fun FilterListItem(
    filterRule: RealmFilterRule,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(filterRule.phrase, style = MaterialTheme.typography.titleMedium)
            Text(
                "Type: ${filterRule.filterType.lowercase()}, Contexts: ${filterRule.filterContexts.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(checked = filterRule.isEnabled, onCheckedChange = { onToggleEnabled() })
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, "Edit Filter")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, "Delete Filter")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFilterDialog(
    existingFilterRule: RealmFilterRule?,
    onDismiss: () -> Unit,
    onSave: (phrase: String, type: FilterType, contexts: List<String>, isEnabled: Boolean) -> Unit
) {
    var phrase by remember { mutableStateOf(existingFilterRule?.phrase ?: "") }
    var selectedType by remember { mutableStateOf(existingFilterRule?.let { FilterType.valueOf(it.filterType) } ?: FilterType.KEYWORD) }
    // For simplicity, editing contexts is not granular here, could be improved with checkboxes
    var contextsString by remember { mutableStateOf(existingFilterRule?.filterContexts?.joinToString(", ") ?: "home, local, federated, notifications") }
    var isEnabled by remember { mutableStateOf(existingFilterRule?.isEnabled ?: true) }
    var phraseError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingFilterRule == null) "Add Filter" else "Edit Filter") },
        text = {
            Column {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it; phraseError = null },
                    label = { Text("Keyword or User ID") },
                    isError = phraseError != null,
                    singleLine = true
                )
                phraseError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Spacer(Modifier.height(8.dp))
                Text("Filter Type:", style = MaterialTheme.typography.labelMedium)
                Row {
                    FilterType.entries.forEach { type ->
                        Row(Modifier.clickable { selectedType = type }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                            Text(type.name.lowercase().replaceFirstChar { it.titlecase() })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contextsString,
                    onValueChange = { contextsString = it },
                    label = { Text("Contexts (comma-separated)") },
                    placeholder = {Text("e.g., home, local, notifications")}
                )
                 Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    Text("Enabled")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (phrase.isBlank()) {
                    phraseError = "Phrase cannot be empty"
                } else {
                    val parsedContexts = contextsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(phrase, selectedType, parsedContexts, isEnabled)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
