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
    primary            = CalSnapOrange,
    onPrimary          = Color.White,
    primaryContainer   = CalSnapOrangeSoft,
    onPrimaryContainer = CalSnapOrangeDark,
    secondary          = CalSnapOrangeDark,
    onSecondary        = Color.White,
    background         = LightBg,
    onBackground       = LightOnBg,
    surface            = LightSurface,
    onSurface          = LightOnBg,
    surfaceVariant     = Color(0xFFE8E4DC),
    onSurfaceVariant   = LightOnBgMuted,
    outline            = Color(0xFFCFCABF),
    error              = Color(0xFFE14B4B),
    onError            = Color.White,
)

private val DarkColors = darkColorScheme(
    primary            = CalSnapOrangeLight,
    onPrimary          = Color(0xFF2A1608),
    primaryContainer   = CalSnapOrangeDark,
    onPrimaryContainer = CalSnapOrangeSoft,
    secondary          = CalSnapOrange,
    onSecondary        = Color.White,
    background         = DarkBg,
    onBackground       = DarkOnBg,
    surface            = DarkSurface,
    onSurface          = DarkOnBg,
    surfaceVariant     = Color(0xFF2A2822),
    onSurfaceVariant   = DarkOnBgMuted,
    outline            = Color(0xFF4A463E),
    error              = Color(0xFFFF6B6B),
    onError            = Color(0xFF3A0A0A),
)

@Composable
fun CalSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic Material You colors are the default on Android 12+; minSdk 33
    // guarantees availability, so we default to true. Users who prefer the
    // hard-brand look can flip this in Settings (wired up in v1.1).
    dynamicColor: Boolean = true,
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
