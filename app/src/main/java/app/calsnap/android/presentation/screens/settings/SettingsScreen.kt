package app.calsnap.android.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_section_ai))
            ApiKeyCard(
                hasKey    = ui.hasGeminiKey,
                onSave    = viewModel::saveGeminiKey,
                onClear   = viewModel::clearGeminiKey,
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_appearance))
            ThemeRow(
                darkTheme  = ui.darkTheme,
                onChange   = viewModel::setDarkTheme,
            )
            LanguageRow(
                current = ui.language,
                onChange = viewModel::setLanguage,
            )

            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_footer_v),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ApiKeyCard(
    hasKey: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    if (hasKey) R.string.settings_api_set else R.string.settings_api_not_set,
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.settings_api_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { onSave(input); input = "" },
                enabled  = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_api_save))
            }
            if (hasKey) {
                OutlinedButton(
                    onClick  = onClear,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_api_clear))
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(darkTheme: Boolean?, onChange: (Boolean?) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_dark_theme),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = darkTheme == true,
                onCheckedChange = { on -> onChange(if (on) true else null) },
            )
        }
    }
}

@Composable
private fun LanguageRow(current: String, onChange: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = { onChange(if (current == "ru") "en" else "ru") },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(current.uppercase())
            }
        }
    }
}
