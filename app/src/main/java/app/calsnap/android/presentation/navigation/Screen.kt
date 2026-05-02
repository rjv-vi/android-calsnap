package app.calsnap.android.presentation.navigation

/**
 * Sealed list of routes. The Compose NavGraph pulls from here so there's a
 * single source of truth for route strings + deep-link paths.
 */
sealed class Screen(val route: String, val deepLink: String? = null) {
    data object Onboarding : Screen("onboarding")
    data object Home       : Screen("home",       deepLink = "home")
    data object Progress   : Screen("progress",   deepLink = "progress")
    data object Add        : Screen("add",        deepLink = "add")
    data object Ai         : Screen("ai",         deepLink = "ai")
    data object Settings   : Screen("settings",   deepLink = "settings")

    companion object {
        /** All bottom-nav destinations (excludes onboarding / modal sheets). */
        val bottomBar = listOf(Home, Progress, Add, Ai, Settings)
    }
}
