package com.example.fediversezdfregurgitated.data.repositories

import com.example.fediversezdfregurgitated.data.local.RealmConfig
import com.example.fediversezdfregurgitated.data.local.RealmStatus
import com.example.fediversezdfregurgitated.data.remote.MastodonApiClient
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import social.bigbone.api.Range
import social.bigbone.api.entity.Status
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class StatusRepository(
    private val apiClient: MastodonApiClient,
    private val realmInstance: Realm = RealmConfig.provideRealm(), // Allow injecting for tests
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        const val TIMELINE_TYPE_LOCAL = "local"
        const val TIMELINE_TYPE_FEDERATED = "federated"
        const val TIMELINE_TYPE_HOME = "home"
    }

    // --- Data Fetching and Caching ---

    suspend fun fetchAndCacheLocalTimeline(range: Range = Range()): Result<Unit> {
        return fetchAndCacheTimeline(TIMELINE_TYPE_LOCAL, range) {
            apiClient.getLocalTimeline(it)
        }
    }

    suspend fun fetchAndCacheFederatedTimeline(range: Range = Range()): Result<Unit> {
        return fetchAndCacheTimeline(TIMELINE_TYPE_FEDERATED, range) {
            apiClient.getFederatedTimeline(it)
        }
    }

    suspend fun fetchAndCacheHomeTimeline(range: Range = Range()): Result<Unit> {
        // Requires OAuth, will be fully implemented later
        return fetchAndCacheTimeline(TIMELINE_TYPE_HOME, range) {
            apiClient.getHomeTimeline(it)
        }
    }

    private suspend fun fetchAndCacheTimeline(
        timelineType: String,
        range: Range,
        fetchFunction: suspend (Range) -> Result<social.bigbone.api.Pageable<Status>>
    ): Result<Unit> = withContext(dispatcher) {
        try {
            val apiResult = fetchFunction(range)
            apiResult.fold(
                onSuccess = { pageableStatuses ->
                    val realmStatuses = pageableStatuses.part.map { apiStatus ->
                        apiStatusToRealmStatus(apiStatus, timelineType)
                    }
                    realmInstance.write {
                        // Using UpdatePolicy.ALL to insert new or update existing statuses
                        realmStatuses.forEach { copyToRealm(it, UpdatePolicy.ALL) }
                    }
                    Result.success(Unit)
                },
                onFailure = {
                    Result.failure(it)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // --- Data Observation ---

    fun getLocalTimelineFlow(): Flow<List<RealmStatus>> {
        return getTimelineFlow(TIMELINE_TYPE_LOCAL)
    }

    fun getFederatedTimelineFlow(): Flow<List<RealmStatus>> {
        return getTimelineFlow(TIMELINE_TYPE_FEDERATED)
    }

    fun getHomeTimelineFlow(): Flow<List<RealmStatus>> {
        return getTimelineFlow(TIMELINE_TYPE_HOME)
    }

    private fun getTimelineFlow(timelineType: String): Flow<List<RealmStatus>> {
        return realmInstance.query<RealmStatus>("timelineType == $0 SORT(createdAt DESC)", timelineType)
            .asFlow()
            .map { resultsChange: ResultsChange<RealmStatus> ->
                resultsChange.list.toList() // Convert RealmResults to a standard List
            }
    }

    // --- Data Conversion ---

    private fun apiStatusToRealmStatus(apiStatus: Status, timelineType: String): RealmStatus {
        val zonedDateTime = ZonedDateTime.parse(apiStatus.createdAt)
        val timestamp = zonedDateTime.toInstant().toEpochMilli()

        // Using apply for cleaner assignment of multiple properties as RealmStatus has no-arg constructor now
        return RealmStatus().apply {
            this.id = apiStatus.id
            this.content = apiStatus.content // Consider sanitizing HTML
            this.authorAvatarUrl = apiStatus.account.avatarStatic
            this.authorDisplayName = apiStatus.account.displayName
            this.authorUsername = "${apiStatus.account.acct}@${getInstanceFromUrl(apiStatus.account.url)}"
            this.createdAt = timestamp
            this.timelineType = timelineType
            this.instanceUrl = getInstanceFromUrl(apiStatus.account.url) ?: "zdf.social"
            this.favourited = apiStatus.isFavourited == true
            this.boosted = apiStatus.isReblogged == true
            this.bookmarked = apiStatus.isBookmarked == true
            this.sensitive = apiStatus.isSensitive
            this.spoilerText = apiStatus.spoilerText
            this.replyCount = apiStatus.repliesCount
            this.boostCount = apiStatus.reblogsCount
            this.favouriteCount = apiStatus.favouritesCount
            // TODO: Map media attachments, polls if added to RealmStatus
        }
    }

    private fun getInstanceFromUrl(url: String?): String? {
        return url?.let {
            try {
                val uri = java.net.URI(it)
                uri.host
            } catch (e: Exception) {
                null // Or log error
            }
        }
    }


    // --- Cache Management (Placeholder - to be expanded) ---

    suspend fun clearOldStatuses(timelineType: String, olderThanMillis: Long) = withContext(dispatcher) {
        realmInstance.write {
            val oldStatuses = query<RealmStatus>("timelineType == $0 AND createdAt < $1", timelineType, olderThanMillis).find()
            delete(oldStatuses)
        }
    }

    suspend fun clearAllStatusesForTimeline(timelineType: String) = withContext(dispatcher) {
        realmInstance.write {
            val statusesToDelete = query<RealmStatus>("timelineType == $0", timelineType).find()
            delete(statusesToDelete)
        }
    }

    // Close Realm when repository is no longer needed (e.g., in ViewModel onCleared)
    fun close() {
        if (!realmInstance.isClosed()) {
            realmInstance.close()
        }
    }
}
