package com.shakilpatel.boloindia.distributor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shakilpatel.boloindia.distributor.data.local.PrefsManager
import com.shakilpatel.boloindia.distributor.navigation.NavGraph
import com.shakilpatel.boloindia.distributor.navigation.Screen
import com.shakilpatel.boloindia.distributor.ui.theme.BoloIndiaDistributorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Install splash screen
        installSplashScreen()

        // Enable edge-to-edge
        enableEdgeToEdge()

        // Check if user is logged in
        val startDestination = runBlocking {
            val token = prefsManager.accessToken.first()
            if (token.isNullOrBlank()) Screen.Auth.route else Screen.Dashboard.route
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.1.16:8000/")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                Timber.d("✅ Backend reachable: ${connection.responseCode}")
            } catch (e: Exception) {
                Timber.e(e, "❌ Backend NOT reachable")
            }
        }



        setContent {
            BoloIndiaDistributorTheme {
                MainScreen(startDestination = startDestination)
            }
        }
    }
}

@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if we should show bottom navigation
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Dashboard.route,
        Screen.Assignments.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier  .padding(innerPadding)
            .consumeWindowInsets(innerPadding)) {
            NavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}

// Bottom Navigation Items
data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Dashboard.route,
        icon = Icons.Default.Home,
        label = "Home"
    ),
    BottomNavItem(
        route = Screen.Assignments.route,
        icon = Icons.Default.Assignment,
        label = "Assignments"
    ),
    BottomNavItem(
        route = Screen.Profile.route,
        icon = Icons.Default.Person,
        label = "Profile"
    )
)
