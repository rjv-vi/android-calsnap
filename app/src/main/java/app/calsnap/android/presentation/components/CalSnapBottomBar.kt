package app.calsnap.android.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.calsnap.android.R
import app.calsnap.android.presentation.navigation.Screen

/**
 * Bottom nav mirroring the PWA layout: Home | Add (accent) | Settings.
 * The PWA's 5-tab nav (Home / Progress / Add / AI / Settings) collapses to
 * 3 in v1 — Progress + AI tabs come in v1.1 (see ROADMAP.md).
 */
@Composable
fun CalSnapBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick  = { onNavigate(Screen.Home.route) },
            icon     = { Icon(Icons.Default.Home, contentDescription = null) },
            label    = { Text(stringResource(R.string.nav_home)) },
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Progress.route,
            onClick  = { onNavigate(Screen.Progress.route) },
            icon     = { Icon(Icons.Default.Favorite, contentDescription = null) },
            label    = { Text(stringResource(R.string.nav_progress)) },
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Add.route,
            onClick  = { onNavigate(Screen.Add.route) },
            icon     = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp)) },
            label    = { Text(stringResource(R.string.nav_add)) },
            colors   = NavigationBarItemDefaults.colors(),
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Ai.route,
            onClick  = { onNavigate(Screen.Ai.route) },
            icon     = { Icon(Icons.Default.Chat, contentDescription = null) },
            label    = { Text(stringResource(R.string.nav_ai)) },
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick  = { onNavigate(Screen.Settings.route) },
            icon     = { Icon(Icons.Default.Settings, contentDescription = null) },
            label    = { Text(stringResource(R.string.nav_settings)) },
        )
    }
}
