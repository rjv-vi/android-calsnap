package app.calsnap.android.presentation.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R

@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshKeyState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.ai_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!ui.hasApiKey) {
                ApiMissing()
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.messages) { msg ->
                    MessageBubble(msg)
                }
            }
            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ui.input,
                    onValueChange = viewModel::updateInput,
                    label = { Text(stringResource(R.string.ai_input_hint)) },
                    modifier = Modifier.weight(1f),
                    enabled = ui.hasApiKey && !ui.loading,
                    minLines = 1,
                    maxLines = 4,
                )
                Button(
                    onClick = viewModel::send,
                    enabled = ui.hasApiKey && ui.input.isNotBlank() && !ui.loading,
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    } else {
                        Text(stringResource(R.string.ai_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiMissing() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.api_key_needed_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.api_key_needed_sub), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MessageBubble(message: AiChatViewModel.ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(modifier = Modifier.fillMaxWidth(0.86f)) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = if (message.fromUser) stringResource(R.string.ai_you) else stringResource(R.string.nav_ai),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
