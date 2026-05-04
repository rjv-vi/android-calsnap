package app.calsnap.android

import android.app.LocaleManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import app.calsnap.android.data.preferences.UserPreferences
import app.calsnap.android.data.repository.UserRepository
import app.calsnap.android.presentation.components.CalSnapEffectsProvider
import app.calsnap.android.presentation.components.CalSnapFeedbackHost
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
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appState: AppBootState
    @Inject lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemNavigation()
        lifecycleScope.launch {
            prefs.seedAppearanceIfMissing(systemDarkTheme(), systemLanguage())
        }

        splash.setKeepOnScreenCondition { !appState.bootResolved.value }

        setContent {
            val darkOverride by prefs.darkTheme.collectAsState(initial = null)
            val language by prefs.storedLanguage.collectAsState(initial = null)
            val soundOn by prefs.soundOn.collectAsState(initial = false)
            val hapticOn by prefs.hapticOn.collectAsState(initial = false)
            val systemDark = isSystemInDarkTheme()
            LaunchedEffect(language) {
                language?.let(::applyLanguage)
            }
            CalSnapTheme(darkTheme = darkOverride ?: systemDark, dynamicColor = false) {
                CalSnapEffectsProvider(soundOn = soundOn, hapticOn = hapticOn) {
                    CalSnapFeedbackHost {
                        val start by appState.startDestination.collectAsState()
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            start?.let { CalSnapNavHost(startDestination = it) }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemNavigation()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemNavigation()
    }

    private fun hideSystemNavigation() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun systemDarkTheme(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun systemLanguage(): String =
        if (resources.configuration.locales[0]?.language == "ru") "ru" else "en"

    private fun applyLanguage(language: String) {
        val tags = if (language == "ru") "ru" else "en"
        val localeManager = getSystemService(LocaleManager::class.java)
        if (localeManager.applicationLocales.toLanguageTags() != tags) {
            localeManager.applicationLocales = LocaleList.forLanguageTags(tags)
        }
    }
}

@Singleton
class AppBootState @Inject constructor(userRepository: UserRepository) {
    val bootResolved = MutableStateFlow(false)
    val startDestination = MutableStateFlow<String?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            val completed = runCatching { userRepository.onboardingCompleted.first() }.getOrDefault(false)
            val hasProfile = if (completed) {
                runCatching { userRepository.profile.first() != null }.getOrDefault(false)
            } else {
                false
            }
            startDestination.value = if (completed && hasProfile) Screen.Home.route else Screen.Onboarding.route
            bootResolved.value = true
        }
    }
}
