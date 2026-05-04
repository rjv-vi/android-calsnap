package app.calsnap.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.calsnap.android.R

val DmSans = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold),
    Font(R.font.dm_sans, FontWeight.ExtraBold),
    Font(R.font.dm_sans, FontWeight.Black),
)

// Default Material 3 typography with CalSnap's weight tuning: the PWA leans
// on heavy (800) display numbers for the calorie ring, so we bump display /
// title weights here to match and apply bundled DM Sans across the app.
val CalSnapTypography = Typography(
    displayLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Black, fontSize = 58.sp, letterSpacing = (-4).sp),
    displayMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Black, fontSize = 52.sp, letterSpacing = (-3).sp),
    displaySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-1.4).sp),

    headlineLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Black, fontSize = 32.sp, letterSpacing = (-1.8).sp),
    headlineMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.4).sp),

    titleLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.8).sp),
    titleMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = (-0.1).sp),

    bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
