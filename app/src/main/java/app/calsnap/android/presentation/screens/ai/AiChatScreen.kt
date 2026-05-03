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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapPill
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak

@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshKeyState() }

    CalSnapScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedSection(0) { AiHeader() }
            if (!ui.hasApiKey) AnimatedSection(1) { ApiMissing() }
            QuickPrompts(enabled = ui.hasApiKey && !ui.loading, onPrompt = viewModel::updateInput)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
private fun AiHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2A2622), Color(0xFF141210))))
                .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🤖", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.background), CircleShape),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ai_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                Text(stringResource(R.string.ai_online), color = Color(0xFF22C55E), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("↻", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
        }
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(prompts) { prompt ->
            CalSnapPill(
                text = prompt,
                selected = false,
                onClick = if (enabled) ({ onPrompt(prompt) }) else null,
            )
        }
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
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (message.fromUser) stringResource(R.string.ai_you) else stringResource(R.string.nav_ai),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = if (message.fromUser) 24.dp else 7.dp,
                                bottomEnd = if (message.fromUser) 7.dp else 24.dp,
                            ),
                        )
                        .background(if (message.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(14.dp),
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
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(14.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .border(androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .border(androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), CircleShape)
                .calSnapClickable(enabled = ui.hasApiKey && !ui.loading, pressedScale = 0.86f, onClick = {}),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface)
                .calSnapClickable(
                    enabled = ui.hasApiKey && ui.input.isNotBlank() && !ui.loading,
                    pressedScale = 0.84f,
                    onClick = viewModel::send,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Send, contentDescription = stringResource(R.string.ai_send), tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(20.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
}
