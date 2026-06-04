package com.lifedbg.mobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifedbg.mobile.ui.screens.DashboardScreen
import com.lifedbg.mobile.ui.screens.DataManagementScreen
import com.lifedbg.mobile.ui.screens.DeviceSyncScreen
import com.lifedbg.mobile.ui.screens.IntentScreen
import com.lifedbg.mobile.ui.screens.LlmPayloadPreviewScreen
import com.lifedbg.mobile.ui.screens.LlmSettingsScreen
import com.lifedbg.mobile.ui.screens.OnboardingScreen
import com.lifedbg.mobile.ui.screens.PermissionScreen
import com.lifedbg.mobile.ui.screens.PrivacyScreen
import com.lifedbg.mobile.ui.screens.ReviewScreen
import com.lifedbg.mobile.ui.screens.SettingsScreen
import com.lifedbg.mobile.ui.screens.TimelineScreen

@Composable
fun LifeDbgAppRoot() {
    val navController = rememberNavController()
    val bottomDestinations = listOf(
        AppDestination.Dashboard,
        AppDestination.Timeline,
        AppDestination.Review,
        AppDestination.Settings,
    )

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(AppDestination.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = UtilityRoutes.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(UtilityRoutes.ONBOARDING) {
                OnboardingScreen(onStart = { navController.navigate(AppDestination.Dashboard.route) })
            }
            composable(AppDestination.Dashboard.route) {
                DashboardScreen(
                    onOpenPermission = { navController.navigate(UtilityRoutes.PERMISSION) },
                    onOpenPreview = { navController.navigate(UtilityRoutes.LLM_PREVIEW) },
                    onOpenIntent = { navController.navigate(UtilityRoutes.INTENT) },
                )
            }
            composable(AppDestination.Timeline.route) { TimelineScreen() }
            composable(AppDestination.Review.route) { ReviewScreen() }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    onOpenOnboarding = { navController.navigate(UtilityRoutes.ONBOARDING) },
                    onOpenPermission = { navController.navigate(UtilityRoutes.PERMISSION) },
                    onOpenDevices = { navController.navigate(UtilityRoutes.DEVICES) },
                    onOpenLlm = { navController.navigate(UtilityRoutes.LLM_SETTINGS) },
                    onOpenPrivacy = { navController.navigate(UtilityRoutes.PRIVACY) },
                    onOpenData = { navController.navigate(UtilityRoutes.DATA) },
                )
            }
            composable(UtilityRoutes.PERMISSION) { PermissionScreen() }
            composable(UtilityRoutes.DEVICES) { DeviceSyncScreen() }
            composable(UtilityRoutes.LLM_SETTINGS) { LlmSettingsScreen() }
            composable(UtilityRoutes.LLM_PREVIEW) { LlmPayloadPreviewScreen() }
            composable(UtilityRoutes.PRIVACY) { PrivacyScreen() }
            composable(UtilityRoutes.INTENT) { IntentScreen() }
            composable(UtilityRoutes.DATA) { DataManagementScreen() }
        }
    }
}
