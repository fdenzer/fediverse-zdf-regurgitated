package com.example.fediversezdfregurgitated

import android.app.Application
import com.example.fediversezdfregurgitated.data.local.RealmConfig
import io.realm.kotlin.Realm

// Custom Application class to initialize Realm (and other global singletons if needed)
class FediverseApplication : Application() {

    lateinit var realm: Realm
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize Realm. This should be done once when the application starts.
        // In a more complex app, you might use a dependency injection framework
        // to manage the Realm instance.
        realm = RealmConfig.provideRealm()

        // You can also initialize other app-wide singletons here
        // For example, initializing a global instance of MastodonApiClient or StatusRepository
        // if not using DI. However, for better testability and lifecycle management,
        // providing these via DI or ViewModelFactories is generally preferred.
    }

    override fun onTerminate() {
        super.onTerminate()
        if (!realm.isClosed()) {
            realm.close()
        }
    }
}
