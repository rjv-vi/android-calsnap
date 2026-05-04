package app.calsnap.android.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.calsnap.android.R
import app.calsnap.android.presentation.navigation.Screen
import app.calsnap.android.ui.theme.CalSnapInk
import app.calsnap.android.ui.theme.CalSnapInkSoft

@Composable
fun CalSnapBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        BottomItem(Screen.Home.route, stringResource(R.string.nav_home), Icons.Default.Home),
        BottomItem(Screen.Progress.route, stringResource(R.string.nav_progress), Icons.Default.Insights),
        BottomItem(Screen.Add.route, stringResource(R.string.nav_add), Icons.Default.Add, prominent = true),
        BottomItem(Screen.Ai.route, stringResource(R.string.nav_ai), Icons.Default.ChatBubble),
        BottomItem(Screen.Settings.route, stringResource(R.string.nav_settings), Icons.Default.Settings),
    )
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
    val showPill = !items[selectedIndex].prominent
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            val segmentWidth = maxWidth / items.size.toFloat()
            val pillWidth = segmentWidth * 0.80f
            val pillX by animateDpAsState(
                targetValue = segmentWidth * selectedIndex.toFloat() + segmentWidth * 0.10f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "navPillX",
            )
            val pillAlpha by animateFloatAsState(
                targetValue = if (showPill) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "navPillAlpha",
            )
            Box(
                modifier = Modifier
                    .offset(x = pillX, y = 4.dp)
                    .width(pillWidth)
                    .height(46.dp)
                    .graphicsLayer { alpha = pillAlpha }
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.25f) 0.09f else 0.07f)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    if (item.prominent) {
                        CalSnapAddNavItem(
                            item = item,
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        CalSnapNavItem(
                            item = item,
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalSnapNavItem(
    item: BottomItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navScale",
    )
    Column(
        modifier = modifier
            .calSnapClickable(pressedScale = 0.92f, sound = CalSnapSoundEffect.TabSwitch, onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = color,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent),
        )
    }
}

@Composable
private fun CalSnapAddNavItem(
    item: BottomItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "addNavScale",
    )
    val brush = if (dark) {
        Brush.linearGradient(listOf(Color(0xFFE8E5DF), Color(0xFFF4F2EE)))
    } else {
        Brush.linearGradient(listOf(CalSnapInkSoft, CalSnapInk))
    }
    val iconColor = if (dark) CalSnapInk else Color(0xFFF2F0EB)
    Column(
        modifier = modifier
            .calSnapClickable(pressedScale = 0.80f, sound = CalSnapSoundEffect.AddFood, haptic = CalSnapHapticEffect.Medium, onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = if (selected) 45f else 0f
                }
                .size(48.dp)
                .shadow(14.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(brush)
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val prominent: Boolean = false,
)
