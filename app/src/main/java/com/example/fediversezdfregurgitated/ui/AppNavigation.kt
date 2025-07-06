package com.example.fediversezdfregurgitated.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Import all filled icons needed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.fediversezdfregurgitated.data.local.AppSettings
import com.example.fediversezdfregurgitated.data.local.SecureTokenStorage
import com.example.fediversezdfregurgitated.data.remote.AuthConstants
import com.example.fediversezdfregurgitated.data.remote.MastodonApiClient
import com.example.fediversezdfregurgitated.data.repositories.FilterRepository
import com.example.fediversezdfregurgitated.data.repositories.StatusRepository
import com.example.fediversezdfregurgitated.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object HomeTimeline : Screen("home", "Home", Icons.Filled.Home)
    object LocalTimeline : Screen("local", "Local", Icons.Filled.DynamicFeed)
    object FederatedTimeline : Screen("federated", "Federated", Icons.Filled.Public)
    object CreatePost : Screen("create_post", "New Post", Icons.Filled.AddCircle)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object ManageFilters : Screen("manage_filters", "Manage Filters")
}

val bottomNavItems = listOf(
    Screen.HomeTimeline,
    Screen.LocalTimeline,
    Screen.FederatedTimeline,
    Screen.Settings
)

@Composable
fun AppNavigation(
    statusRepository: StatusRepository,
    apiClient: MastodonApiClient,
    tokenStorage: SecureTokenStorage,
    appSettings: AppSettings,
    filterRepository: FilterRepository,
    isLoadingAuth: Boolean,
    isInitiallyLoggedIn: Boolean,
    onLoginRequest: () -> Unit,
    onLogoutRequest: () -> Unit,
    authErrorMessage: String?,
    onSettingsChanged: () -> Unit
) {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(isInitiallyLoggedIn) }

    LaunchedEffect(tokenStorage, apiClient.hasAccessToken()) {
        isLoggedIn = tokenStorage.hasAccessToken() && apiClient.hasAccessToken()
    }

    if (!isLoggedIn) {
        LoginScreen(
            isLoading = isLoadingAuth,
            onLoginClick = onLoginRequest,
            errorMessage = authErrorMessage
        )
    } else {
        Scaffold(
            bottomBar = {
                AppBottomNavigationBar(navController = navController, onLogoutClick = onLogoutRequest)
            },
            floatingActionButton = {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                if (currentRoute in listOf(Screen.HomeTimeline.route, Screen.LocalTimeline.route, Screen.FederatedTimeline.route)) {
                    FloatingActionButton(onClick = { navController.navigate(Screen.CreatePost.route) }) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Create New Post") // Corrected contentDescription
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.HomeTimeline.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.HomeTimeline.route) {
                    TimelineScreen(apiClient, statusRepository, TimelineType.HOME, "Home")
                }
                composable(Screen.LocalTimeline.route) {
                    TimelineScreen(apiClient, statusRepository, TimelineType.LOCAL, "Local (${apiClient.getInstanceUrl()})")
                }
                composable(Screen.FederatedTimeline.route) {
                    TimelineScreen(apiClient, statusRepository, TimelineType.FEDERATED, "Federated")
                }
                composable(Screen.CreatePost.route) {
                    PostCreationScreen(
                        apiClient = apiClient,
                        onPostSuccessfully = { navController.popBackStack() },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        appSettings = appSettings,
                        filterRepository = filterRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onThemeChanged = onSettingsChanged,
                        onFontSizeChanged = onSettingsChanged,
                        onNavigateToManageFilters = { navController.navigate(Screen.ManageFilters.route) }
                    )
                }
                composable(Screen.ManageFilters.route) {
                    ManageFiltersScreen(
                        filterRepository = filterRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

fun MastodonApiClient.getInstanceUrl(): String {
    return AuthConstants.ZDF_SOCIAL_INSTANCE_URL.replace("https://", "")
}

@Composable
fun AppBottomNavigationBar(navController: NavController, onLogoutClick: () -> Unit) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Logout, contentDescription = "Logout") },
            label = { Text("Logout") },
            selected = false,
            onClick = onLogoutClick
        )
    }
}
