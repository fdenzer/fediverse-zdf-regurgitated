package com.example.fediversezdfregurgitated.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.widget. первыйToast // Unused import, but shows common pattern
import android.widget.Toast // For showing interaction results
import com.example.fediversezdfregurgitated.data.local.RealmStatus
import com.example.fediversezdfregurgitated.data.remote.MastodonApiClient
import com.example.fediversezdfregurgitated.data.repositories.StatusRepository
import com.example.fediversezdfregurgitated.ui.components.TimelineItem
import kotlinx.coroutines.launch

enum class TimelineType {
    HOME, LOCAL, FEDERATED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TimelineScreen(
    statusRepository: StatusRepository,
    timelineType: TimelineType,
    title: String
) {
    val coroutineScope = rememberCoroutineScope()
    val statusesFlow = when (timelineType) {
        TimelineType.HOME -> statusRepository.getHomeTimelineFlow()
        TimelineType.LOCAL -> statusRepository.getLocalTimelineFlow()
        TimelineType.FEDERATED -> statusRepository.getFederatedTimelineFlow()
    }
    val statuses by statusesFlow.collectAsState(initial = emptyList())

    var isRefreshing by remember { mutableStateOf(false) }
    var initialLoadDone by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                when (timelineType) {
                    TimelineType.HOME -> statusRepository.fetchAndCacheHomeTimeline()
                    TimelineType.LOCAL -> statusRepository.fetchAndCacheLocalTimeline()
                    TimelineType.FEDERATED -> statusRepository.fetchAndCacheFederatedTimeline()
                }.also {
                    isRefreshing = false
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (statuses.isEmpty() && !initialLoadDone) {
            isRefreshing = true // Show indicator during initial load
            when (timelineType) {
                TimelineType.HOME -> statusRepository.fetchAndCacheHomeTimeline()
                TimelineType.LOCAL -> statusRepository.fetchAndCacheLocalTimeline()
                TimelineType.FEDERATED -> statusRepository.fetchAndCacheFederatedTimeline()
            }.also {
                isRefreshing = false
                initialLoadDone = true
            }
        } else {
            initialLoadDone = true // Already have data or attempted load
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
                .fillMaxSize()
        ) {
            if (!initialLoadDone && isRefreshing) { // Show centered indicator only on initial load
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (statuses.isEmpty() && initialLoadDone && !isRefreshing) {
                Text("No statuses to display.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(statuses, key = { it.id }) { status ->
                        TimelineItem(status = status)
                    }
                    // TODO: Add item for loading more (infinite scroll)
                }
            }
            PullRefreshIndicator(
                refreshing = isRefreshing && initialLoadDone, // Show pull refresh indicator only after initial load
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
