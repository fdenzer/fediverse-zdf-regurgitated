package com.example.fediversezdfregurgitated.data.repositories

import com.example.fediversezdfregurgitated.data.local.FilterType
import com.example.fediversezdfregurgitated.data.local.RealmConfig
import com.example.fediversezdfregurgitated.data.local.RealmFilterRule
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId

class FilterRepository(
    private val realmInstance: Realm = RealmConfig.provideRealm(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun addFilter(
        phrase: String,
        type: FilterType,
        contexts: List<String> = listOf("home", "local", "federated", "notifications"), // Default contexts
        isEnabled: Boolean = true
    ): Result<RealmFilterRule> = withContext(dispatcher) {
        if (phrase.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Filter phrase cannot be blank."))
        }
        try {
            lateinit var newRule: RealmFilterRule
            realmInstance.write {
                newRule = copyToRealm(RealmFilterRule().apply {
                    this.phrase = phrase
                    this.filterType = type.name
                    this.filterContexts.clear()
                    this.filterContexts.addAll(contexts)
                    this.isEnabled = isEnabled
                })
            }
            Result.success(newRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFilter(
        id: ObjectId,
        phrase: String? = null,
        type: FilterType? = null,
        contexts: List<String>? = null,
        isEnabled: Boolean? = null
    ): Result<Unit> = withContext(dispatcher) {
        try {
            realmInstance.write {
                val ruleToUpdate = query<RealmFilterRule>("id == $0", id).first().find()
                ruleToUpdate?.let { rule ->
                    phrase?.let { if (it.isNotBlank()) rule.phrase = it }
                    type?.let { rule.filterType = it.name }
                    contexts?.let {
                        rule.filterContexts.clear()
                        rule.filterContexts.addAll(it)
                    }
                    isEnabled?.let { rule.isEnabled = it }
                    copyToRealm(rule, UpdatePolicy.ALL) // Persist changes
                } ?: return@write Result.failure(NoSuchElementException("Filter rule with id $id not found."))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFilter(id: ObjectId): Result<Unit> = withContext(dispatcher) {
        try {
            realmInstance.write {
                val ruleToDelete = query<RealmFilterRule>("id == $0", id).first().find()
                ruleToDelete?.let { delete(it) }
                    ?: return@write Result.failure(NoSuchElementException("Filter rule with id $id not found."))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllFiltersFlow(): Flow<List<RealmFilterRule>> {
        return realmInstance.query<RealmFilterRule>()
            .asFlow()
            .map { it.list.toList() }
    }

    suspend fun getActiveFiltersForContext(context: String): List<RealmFilterRule> = withContext(dispatcher) {
        realmInstance.query<RealmFilterRule>("isEnabled == true AND $0 IN filterContexts", context).find().toList()
    }

    // This method would be used by StatusRepository to check if a status should be filtered
    suspend fun shouldFilterStatus(
        statusContent: String,
        statusAuthorId: String, // Assuming Mastodon account ID is used for user filtering
        statusAuthorUsername: String, // e.g. "user@instance"
        timelineContext: String // e.g., "home", "local"
    ): Boolean = withContext(dispatcher) {
        val activeFilters = getActiveFiltersForContext(timelineContext)
        if (activeFilters.isEmpty()) return@withContext false

        for (filter in activeFilters) {
            when (FilterType.valueOf(filter.filterType)) {
                FilterType.KEYWORD -> {
                    // Basic keyword check, case-insensitive.
                    // TODO: Add whole word matching, regex options later.
                    if (statusContent.contains(filter.phrase, ignoreCase = true)) {
                        return@withContext true
                    }
                }
                FilterType.USER_ID -> {
                    // Assuming filter.phrase for USER_ID stores the Mastodon account ID.
                    // Or, if it stores username@instance, parse accordingly.
                    if (filter.phrase == statusAuthorId || filter.phrase.equals(statusAuthorUsername, ignoreCase = true)) {
                        return@withContext true
                    }
                }
            }
        }
        return@withContext false
    }

    fun close() {
        if (!realmInstance.isClosed()) {
            realmInstance.close()
        }
    }
}
