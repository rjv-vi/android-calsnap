package app.calsnap.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// CalSnap uses big, soft, iOS-26-ish corners everywhere. Material 3 defaults
// are tighter (4/8/12/16), so we go up one step across the board.
val CalSnapShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small      = RoundedCornerShape(16.dp),
    medium     = RoundedCornerShape(22.dp),
    large      = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
