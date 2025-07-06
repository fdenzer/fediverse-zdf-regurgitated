package com.example.fediversezdfregurgitated.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fediversezdfregurgitated.data.remote.MastodonApiClient
import kotlinx.coroutines.launch
import social.bigbone.api.entity.Status // For Visibility enum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCreationScreen(
    apiClient: MastodonApiClient,
    onPostSuccessfully: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var spoilerText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var postError by remember { mutableStateOf<String?>(null) }
    var showVisibilitySelector by remember { mutableStateOf(false) }
    var selectedVisibility by remember { mutableStateOf(Status.Visibility.PUBLIC) }

    // TODO: Add states for image attachments, polls, etc.

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current // For Toasts

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Post") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (statusText.isNotBlank()) {
                                isPosting = true
                                postError = null
                                coroutineScope.launch {
                                    val result = apiClient.postStatus(
                                        statusText = statusText,
                                        visibility = selectedVisibility,
                                        spoilerText = spoilerText.takeIf { it.isNotBlank() }
                                        // TODO: Pass media_ids, poll options etc.
                                    )
                                    result.onSuccess {
                                        isPosting = false
                                        // TODO: Clear local draft if applicable
                                        onPostSuccessfully()
                                    }.onFailure {
                                        isPosting = false
                                        postError = "Error posting: ${it.message}"
                                    }
                                }
                            } else {
                                // Show error or disable button
                                postError = "Post content cannot be empty."
                            }
                        },
                        enabled = statusText.isNotBlank() && !isPosting
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Post")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (isPosting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = statusText,
                onValueChange = { statusText = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Takes most space
                maxLines = 15
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = spoilerText,
                onValueChange = { spoilerText = it },
                label = { Text("Content Warning (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row { // Action buttons
                    IconButton(onClick = { /* TODO: Implement image picker */ }) {
                        Icon(Icons.Default.Image, contentDescription = "Add image to post")
                    }
                    IconButton(onClick = { /* TODO: Implement poll creation */ }) {
                        Icon(Icons.Default.Poll, contentDescription = "Add poll to post")
                    }
                    IconButton(onClick = { /* TODO: Show spoiler text field or toggle */ }) {
                        // Content description for spoiler text field is handled by its label
                        Icon(Icons.Default.Warning, contentDescription = "Add content warning")
                    }
                }

                Button(onClick = { showVisibilitySelector = true }) {
                    Text("Visibility: ${selectedVisibility.value.replaceFirstChar { it.uppercase() }}")
                }
            }

            postError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (showVisibilitySelector) {
                VisibilitySelectorDialog(
                    currentVisibility = selectedVisibility,
                    onVisibilitySelected = {
                        selectedVisibility = it
                        showVisibilitySelector = false
                    },
                    onDismiss = { showVisibilitySelector = false }
                )
            }
        }
    }
}

@Composable
fun VisibilitySelectorDialog(
    currentVisibility: Status.Visibility,
    onVisibilitySelected: (Status.Visibility) -> Unit,
    onDismiss: () -> Unit
) {
    val visibilities = listOf(
        Status.Visibility.PUBLIC,
        Status.Visibility.UNLISTED,
        Status.Visibility.PRIVATE, // Followers-only
        Status.Visibility.DIRECT
    )

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Post Visibility", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                visibilities.forEach { visibility ->
                    Button(
                        onClick = { onVisibilitySelected(visibility) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (visibility == currentVisibility) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(visibility.value.replaceFirstChar { it.uppercase() })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
