package app.calsnap.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface CalSnapToastHostState {
    fun show(message: String)
}

private object NoOpToastHostState : CalSnapToastHostState {
    override fun show(message: String) = Unit
}

val LocalCalSnapToastHost = staticCompositionLocalOf<CalSnapToastHostState> { NoOpToastHostState }

@Composable
fun CalSnapFeedbackHost(content: @Composable () -> Unit) {
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    val hostState = remember(scope) {
        object : CalSnapToastHostState {
            override fun show(messageText: String) {
                hideJob?.cancel()
                message = messageText
                hideJob = scope.launch {
                    delay(2400)
                    message = null
                }
            }
        }
    }

    CompositionLocalProvider(LocalCalSnapToastHost provides hostState) {
        Box(Modifier.fillMaxSize()) {
            content()
            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn(tween(140, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(160, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(120, easing = FastOutSlowInEasing)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 92.dp),
            ) {
                message?.let { text ->
                    Box(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalSnapConfirmDialog(
    visible: Boolean,
    icon: String,
    title: String,
    body: String,
    actionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String,
    destructive: Boolean = false,
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ),
                    ),
                )
                .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), RoundedCornerShape(30.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(icon, style = MaterialTheme.typography.headlineLarge)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalSnapSecondaryButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(cancelLabel)
                }
                CalSnapPrimaryButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        actionLabel,
                        color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
            }
        }
    }
}
