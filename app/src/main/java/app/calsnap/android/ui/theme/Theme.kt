package app.calsnap.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Brand-first color scheme ───────────────────────────────────────────────
// Used when the user has explicitly opted out of Material You dynamic colors
// (or if we decide to pin the brand orange in some contexts). Otherwise we
// prefer wallpaper-derived palettes via Android 12+ `dynamicColorScheme`.
private val LightColors = lightColorScheme(
    primary            = CalSnapInk,
    onPrimary          = LightBg,
    primaryContainer   = Color(0xFFE8E4DC),
    onPrimaryContainer = CalSnapInk,
    secondary          = CalSnapStreak,
    onSecondary        = Color.White,
    background         = LightBg,
    onBackground       = LightOnBg,
    surface            = LightSurface,
    onSurface          = LightOnBg,
    surfaceVariant     = CalSnapWarmSurface2,
    onSurfaceVariant   = LightOnBgMuted,
    outline            = Color(0x24141210),
    error              = Color(0xFFC62626),
    onError            = Color.White,
    errorContainer     = Color(0xFFFBE5E5),
    onErrorContainer   = Color(0xFF7C1717),
)

private val DarkColors = darkColorScheme(
    primary            = DarkOnBg,
    onPrimary          = DarkBg,
    primaryContainer   = Color(0xFF2C2825),
    onPrimaryContainer = DarkOnBg,
    secondary          = CalSnapStreak,
    onSecondary        = Color(0xFF2A1608),
    background         = DarkBg,
    onBackground       = DarkOnBg,
    surface            = DarkSurface,
    onSurface          = DarkOnBg,
    surfaceVariant     = Color(0xFF2A2822),
    onSurfaceVariant   = DarkOnBgMuted,
    outline            = Color(0x24F4F2EE),
    error              = Color(0xFFE03535),
    onError            = Color(0xFF3A0A0A),
    errorContainer     = Color(0xFF3A1515),
    onErrorContainer   = Color(0xFFFFD6D6),
)

@Composable
fun CalSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic Material You colors are the default on Android 12+; minSdk 33
    // guarantees availability, so we default to true. Users who prefer the
    // hard-brand look can flip this in Settings (wired up in v1.1).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: content draws under the status bar, we just flip
            // the icon tint based on background luminance.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CalSnapTypography,
        shapes      = CalSnapShapes,
        content     = content,
    )
}
