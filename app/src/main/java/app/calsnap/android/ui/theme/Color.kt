package app.calsnap.android.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand ──────────────────────────────────────────────────────────────────
// Pulled from the CalSnap PWA (--acc / --bg0 / --bg1 tokens). Keeping the
// native app visually in the CalSnap family even though the PWA uses a
// flatter, iOS-26-ish style while we lean into Material 3.
val CalSnapOrange       = Color(0xFFFF5500)
val CalSnapOrangeLight  = Color(0xFFFF6618)
val CalSnapOrangeDark   = Color(0xFFC84000)
val CalSnapOrangeSoft   = Color(0x1AFF5500)
val CalSnapInk          = Color(0xFF141210)
val CalSnapInkSoft      = Color(0xFF2A2622)
val CalSnapWarmBg       = Color(0xFFF2F0EB)
val CalSnapWarmSurface  = Color(0xFFFFFFFF)
val CalSnapWarmSurface2 = Color(0xFFF7F5F1)
val CalSnapStreak       = Color(0xFFFF5500)

// ─── Macro chips ────────────────────────────────────────────────────────────
val MacroProtein = Color(0xFFB84530) // Б
val MacroCarbs   = Color(0xFF8B6020) // У
val MacroFat     = Color(0xFF1A50AE) // Ж
val MacroWater   = Color(0xFF1D4ED8)

// ─── Surfaces (light) ───────────────────────────────────────────────────────
val LightBg      = CalSnapWarmBg
val LightSurface = CalSnapWarmSurface
val LightOnBg    = CalSnapInk
val LightOnBgMuted = Color(0x8A141210)

// ─── Surfaces (dark) ────────────────────────────────────────────────────────
val DarkBg       = Color(0xFF0F0E0C)
val DarkSurface  = Color(0xFF1C1A16)
val DarkOnBg     = Color(0xFFF2F0EB)
val DarkOnBgMuted= Color(0xFF9A9489)
