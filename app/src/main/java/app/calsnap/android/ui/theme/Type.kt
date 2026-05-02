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
    displayLarge = TextStyle(fontWeight = FontWeight.Black,   fontSize = 56.sp, letterSpacing = (-1).sp),
    displayMedium= TextStyle(fontWeight = FontWeight.Black,   fontSize = 44.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp),

    headlineLarge  = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 20.sp),

    titleLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.sp),
    titleSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.sp),

    bodyLarge  = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall  = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 11.sp, letterSpacing = 0.5.sp),
)
