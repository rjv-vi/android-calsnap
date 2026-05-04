package app.calsnap.android.presentation.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSoundEffect
import app.calsnap.android.presentation.components.LocalCalSnapEffects
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapInk
import app.calsnap.android.ui.theme.CalSnapStreak

@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val effects = LocalCalSnapEffects.current
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { viewModel.refreshKeyState() }
    LaunchedEffect(ui.messages.size) {
        val last = ui.messages.lastOrNull()
        if (ui.messages.size > 1 && last?.fromUser == false) {
            effects.sound.play(CalSnapSoundEffect.AiReply)
        }
    }
    LaunchedEffect(ui.error) {
        if (ui.error != null) effects.sound.play(CalSnapSoundEffect.AiError)
    }
    LaunchedEffect(ui.messages.size, ui.loading) {
        val target = ui.messages.lastIndex + if (ui.loading) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target)
    }

    CalSnapScreen(glow = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            AiHeader(onBack = onBack)
            QuickPrompts(enabled = ui.hasApiKey && !ui.loading, onPrompt = viewModel::updateInput)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!ui.hasApiKey) item { ApiMissing() }
                itemsIndexed(ui.messages) { index, msg -> MessageBubble(msg, index) }
                if (ui.loading) item { TypingBubble() }
            }
            ui.error?.let { ErrorCard(it) }
            InputBar(ui, viewModel)
        }
    }
}

@Composable
private fun AiHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AiCircleButton(text = "‹", sound = CalSnapSoundEffect.Back, onClick = onBack)
            AiAvatar(size = 42.dp, online = true)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ai_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = (-0.3).sp)
                Text(stringResource(R.string.ai_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            AiStatusPill()
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), thickness = 0.5.dp)
    }
}

@Composable
private fun AiAvatar(size: androidx.compose.ui.unit.Dp, online: Boolean) {
    val pulse by rememberInfiniteTransition(label = "aiOnlinePulse").animateFloat(
        initialValue = 0.58f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "aiOnlinePulseValue",
    )
    val contentColor = aiAccentContentColor()
    Box(
        modifier = Modifier
            .size(size)
            .shadow(10.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(aiAccentBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Text("AI", color = contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(1.dp)
                    .size(11.dp)
                    .graphicsLayer { alpha = pulse }
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.surface), CircleShape),
            )
        }
    }
}

@Composable
private fun AiStatusPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CalSnapStreak.copy(alpha = 0.10f))
            .border(BorderStroke(0.5.dp, CalSnapStreak.copy(alpha = 0.18f)), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.ai_online), color = CalSnapStreak, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AiCircleButton(text: String, sound: CalSnapSoundEffect, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .shadow(4.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), CircleShape)
            .calSnapClickable(pressedScale = 0.84f, sound = sound, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickPrompts(enabled: Boolean, onPrompt: (String) -> Unit) {
    val prompts = listOf(
        stringResource(R.string.ai_prompt_norm),
        stringResource(R.string.ai_prompt_eat),
        stringResource(R.string.ai_prompt_diet),
        stringResource(R.string.ai_prompt_bulk),
        stringResource(R.string.ai_prompt_snack),
        stringResource(R.string.ai_prompt_cut),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f)),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            items(prompts) { prompt ->
                AiSuggestionChip(
                    text = prompt,
                    onClick = if (enabled) ({ onPrompt(prompt) }) else null,
                )
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), thickness = 0.5.dp)
    }
}

@Composable
private fun AiSuggestionChip(text: String, onClick: (() -> Unit)?) {
    val enabled = onClick != null
    Box(
        modifier = Modifier
            .shadow(5.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), RoundedCornerShape(20.dp))
            .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
            .then(if (onClick != null) Modifier.calSnapClickable(pressedScale = 0.91f, sound = CalSnapSoundEffect.AiSend, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ApiMissing() {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        padding = PaddingValues(16.dp),
        containerBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.errorContainer)),
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.24f),
        elevation = 8.dp,
    ) {
        Text(stringResource(R.string.api_key_needed_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.api_key_needed_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun MessageBubble(message: AiChatViewModel.ChatMessage, index: Int) {
    var visible by remember(message.text, message.fromUser) { mutableStateOf(false) }
    LaunchedEffect(message.text, message.fromUser) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, delayMillis = (index % 4) * 20, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(220, easing = FastOutSlowInEasing)) { 10 },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        ) {
            if (message.fromUser) UserMessage(message.text) else AiMessage(message.text)
        }
    }
}

@Composable
private fun UserMessage(text: String) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
    val contentColor = aiAccentContentColor()
    Box(
        modifier = Modifier
            .fillMaxWidth(0.84f)
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(aiAccentBrush())
            .padding(horizontal = 15.dp, vertical = 11.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = contentColor,
        )
    }
}

@Composable
private fun AiMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(0.90f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        AiAvatar(size = 28.dp, online = false)
        Column(Modifier.weight(1f)) {
            Text(
                "CALSNAP AI",
                modifier = Modifier.padding(start = 2.dp, bottom = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
            Box(
                modifier = Modifier
                    .shadow(6.dp, shape, clip = false)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), shape)
                    .padding(horizontal = 15.dp, vertical = 11.dp),
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Row(
            modifier = Modifier.fillMaxWidth(0.90f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AiAvatar(size = 28.dp, online = false)
            Row(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp), clip = false)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp))
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TypingDot(0)
                TypingDot(120)
                TypingDot(240)
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.ai_typing), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TypingDot(delayMillis: Int) {
    val dot by rememberInfiniteTransition(label = "typingDot$delayMillis").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, delayMillis = delayMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "typingDotValue$delayMillis",
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer {
                alpha = 0.35f + dot * 0.65f
                scaleX = 0.82f + dot * 0.26f
                scaleY = 0.82f + dot * 0.26f
                translationY = -dot * 5f
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant),
    )
}

@Composable
private fun ErrorCard(message: String) {
    CalSnapCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        padding = PaddingValues(12.dp),
        containerBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.errorContainer)),
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.24f),
        elevation = 6.dp,
    ) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InputBar(ui: AiChatViewModel.UiState, viewModel: AiChatViewModel) {
    val effects = LocalCalSnapEffects.current
    val sendEnabled = ui.hasApiKey && ui.input.isNotBlank() && !ui.loading
    val sendAlpha by animateFloatAsState(
        targetValue = if (sendEnabled) 1f else 0.44f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "aiSendAlpha",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
    ) {
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .shadow(6.dp, RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = ui.input,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ui.hasApiKey && !ui.loading,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (ui.input.isBlank()) {
                                Text(stringResource(R.string.ai_input_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f), style = MaterialTheme.typography.bodyLarge)
                            }
                            innerTextField()
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(5.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer { alpha = sendAlpha }
                    .shadow(10.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(aiAccentBrush())
                    .calSnapClickable(
                        enabled = sendEnabled,
                        pressedScale = 0.80f,
                        sound = null,
                        onClick = {
                            effects.sound.play(CalSnapSoundEffect.AiSend)
                            viewModel.send()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Send, contentDescription = stringResource(R.string.ai_send), tint = aiAccentContentColor(), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun aiAccentBrush(): Brush {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    return if (dark) {
        Brush.linearGradient(listOf(Color(0xFF3A3630), Color(0xFF1A1916)))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface, CalSnapInk))
    }
}

@Composable
private fun aiAccentContentColor(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    return if (dark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.background
}
