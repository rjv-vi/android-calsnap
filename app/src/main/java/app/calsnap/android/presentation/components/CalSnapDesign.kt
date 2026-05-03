package app.calsnap.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.calsnap.android.ui.theme.CalSnapInk
import app.calsnap.android.ui.theme.CalSnapInkSoft
import app.calsnap.android.ui.theme.CalSnapStreak
import kotlinx.coroutines.delay

fun Modifier.calSnapClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.985f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "calSnapPress",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

@Composable
fun CalSnapScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    val topGlow = CalSnapStreak.copy(alpha = if (dark) 0.08f else 0.055f)
    val inkGlow = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.08f else 0.026f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    color = topGlow,
                    radius = size.maxDimension * 0.34f,
                    center = Offset(size.width * 0.14f, size.height * 0.02f),
                )
                drawCircle(
                    color = inkGlow,
                    radius = size.maxDimension * 0.42f,
                    center = Offset(size.width * 1.02f, size.height * 0.16f),
                )
            },
        content = content,
    )
}

@Composable
fun AnimatedSection(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 36L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { 8 },
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
fun CalSnapCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(18.dp),
    containerBrush: Brush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ),
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
    elevation: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(containerBrush)
            .border(BorderStroke(0.5.dp, borderColor), shape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content,
        )
    }
}

@Composable
fun CalSnapIconTile(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    background: Color = CalSnapStreak.copy(alpha = 0.10f),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3f))
            .background(background)
            .border(BorderStroke(0.5.dp, CalSnapStreak.copy(alpha = 0.18f)), RoundedCornerShape(size / 3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun CalSnapPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    val brush = if (dark) {
        Brush.linearGradient(listOf(Color(0xFFE8E5DF), Color(0xFFF4F2EE)))
    } else {
        Brush.linearGradient(listOf(CalSnapInkSoft, CalSnapInk))
    }
    val contentColor = if (dark) CalSnapInk else Color(0xFFF2F0EB)
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
            .shadow(14.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(brush)
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(22.dp))
            .calSnapClickable(enabled = enabled, pressedScale = 0.96f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun CalSnapSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f))
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)), RoundedCornerShape(20.dp))
            .calSnapClickable(enabled = enabled, pressedScale = 0.96f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun CalSnapTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (minLines > 1) 6 else 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    suffix: @Composable (() -> Unit)? = null,
) {
    val placeholderContent: (@Composable () -> Unit)? = placeholder?.let { hint -> { Text(hint) } }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholderContent,
        enabled = enabled,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        suffix = suffix,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            cursorColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
fun CalSnapPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(999.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(BorderStroke(0.5.dp, border), shape)
            .then(if (onClick != null) Modifier.calSnapClickable(pressedScale = 0.93f, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Text(icon, modifier = Modifier.padding(end = 6.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CalSnapProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CalSnapStreak,
    track: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(720, easing = FastOutSlowInEasing),
        label = "calSnapProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.72f), color))),
        )
    }
}

/**
 * Pill-shaped text field tuned to match the PWA `.inp` style:
 * - Soft surface fill, 22dp radius, 1.5dp outline that brightens on focus.
 * - Optional suffix label (cm/kg) and trailing icon slot (eye for password).
 * - 17sp body text, normal weight, neutral placeholder.
 */
@Composable
fun CalSnapPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    suffix: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    minHeight: Dp = 56.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.36f),
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "calSnapPillBorder",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "calSnapPillRing",
    )
    val ringColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * ringAlpha)
    val shape = RoundedCornerShape(22.dp)
    val selectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.onSurface,
        backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
    )
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .drawBehind {
                if (ringAlpha > 0f) {
                    drawRoundRect(
                        color = ringColor,
                        topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            size.width + 6.dp.toPx(),
                            size.height + 6.dp.toPx(),
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            (22 + 3).dp.toPx(),
                            (22 + 3).dp.toPx(),
                        ),
                    )
                }
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.5.dp, borderColor), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.1).sp,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (leading != null) {
                            leading()
                            Spacer(Modifier.width(10.dp))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    placeholder,
                                    style = TextStyle(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f),
                                        letterSpacing = (-0.1).sp,
                                    ),
                                )
                            }
                            innerTextField()
                        }
                        if (suffix != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                suffix,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                        if (trailing != null) {
                            Spacer(Modifier.width(10.dp))
                            trailing()
                        }
                    }
                },
            )
        }
    }
}

/**
 * Apple-emoji logo with soft orange glow, mirroring the PWA `.ob-logo` icon
 * (`filter:drop-shadow(0 12px 32px rgba(255,85,0,.4))`). Uses Compose blur
 * so the glow is real rather than baked into a drawable.
 */
@Composable
fun CalSnapAppleLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 68.dp,
    glowColor: Color = CalSnapStreak,
) {
    Box(
        modifier = modifier.size(iconSize + 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize * 0.78f)
                .blur(28.dp)
                .clip(CircleShape)
                .background(glowColor.copy(alpha = 0.42f)),
        )
        Text(
            "🍎",
            style = TextStyle(fontSize = iconSize.value.sp),
        )
    }
}

/**
 * 5-step progress dots from the original onboarding (`.ob-prog`).
 * Rendered as evenly-spaced thin pills, the completed/current ones tinted
 * with `t0` and the rest with `f1`.
 */
@Composable
fun CalSnapStepDots(
    step: Int,
    total: Int = 5,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        (1..total).forEach { index ->
            val done = index <= step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (done) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                    ),
            )
        }
    }
}
