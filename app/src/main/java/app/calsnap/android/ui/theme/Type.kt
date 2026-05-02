package app.calsnap.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default Material 3 typography with CalSnap's weight tuning: the PWA leans
// on heavy (800) display numbers for the calorie ring, so we bump display /
// title weights here to match. We skip a custom font for v1 to keep the APK
// small; DM Sans is available on Android 13+ via system fallback and looks
// close to Roboto at the weights we use.
val CalSnapTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black,   fontSize = 58.sp, letterSpacing = (-4).sp),
    displayMedium= TextStyle(fontWeight = FontWeight.Black,   fontSize = 52.sp, letterSpacing = (-3).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-1.4).sp),

    headlineLarge  = TextStyle(fontWeight = FontWeight.Black, fontSize = 32.sp, letterSpacing = (-1.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-0.8).sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.4).sp),

    titleLarge  = TextStyle(fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.8).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    titleSmall  = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = (-0.1).sp),

    bodyLarge  = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall  = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 11.sp, letterSpacing = 0.5.sp),
)
