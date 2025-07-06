package com.example.fediversezdfregurgitated

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.fediversezdfregurgitated.data.local.SecureTokenStorage
import com.example.fediversezdfregurgitated.data.local.AppSettings
import com.example.fediversezdfregurgitated.data.local.SecureTokenStorage
import com.example.fediversezdfregurgitated.data.remote.AuthConstants
import com.example.fediversezdfregurgitated.data.remote.MastodonApiClient
import com.example.fediversezdfregurgitated.data.repositories.StatusRepository
import com.example.fediversezdfregurgitated.ui.AppNavigation
import com.example.fediversezdfregurgitated.ui.theme.FediverseZDFRegurgitatedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    private lateinit var statusRepository: StatusRepository
    private lateinit var apiClient: MastodonApiClient
    private lateinit var tokenStorage: SecureTokenStorage

    private var isLoadingAuth by mutableStateOf(false)
    private var authErrorMessage by mutableStateOf<String?>(null)
    private var settingsTrigger by mutableStateOf(0) // Used to trigger recomposition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenStorage = SecureTokenStorage(applicationContext)
        val currentToken = tokenStorage.getAccessToken()

        apiClient = MastodonApiClient(currentToken)
        statusRepository = StatusRepository(apiClient, (application as FediverseApplication).realm)

        setContent {
            // By reading settingsTrigger, we ensure recomposition when it changes
            val themeAppSettings = remember(settingsTrigger) { appSettings }

            FediverseZDFRegurgitatedTheme(appSettings = themeAppSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        statusRepository = statusRepository,
                        apiClient = apiClient,
                        tokenStorage = tokenStorage,
                        appSettings = themeAppSettings, // Pass the potentially updated AppSettings
                        isLoadingAuth = isLoadingAuth,
                        isInitiallyLoggedIn = tokenStorage.hasAccessToken(),
                        onLoginRequest = ::launchOAuthFlow,
                        onLogoutRequest = ::logout,
                        authErrorMessage = authErrorMessage,
                        onSettingsChanged = { settingsTrigger++ } // Increment to trigger recomposition
                    )
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun launchOAuthFlow() {
        isLoadingAuth = true
        authErrorMessage = null
        val authUrl = AuthConstants.getAuthorizationUrl()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            startActivity(intent)
        } catch (e: Exception) {
            isLoadingAuth = false
            authErrorMessage = "Could not launch browser for login. Please try again."
            e.printStackTrace()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && !isLoadingAuth) { // Avoid processing twice if already loading
            intent.data?.let { uri ->
                if (uri.scheme == AuthConstants.REDIRECT_URI_PLACEHOLDER.split("://")[0] &&
                    uri.host == AuthConstants.REDIRECT_URI_PLACEHOLDER.split("://")[1].split("/")[0]
                ) {
                    uri.getQueryParameter("code")?.let { code ->
                        isLoadingAuth = true
                        authErrorMessage = null
                        lifecycleScope.launch { // Use lifecycleScope directly for UI updates
                            val result = withContext(Dispatchers.IO) {
                                apiClient.exchangeCodeForToken(code)
                            }
                            result.onSuccess { mastodonToken ->
                                tokenStorage.saveAccessToken(mastodonToken.accessToken)
                                apiClient.setAccessToken(mastodonToken.accessToken)
                                isLoadingAuth = false
                                // Navigation will recompose due to isLoggedIn state change
                            }.onFailure { error ->
                                isLoadingAuth = false
                                authErrorMessage = "Failed to get token: ${error.message}"
                                error.printStackTrace()
                            }
                        }
                    } ?: uri.getQueryParameter("error")?.let { error ->
                        isLoadingAuth = false
                        authErrorMessage = "OAuth Error: $error. Description: ${uri.getQueryParameter("error_description")}"
                        println("OAuth Error: $error, Description: ${uri.getQueryParameter("error_description")}")
                    }
                }
            }
        }
    }

    private fun logout() {
        tokenStorage.clearAccessToken()
        apiClient.setAccessToken(null)
        // AppNavigation will recompose and show LoginScreen
        // Optionally clear cached data specific to the user
        lifecycleScope.launch(Dispatchers.IO) {
            statusRepository.clearAllStatusesForTimeline(StatusRepository.TIMELINE_TYPE_HOME)
            // Clear other user specific data if necessary
        }
    }
}

// Preview for LoginScreen (part of AppNavigation's logic)
@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
fun LoginScreenPreview() {
    FediverseZDFRegurgitatedTheme {
        val context = LocalContext.current
        AppNavigation(
            statusRepository = StatusRepository(MastodonApiClient(null), (context.applicationContext as FediverseApplication).realm),
            apiClient = MastodonApiClient(null),
            tokenStorage = SecureTokenStorage(context),
            isLoadingAuth = false,
            isInitiallyLoggedIn = false,
            onLoginRequest = {},
            onLogoutRequest = {},
            authErrorMessage = null
        )
    }
}

// Preview for Main App View (part of AppNavigation's logic)
@Preview(showBackground = true, name = "Main App Preview (Logged In)")
@Composable
fun MainAppScreenPreview() {
    FediverseZDFRegurgitatedTheme {
        val context = LocalContext.current
        // Simulate being logged in for preview
        val tokenStorage = SecureTokenStorage(context)
        tokenStorage.saveAccessToken("fake_token_for_preview")

        AppNavigation(
            statusRepository = StatusRepository(MastodonApiClient("fake_token_for_preview"), (context.applicationContext as FediverseApplication).realm),
            apiClient = MastodonApiClient("fake_token_for_preview"),
            tokenStorage = tokenStorage,
            isLoadingAuth = false,
            isInitiallyLoggedIn = true,
            onLoginRequest = {},
            onLogoutRequest = {},
            authErrorMessage = null
        )
        // Clean up for next preview run if necessary, though EncryptedSharedPrefs might persist
        // tokenStorage.clearAccessToken()
    }
}
