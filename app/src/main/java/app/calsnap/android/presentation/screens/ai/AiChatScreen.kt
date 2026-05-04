package app.calsnap.android.presentation.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSoundEffect
import app.calsnap.android.presentation.components.LocalCalSnapEffects
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak

@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val effects = LocalCalSnapEffects.current
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

    CalSnapScreen(glow = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            AiHeader(onBack = onBack)
            QuickPrompts(enabled = ui.hasApiKey && !ui.loading, onPrompt = viewModel::updateInput)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AiCircleButton(text = "‹", sound = CalSnapSoundEffect.Back, onClick = onBack)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2A2622), Color(0xFF141210))))
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🤖", style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface), CircleShape),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ai_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                    Text(stringResource(R.string.ai_online), color = Color(0xFF22C55E), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
            AiCircleButton(text = "↻", sound = CalSnapSoundEffect.ButtonTap, onClick = {})
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), thickness = 0.5.dp)
    }
}

@Composable
private fun AiCircleButton(text: String, sound: CalSnapSoundEffect, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            .border(androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), CircleShape)
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
            .background(MaterialTheme.colorScheme.background),
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), RoundedCornerShape(20.dp))
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
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180, delayMillis = (index % 4) * 20, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 8 },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.84f),
                horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!message.fromUser) {
                    Text(
                        text = stringResource(R.string.nav_ai).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 36.dp, bottom = 1.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (message.fromUser) 20.dp else 5.dp,
                                bottomEnd = if (message.fromUser) 5.dp else 20.dp,
                            ),
                        )
                        .background(if (message.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .border(
                            androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (message.fromUser) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                            ),
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (message.fromUser) 20.dp else 5.dp,
                                bottomEnd = if (message.fromUser) 5.dp else 20.dp,
                            ),
                        )
                        .padding(horizontal = 15.dp, vertical = 11.dp),
                ) {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .border(androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 3.dp, color = CalSnapStreak)
                Text(stringResource(R.string.ai_typing), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
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
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), RoundedCornerShape(999.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = ui.input,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ui.hasApiKey && !ui.loading,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        Box {
                            if (ui.input.isBlank()) {
                                Text(stringResource(R.string.ai_input_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f), style = MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), CircleShape)
                    .calSnapClickable(enabled = ui.hasApiKey && !ui.loading, pressedScale = 0.86f, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface)
                    .calSnapClickable(
                        enabled = ui.hasApiKey && ui.input.isNotBlank() && !ui.loading,
                        pressedScale = 0.84f,
                        sound = null,
                        onClick = {
                            effects.sound.play(CalSnapSoundEffect.AiSend)
                            viewModel.send()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Send, contentDescription = stringResource(R.string.ai_send), tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(18.dp))
            }
        }
    }
}
