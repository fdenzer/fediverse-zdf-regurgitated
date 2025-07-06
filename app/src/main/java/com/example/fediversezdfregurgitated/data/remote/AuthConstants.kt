package com.example.fediversezdfregurgitated.data.remote

object AuthConstants {
    const val CLIENT_ID_PLACEHOLDER = "YOUR_CLIENT_ID_PLACEHOLDER"
    const val CLIENT_SECRET_PLACEHOLDER = "YOUR_CLIENT_SECRET_PLACEHOLDER"
    const val REDIRECT_URI_PLACEHOLDER = "myapp://oauth-callback" // Must match what's registered
    const val ZDF_SOCIAL_INSTANCE_URL = "https://zdf.social" // Base URL of the instance

    // Scopes define the permissions your app is requesting
    // Common scopes: read, write, follow, push
    // Adjust these based on your app's needs
    val SCOPES = listOf("read", "write", "follow").joinToString(" ")

    const val OAUTH_AUTH_URL_PATH = "/oauth/authorize"
    const val OAUTH_TOKEN_URL_PATH = "/oauth/token"

    fun getAuthorizationUrl(): String {
        return "$ZDF_SOCIAL_INSTANCE_URL$OAUTH_AUTH_URL_PATH?" +
                "client_id=$CLIENT_ID_PLACEHOLDER&" +
                "redirect_uri=$REDIRECT_URI_PLACEHOLDER&" +
                "response_type=code&" +
                "scope=$SCOPES"
    }
}
