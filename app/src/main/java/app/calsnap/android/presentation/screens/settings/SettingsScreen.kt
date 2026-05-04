package app.calsnap.android.presentation.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.remote.GeminiClient
import app.calsnap.android.domain.BmrCalculator
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapHapticEffect
import app.calsnap.android.presentation.components.CalSnapIconTile
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapSoundEffect
import app.calsnap.android.presentation.components.CalSnapTextField
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    CalSnapScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedSection(0) { SettingsHeader() }
            AnimatedSection(1) {
                SectionLabel(stringResource(R.string.settings_section_ai))
                ApiKeyCard(
                    hasKey = ui.hasGeminiKey,
                    onSave = viewModel::saveGeminiKey,
                    onClear = viewModel::clearGeminiKey,
                )
            }
            AnimatedSection(2) {
                ModelCard(
                    hasKey = ui.hasGeminiKey,
                    selectedModel = ui.selectedModel,
                    models = ui.models,
                    loading = ui.modelsLoading,
                    error = ui.modelsError,
                    onLoad = viewModel::loadGeminiModels,
                    onSelect = viewModel::selectGeminiModel,
                )
            }
            AnimatedSection(3) {
                SectionLabel(stringResource(R.string.settings_section_appearance))
                ThemeRow(
                    darkTheme = ui.darkTheme,
                    onChange = viewModel::setDarkTheme,
                )
                Spacer(Modifier.height(10.dp))
                LanguageRow(
                    current = ui.language,
                    onChange = viewModel::setLanguage,
                )
                Spacer(Modifier.height(10.dp))
                ToggleSettingRow(
                    icon = "🔊",
                    title = stringResource(R.string.settings_sounds),
                    subtitle = stringResource(R.string.settings_sounds_sub),
                    checked = ui.soundOn,
                    onChange = viewModel::setSoundOn,
                )
                Spacer(Modifier.height(10.dp))
                ToggleSettingRow(
                    icon = "📳",
                    title = stringResource(R.string.settings_haptics),
                    subtitle = stringResource(R.string.settings_haptics_sub),
                    checked = ui.hapticOn,
                    onChange = viewModel::setHapticOn,
                )
            }
            AnimatedSection(4) {
                SectionLabel(stringResource(R.string.settings_section_profile))
                ProfileCard(profile = ui.profile)
            }
            Text(
                text = stringResource(R.string.settings_footer_v),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfile?) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), padding = PaddingValues(0.dp), elevation = 14.dp) {
        SettingsValueRow(
            icon = "Aa",
            title = stringResource(R.string.settings_profile_name),
            value = profile?.name?.takeIf { it.isNotBlank() } ?: "—",
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "cm",
            title = stringResource(R.string.settings_profile_params),
            value = profile?.let { "${BmrCalculator.ageFromDob(it.dob)} · ${it.heightCm.toInt()} ${stringResource(R.string.unit_cm)} · ${it.weightKg.toInt()} ${stringResource(R.string.unit_kg)}" } ?: "—",
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🎯",
            title = stringResource(R.string.settings_profile_goal),
            value = profile?.goalLabel() ?: "—",
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🔥",
            title = stringResource(R.string.settings_profile_kcal),
            subtitle = stringResource(R.string.settings_profile_kcal_sub),
            value = profile?.let { "${it.kcalGoal} ${stringResource(R.string.unit_kcal)}" } ?: "—",
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🥗",
            title = stringResource(R.string.settings_profile_prefs),
            subtitle = profile?.prefsSummary() ?: stringResource(R.string.settings_profile_no_prefs),
            value = "›",
        )
    }
}

@Composable
private fun SettingsHeader() {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        padding = PaddingValues(16.dp),
        containerBrush = Brush.verticalGradient(listOf(CalSnapStreak.copy(alpha = 0.13f), MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))),
        borderColor = CalSnapStreak.copy(alpha = 0.18f),
        elevation = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = "⚙️", size = 54.dp, background = CalSnapStreak.copy(alpha = 0.11f))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.settings_footer_v), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun ApiKeyCard(
    hasKey: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), padding = PaddingValues(16.dp), elevation = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = "🔑", size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (hasKey) R.string.settings_api_set else R.string.settings_api_not_set),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(stringResource(R.string.api_key_needed_sub), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        CalSnapTextField(
            value = input,
            onValueChange = { input = it },
            label = stringResource(R.string.settings_api_hint),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        CalSnapPrimaryButton(
            onClick = { onSave(input); input = "" },
            enabled = input.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_api_save))
        }
        if (hasKey) {
            Spacer(Modifier.height(8.dp))
            CalSnapSecondaryButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_api_clear))
            }
        }
    }
}

@Composable
private fun ModelCard(
    hasKey: Boolean,
    selectedModel: String,
    models: List<GeminiClient.GeminiModelInfo>,
    loading: Boolean,
    error: String?,
    onLoad: () -> Unit,
    onSelect: (String) -> Unit,
) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), padding = PaddingValues(16.dp), elevation = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = "✨", size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_model_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(selectedModel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        CalSnapSecondaryButton(onClick = onLoad, enabled = hasKey && !loading, modifier = Modifier.fillMaxWidth()) {
            if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 3.dp, color = CalSnapStreak)
            else Text(stringResource(R.string.settings_model_load))
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        if (models.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                models.take(12).forEach { model ->
                    ModelRow(model, selected = model.id == selectedModel, onSelect = { onSelect(model.id) })
                }
            }
        }
    }
}

@Composable
private fun ModelRow(model: GeminiClient.GeminiModelInfo, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .calSnapClickable(pressedScale = 0.97f, onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(if (selected) "✓" else "○", color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
        Column(Modifier.weight(1f)) {
            Text(model.name, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(model.id, color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ToggleSettingRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), padding = PaddingValues(16.dp), elevation = 10.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .calSnapClickable(
                    pressedScale = 0.97f,
                    sound = CalSnapSoundEffect.Toggle,
                    haptic = CalSnapHapticEffect.Tick,
                    onClick = { onChange(!checked) },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalSnapIconTile(icon = icon, size = 46.dp, background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SettingsValueRow(
    icon: String,
    title: String,
    value: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalSnapIconTile(
            icon = icon,
            size = 44.dp,
            background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        thickness = 0.5.dp,
    )
}

@Composable
private fun UserProfile.goalLabel(): String = when (goal) {
    UserProfile.Goal.LOSE -> stringResource(R.string.onboarding_goal_lose)
    UserProfile.Goal.MAINTAIN -> stringResource(R.string.onboarding_goal_maintain)
    UserProfile.Goal.GAIN -> stringResource(R.string.onboarding_goal_gain)
}

@Composable
private fun UserProfile.prefsSummary(): String {
    val labels = preferences.mapNotNull { pref ->
        when (pref) {
            "no_meat" -> stringResource(R.string.pref_no_meat)
            "no_gluten" -> stringResource(R.string.pref_no_gluten)
            "no_lactose" -> stringResource(R.string.pref_no_lactose)
            "no_sugar" -> stringResource(R.string.pref_no_sugar)
            "vegan" -> stringResource(R.string.pref_vegan)
            "keto" -> stringResource(R.string.pref_keto)
            "halal" -> stringResource(R.string.pref_halal)
            "no_eggs" -> stringResource(R.string.pref_no_eggs)
            else -> null
        }
    }
    return when {
        labels.isNotEmpty() && allergies.isNotBlank() -> labels.joinToString(", ") + " · " + allergies
        labels.isNotEmpty() -> labels.joinToString(", ")
        allergies.isNotBlank() -> allergies
        else -> stringResource(R.string.settings_profile_no_prefs)
    }
}

@Composable
private fun ThemeRow(darkTheme: Boolean?, onChange: (Boolean?) -> Unit) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), padding = PaddingValues(16.dp), elevation = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = if (darkTheme == true) "🌙" else "☀️", size = 46.dp, background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_dark_theme), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(if (darkTheme == true) "Dark" else "System / Light", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = darkTheme == true, onCheckedChange = { on -> onChange(if (on) true else null) })
        }
    }
}

@Composable
private fun LanguageRow(current: String, onChange: (String) -> Unit) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), padding = PaddingValues(16.dp), elevation = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = "🌐", size = 46.dp, background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
            Text(stringResource(R.string.settings_language), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .calSnapClickable { onChange(if (current == "ru") "en" else "ru") }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(current.uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
            }
        }
    }
}
