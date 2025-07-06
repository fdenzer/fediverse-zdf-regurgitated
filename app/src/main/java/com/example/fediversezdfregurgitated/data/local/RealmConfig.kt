package com.example.fediversezdfregurgitated.data.local

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object RealmConfig {
    private const val REALM_NAME = "fediverseZDF.realm"

    // Define all RealmObject classes that are part of this schema
    private val schemaClasses = setOf(
        RealmStatus::class,
        RealmDraft::class,
        RealmFilterRule::class // Added FilterRule
        // Add other RealmObject classes here as they are created
        // RealmUserNotification::class,
        // RealmUserAccount::class,
    )

    // Increment schema version when you add new RealmObject classes or modify existing ones
    private const val CURRENT_SCHEMA_VERSION = 2L


    fun provideRealm(): Realm {
        val config = RealmConfiguration.Builder(schema = schemaClasses)
            .name(REALM_NAME)
            .deleteRealmIfMigrationNeeded() // Use during development, implement proper migration for production
            .schemaVersion(CURRENT_SCHEMA_VERSION)
            // .migration(AutomaticSchemaMigration { context ->
            //    // Define migration logic if needed for schema changes
            //    // For example, if you add a new property or class
            //    val oldVersion = context.oldVersion
            //    val newRealm = context.newRealm
            //
            //    if (oldVersion < 2L) {
            //        // If migrating from a version before RealmFilterRule was added,
            //        // no specific action is needed for this class itself unless
            //        // other classes changed related to it.
            //    }
            // })
            .build()
        return Realm.open(config)
    }
}
