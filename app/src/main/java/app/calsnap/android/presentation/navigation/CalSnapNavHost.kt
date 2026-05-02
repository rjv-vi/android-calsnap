package app.calsnap.android.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.calsnap.android.presentation.components.CalSnapBottomBar
import app.calsnap.android.presentation.screens.add.AddFoodScreen
import app.calsnap.android.presentation.screens.ai.AiChatScreen
import app.calsnap.android.presentation.screens.home.HomeScreen
import app.calsnap.android.presentation.screens.onboarding.OnboardingScreen
import app.calsnap.android.presentation.screens.progress.ProgressScreen
import app.calsnap.android.presentation.screens.settings.SettingsScreen

@Composable
fun CalSnapNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in Screen.bottomBar.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CalSnapBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // popUpTo the graph start so the back stack never
                            // grows past a single entry per top-level tab.
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        CalSnapGraph(
            navController    = navController,
            startDestination = startDestination,
            contentPadding   = innerPadding,
        )
    }
}

@Composable
private fun CalSnapGraph(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = Modifier.padding(contentPadding),
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(onAddFood = { navController.navigate(Screen.Add.route) })
        }
        composable(Screen.Progress.route) {
            ProgressScreen()
        }
        composable(Screen.Add.route) {
            AddFoodScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Screen.Ai.route) {
            AiChatScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
