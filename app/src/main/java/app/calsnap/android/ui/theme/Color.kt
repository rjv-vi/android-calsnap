package app.calsnap.android.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand ──────────────────────────────────────────────────────────────────
// Pulled from the CalSnap PWA (--acc / --bg0 / --bg1 tokens). Keeping the
// native app visually in the CalSnap family even though the PWA uses a
// flatter, iOS-26-ish style while we lean into Material 3.
val CalSnapOrange       = Color(0xFFFF7A30)
val CalSnapOrangeLight  = Color(0xFFFF9A5C)
val CalSnapOrangeDark   = Color(0xFFCC5F1C)
val CalSnapOrangeSoft   = Color(0xFFFFE8DC)

// ─── Macro chips ────────────────────────────────────────────────────────────
val MacroProtein = Color(0xFFE85D5D) // Б
val MacroCarbs   = Color(0xFF3B82F6) // У
val MacroFat     = Color(0xFFFACC15) // Ж
val MacroWater   = Color(0xFF3B82F6)

// ─── Surfaces (light) ───────────────────────────────────────────────────────
val LightBg      = Color(0xFFF2F0EB)
val LightSurface = Color(0xFFFFFFFF)
val LightOnBg    = Color(0xFF1C1A16)
val LightOnBgMuted = Color(0xFF7B7469)

// ─── Surfaces (dark) ────────────────────────────────────────────────────────
val DarkBg       = Color(0xFF0F0E0C)
val DarkSurface  = Color(0xFF1C1A16)
val DarkOnBg     = Color(0xFFF2F0EB)
val DarkOnBgMuted= Color(0xFF9A9489)
