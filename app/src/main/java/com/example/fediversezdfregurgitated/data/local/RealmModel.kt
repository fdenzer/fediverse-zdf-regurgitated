package com.example.fediversezdfregurgitated.data.local

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

// Represents a simplified Status object for local storage
class RealmStatus : RealmObject {
    @PrimaryKey
    var id: String = "" // Mastodon status ID
    var content: String = ""
    var authorAvatarUrl: String = ""
    var authorDisplayName: String = ""
    var authorUsername: String = "" // e.g., user@instance
    var createdAt: Long = 0L // Store as timestamp
    var timelineType: String = "" // "home", "local", "federated" to distinguish
    var instanceUrl: String = "zdf.social" // To support multi-instance later if needed

    var favourited: Boolean = false
    var boosted: Boolean = false
    var bookmarked: Boolean = false
    var sensitive: Boolean = false
    var spoilerText: String = ""
    var replyCount: Int = 0
    var boostCount: Int = 0
    var favouriteCount: Int = 0
    // Add other fields like media attachments, polls, etc. later

    // Required no-arg constructor
    constructor() {}

    // Full constructor might be too long, consider a builder or apply block
}

// Example for a draft post
class RealmDraft : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId() // Auto-generated local ID
    var content: String = ""
    // Add other fields like attachments, content warnings, poll options etc. later
    var createdAt: Long = System.currentTimeMillis()

    constructor(content: String) {
        this.content = content
    }
    constructor() {} // Required no-arg
}

// TODO: Add other Realm objects as needed, e.g., for Notifications, UserAccounts, Filters etc.
// class RealmUserNotification : RealmObject { ... }
// class RealmUserAccount : RealmObject { ... } // For multi-account support
// class RealmFilter : RealmObject { ... }
