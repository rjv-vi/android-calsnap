package app.calsnap.android.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
        containerColor = MaterialTheme.colorScheme.background,
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
        enterTransition  = { calSnapEnterTransition() },
        exitTransition   = { calSnapExitTransition() },
        popEnterTransition = { calSnapEnterTransition() },
        popExitTransition  = { calSnapExitTransition() },
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

private fun calSnapEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
        slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it / 18 }

private fun calSnapExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(110, easing = FastOutSlowInEasing)) +
        slideOutVertically(animationSpec = tween(150, easing = FastOutSlowInEasing)) { -it / 24 }
