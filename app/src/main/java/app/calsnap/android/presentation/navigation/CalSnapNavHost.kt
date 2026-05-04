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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.calsnap.android.presentation.components.CalSnapSoundEffect
import app.calsnap.android.presentation.components.CalSnapBottomBar
import app.calsnap.android.presentation.components.LocalCalSnapEffects
import app.calsnap.android.presentation.screens.add.AddFoodScreen
import app.calsnap.android.presentation.screens.ai.AiChatScreen
import app.calsnap.android.presentation.screens.home.HomeScreen
import app.calsnap.android.presentation.screens.onboarding.OnboardingScreen
import app.calsnap.android.presentation.screens.progress.ProgressScreen
import app.calsnap.android.presentation.screens.settings.SettingsScreen

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalSnapNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var showAddSheet by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val effects = LocalCalSnapEffects.current

    val showBottomBar = currentRoute in Screen.bottomBarRoutes

    LaunchedEffect(showAddSheet) {
        if (showAddSheet) effects.sound.play(CalSnapSoundEffect.SheetOpen)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                CalSnapBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == Screen.Add.route) {
                            showAddSheet = true
                        } else {
                            navController.navigate(route) {
                                // popUpTo the graph start so the back stack never
                                // grows past a single entry per top-level tab.
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
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
            onAddFood        = { showAddSheet = true },
        )
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                effects.sound.play(CalSnapSoundEffect.SheetClose)
                showAddSheet = false
            },
            sheetState = addSheetState,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            scrimColor = Color.Black.copy(alpha = 0.42f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        ) {
            AddFoodScreen(
                onDismiss = {
                    effects.sound.play(CalSnapSoundEffect.SheetClose)
                    showAddSheet = false
                },
                sheetMode = true,
            )
        }
    }
}

@Composable
private fun CalSnapGraph(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues,
    onAddFood: () -> Unit,
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
            HomeScreen(onAddFood = onAddFood)
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
