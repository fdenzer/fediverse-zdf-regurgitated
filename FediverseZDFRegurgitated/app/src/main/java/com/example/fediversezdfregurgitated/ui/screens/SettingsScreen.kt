package com.example.fediversezdfregurgitated.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fediversezdfregurgitated.data.local.AppTheme
import com.example.fediversezdfregurgitated.data.local.AppSettings
import com.example.fediversezdfregurgitated.data.local.FontSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    appSettings: AppSettings,
    filterRepository: FilterRepository, // Added FilterRepository
    onNavigateBack: () -> Unit,
    onThemeChanged: () -> Unit,
    onFontSizeChanged: () -> Unit,
    onNavigateToManageFilters: () -> Unit // Callback to navigate
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    // Observe changes to trigger recomposition if AppSettings were a StateFlow
    val currentThemeName by remember(appSettings.currentTheme) { mutableStateOf(appSettings.currentTheme.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }) }
    val currentFontSizeName by remember(appSettings.fontSize) { mutableStateOf(appSettings.fontSize.name.lowercase().replaceFirstChar { it.titlecase() }) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

            SettingItem(
                title = "Theme",
                currentValue = currentThemeName,
                onClick = { showThemeDialog = true }
            )

            SettingItem(
                title = "Font Size",
                currentValue = currentFontSizeName,
                onClick = { showFontSizeDialog = true }
            )

            // TODO: Add Filter List Management UI here

            if (showThemeDialog) {
                ThemeChooserDialog(
                    currentTheme = appSettings.currentTheme,
                    onThemeSelected = {
                        appSettings.currentTheme = it
                        showThemeDialog = false
                        onThemeChanged() // Notify MainActivity to recompose with new theme
                    },
                    onDismiss = { showThemeDialog = false }
                )
            }

            if (showFontSizeDialog) {
                FontSizeChooserDialog(
                    currentFontSize = appSettings.fontSize,
                    onFontSizeSelected = {
                        appSettings.fontSize = it
                        showFontSizeDialog = false
                        onFontSizeChanged() // Notify MainActivity
                    },
                    onDismiss = { showFontSizeDialog = false }
                )
            }
        }
    }
}

@Composable
fun SettingItem(title: String, currentValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(currentValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    Divider()
}

@Composable
fun ThemeChooserDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(theme.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FontSizeChooserDialog(
    currentFontSize: FontSize,
    onFontSizeSelected: (FontSize) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Font Size") },
        text = {
            Column {
                FontSize.entries.forEach { size ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onFontSizeSelected(size) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (size == currentFontSize),
                            onClick = { onFontSizeSelected(size) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(size.name.lowercase().replaceFirstChar { it.titlecase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
