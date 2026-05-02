package app.calsnap.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.calsnap.android.data.repository.UserRepository
import app.calsnap.android.presentation.navigation.CalSnapNavHost
import app.calsnap.android.presentation.navigation.Screen
import app.calsnap.android.ui.theme.CalSnapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Singleton
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // v1 stub — real gate will come from UserRepository (onboarding completed?).
    @Inject lateinit var appState: AppBootState

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash visible while we resolve "is onboarding done?" from
        // DataStore. Once resolved, the splash dismisses and Compose renders
        // the correct start destination.
        splash.setKeepOnScreenCondition { !appState.bootResolved.value }

        setContent {
            CalSnapTheme {
                val start by appState.startDestination.collectAsState()
                Surface(modifier = Modifier.fillMaxSize()) {
                    start?.let { CalSnapNavHost(startDestination = it) }
                }
            }
        }
    }
}

/**
 * Tiny holder for boot-time state resolved before the first composition.
 * Real app will populate `startDestination` + `bootResolved` from
 * UserRepository.flowOnboardingCompleted() in the injected module.
 */
@Singleton
class AppBootState @Inject constructor(userRepository: UserRepository) {
    val bootResolved = MutableStateFlow(false)
    val startDestination = MutableStateFlow<String?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            val completed = userRepository.onboardingCompleted.first()
            startDestination.value = if (completed) Screen.Home.route else Screen.Onboarding.route
            bootResolved.value = true
        }
    }
}
