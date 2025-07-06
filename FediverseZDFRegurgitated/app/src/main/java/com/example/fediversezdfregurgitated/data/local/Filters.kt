package com.example.fediversezdfregurgitated.data.local

import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

// Represents a user-defined filter.
// A filter can have multiple keywords and apply to specific contexts (e.g., home, notifications).
class RealmFilterRule : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var phrase: String = "" // The keyword or user ID to filter
    var filterType: String = FilterType.KEYWORD.name // "keyword" or "user"
    var filterContexts: RealmList<String> = io.realm.kotlin.ext.realmListOf() // e.g., "home", "local", "notifications"
    var isEnabled: Boolean = true
    var createdAt: Long = System.currentTimeMillis()

    // Future enhancements:
    // var wholeWord: Boolean = false
    // var caseSensitive: Boolean = false
    // var expiresAt: Long? = null
    // var filterAction: String = FilterAction.HIDE.name // "hide", "warn"
}

enum class FilterType {
    KEYWORD, USER_ID
}

// enum class FilterAction {
// HIDE, // Completely hide the status
// WARN // Show a content warning instead of hiding
// }

// Helper to create default contexts
fun defaultFilterContexts(): RealmList<String> {
    return io.realm.kotlin.ext.realmListOf("home", "local", "federated", "notifications")
}
