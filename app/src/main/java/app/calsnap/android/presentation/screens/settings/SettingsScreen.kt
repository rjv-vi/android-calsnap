package app.calsnap.android.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import app.calsnap.android.presentation.components.CalSnapConfirmDialog
import app.calsnap.android.presentation.components.CalSnapHapticEffect
import app.calsnap.android.presentation.components.CalSnapIconTile
import app.calsnap.android.presentation.components.CalSnapPill
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapSoundEffect
import app.calsnap.android.presentation.components.CalSnapTextField
import app.calsnap.android.presentation.components.LocalCalSnapEffects
import app.calsnap.android.presentation.components.LocalCalSnapToastHost
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var profileSheet by remember { mutableStateOf<ProfileSheet?>(null) }
    var resetConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val toast = LocalCalSnapToastHost.current
    val effects = LocalCalSnapEffects.current
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val payload = ui.exportPayload
        if (uri != null && payload != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.content.toByteArray()) }
            effects.haptic.play(CalSnapHapticEffect.Success)
            effects.sound.play(CalSnapSoundEffect.ExportDone)
            toast.show(context.getString(R.string.settings_export_done))
        }
        viewModel.consumeExport()
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val payload = ui.exportPayload
        if (uri != null && payload != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.content.toByteArray()) }
            effects.haptic.play(CalSnapHapticEffect.Success)
            effects.sound.play(CalSnapSoundEffect.ExportDone)
            toast.show(context.getString(R.string.settings_export_done))
        }
        viewModel.consumeExport()
    }

    LaunchedEffect(ui.exportPayload) {
        val payload = ui.exportPayload ?: return@LaunchedEffect
        if (payload.mimeType == "application/json") jsonLauncher.launch(payload.fileName)
        else csvLauncher.launch(payload.fileName)
    }
    LaunchedEffect(ui.exportError) {
        ui.exportError?.let { toast.show(it) }
    }

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
                ProfileCard(profile = ui.profile, onEdit = { profileSheet = it })
            }
            AnimatedSection(5) {
                SectionLabel(stringResource(R.string.settings_section_data))
                DataCard(
                    loading = ui.exportLoading,
                    onExportCsv = viewModel::prepareCsvExport,
                    onExportJson = viewModel::prepareJsonExport,
                    onReset = { resetConfirm = true },
                )
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
    val profile = ui.profile
    val editing = profileSheet
    if (editing != null && profile != null) {
        ModalBottomSheet(
            onDismissRequest = { profileSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        ) {
            ProfileEditSheet(
                sheet = editing,
                profile = profile,
                onDismiss = { profileSheet = null },
                onUpdate = viewModel::updateProfile,
            )
        }
    }
    CalSnapConfirmDialog(
        visible = resetConfirm,
        icon = "🗑️",
        title = stringResource(R.string.settings_reset_title),
        body = stringResource(R.string.settings_reset_body),
        actionLabel = stringResource(R.string.settings_reset_action),
        onConfirm = {
            resetConfirm = false
            viewModel.resetAllData()
            effects.haptic.play(CalSnapHapticEffect.Heavy)
            effects.sound.play(CalSnapSoundEffect.ResetConfirm)
            toast.show(context.getString(R.string.settings_reset_done))
        },
        onDismiss = { resetConfirm = false },
        cancelLabel = stringResource(R.string.cancel),
        destructive = true,
    )
}

@Composable
private fun ProfileCard(profile: UserProfile?, onEdit: (ProfileSheet) -> Unit) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), padding = PaddingValues(0.dp), elevation = 14.dp) {
        SettingsValueRow(
            icon = "Aa",
            title = stringResource(R.string.settings_profile_name),
            value = profile?.name?.takeIf { it.isNotBlank() } ?: "—",
            onClick = { onEdit(ProfileSheet.NAME) },
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "cm",
            title = stringResource(R.string.settings_profile_params),
            value = profile?.let { "${BmrCalculator.ageFromDob(it.dob)} · ${it.heightCm.toInt()} ${stringResource(R.string.unit_cm)} · ${it.weightKg.toInt()} ${stringResource(R.string.unit_kg)}" } ?: "—",
            onClick = { onEdit(ProfileSheet.PARAMS) },
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🎯",
            title = stringResource(R.string.settings_profile_goal),
            value = profile?.goalLabel() ?: "—",
            onClick = { onEdit(ProfileSheet.GOAL) },
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🔥",
            title = stringResource(R.string.settings_profile_kcal),
            subtitle = stringResource(R.string.settings_profile_kcal_sub),
            value = profile?.let { "${it.kcalGoal} ${stringResource(R.string.unit_kcal)}" } ?: "—",
            onClick = { onEdit(ProfileSheet.NUTRITION) },
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🥗",
            title = stringResource(R.string.settings_profile_prefs),
            subtitle = profile?.prefsSummary() ?: stringResource(R.string.settings_profile_no_prefs),
            value = "›",
            onClick = { onEdit(ProfileSheet.PREFS) },
        )
    }
}

@Composable
private fun DataCard(
    loading: Boolean,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onReset: () -> Unit,
) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), padding = PaddingValues(0.dp), elevation = 14.dp) {
        SettingsValueRow(
            icon = "📊",
            title = stringResource(R.string.settings_export_csv),
            value = if (loading) "…" else "›",
            onClick = onExportCsv,
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "⬇️",
            title = stringResource(R.string.settings_export_json),
            value = if (loading) "…" else "›",
            onClick = onExportJson,
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "⬆️",
            title = stringResource(R.string.settings_import_json),
            subtitle = stringResource(R.string.settings_import_json_sub),
            value = "›",
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🗑️",
            title = stringResource(R.string.settings_reset_all),
            value = "",
            onClick = onReset,
        )
    }
}

private enum class ProfileSheet { NAME, PARAMS, GOAL, NUTRITION, PREFS }

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProfileEditSheet(
    sheet: ProfileSheet,
    profile: UserProfile,
    onDismiss: () -> Unit,
    onUpdate: (Boolean, (UserProfile) -> UserProfile) -> Unit,
) {
    var name by remember(sheet, profile.name) { mutableStateOf(profile.name) }
    var dob by remember(sheet, profile.dob) { mutableStateOf(profile.dob) }
    var gender by remember(sheet, profile.gender) { mutableStateOf(profile.gender) }
    var activity by remember(sheet, profile.activity) { mutableStateOf(profile.activity) }
    var height by remember(sheet, profile.heightCm) { mutableStateOf(profile.heightCm.toInt().toString()) }
    var weight by remember(sheet, profile.weightKg) { mutableStateOf(profile.weightKg.toInt().toString()) }
    var goal by remember(sheet, profile.goal) { mutableStateOf(profile.goal) }
    var kcal by remember(sheet, profile.kcalGoal) { mutableStateOf(profile.kcalGoal.toString()) }
    var protein by remember(sheet, profile.proteinGoal) { mutableStateOf(profile.proteinGoal.toString()) }
    var carbs by remember(sheet, profile.carbsGoal) { mutableStateOf(profile.carbsGoal.toString()) }
    var fat by remember(sheet, profile.fatGoal) { mutableStateOf(profile.fatGoal.toString()) }
    var prefs by remember(sheet, profile.preferences) { mutableStateOf(profile.preferences) }
    var allergies by remember(sheet, profile.allergies) { mutableStateOf(profile.allergies) }
    val title = when (sheet) {
        ProfileSheet.NAME -> stringResource(R.string.settings_profile_name)
        ProfileSheet.PARAMS -> stringResource(R.string.settings_profile_params)
        ProfileSheet.GOAL -> stringResource(R.string.settings_profile_goal)
        ProfileSheet.NUTRITION -> stringResource(R.string.settings_profile_kcal)
        ProfileSheet.PREFS -> stringResource(R.string.settings_profile_prefs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        when (sheet) {
            ProfileSheet.NAME -> {
                CalSnapTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = stringResource(R.string.onboarding_name_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                SaveSheetButton(
                    onClick = {
                        onUpdate(false) { it.copy(name = name.trim().ifBlank { it.name }) }
                        onDismiss()
                    },
                )
            }
            ProfileSheet.PARAMS -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CalSnapPill(
                        text = stringResource(R.string.onboarding_gender_male),
                        selected = gender == UserProfile.Gender.MALE,
                        modifier = Modifier.weight(1f),
                        onClick = { gender = UserProfile.Gender.MALE },
                    )
                    CalSnapPill(
                        text = stringResource(R.string.onboarding_gender_female),
                        selected = gender == UserProfile.Gender.FEMALE,
                        modifier = Modifier.weight(1f),
                        onClick = { gender = UserProfile.Gender.FEMALE },
                    )
                }
                CalSnapTextField(
                    value = dob,
                    onValueChange = { dob = it.take(10) },
                    label = stringResource(R.string.onboarding_dob_label),
                    placeholder = "yyyy-MM-dd",
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CalSnapTextField(
                        value = height,
                        onValueChange = { height = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = stringResource(R.string.onboarding_height_label),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    CalSnapTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = stringResource(R.string.onboarding_weight_label),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                ActivityPills(activity = activity, onActivity = { activity = it })
                SaveSheetButton(
                    onClick = {
                        onUpdate(true) {
                            it.copy(
                                dob = dob,
                                gender = gender,
                                activity = activity,
                                heightCm = height.toFloatOrNull() ?: it.heightCm,
                                weightKg = weight.toFloatOrNull() ?: it.weightKg,
                            )
                        }
                        onDismiss()
                    },
                )
            }
            ProfileSheet.GOAL -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalPill(UserProfile.Goal.LOSE, goal) { goal = it }
                    GoalPill(UserProfile.Goal.MAINTAIN, goal) { goal = it }
                    GoalPill(UserProfile.Goal.GAIN, goal) { goal = it }
                }
                SaveSheetButton(
                    onClick = {
                        onUpdate(true) { it.copy(goal = goal) }
                        onDismiss()
                    },
                )
            }
            ProfileSheet.NUTRITION -> {
                CalSnapTextField(
                    value = kcal,
                    onValueChange = { kcal = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = stringResource(R.string.settings_profile_kcal),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CalSnapTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = stringResource(R.string.macro_protein),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    CalSnapTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = stringResource(R.string.macro_carbs),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    CalSnapTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = stringResource(R.string.macro_fat),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                SaveSheetButton(
                    onClick = {
                        onUpdate(false) {
                            it.copy(
                                kcalGoal = kcal.toIntOrNull() ?: it.kcalGoal,
                                proteinGoal = protein.toIntOrNull() ?: it.proteinGoal,
                                carbsGoal = carbs.toIntOrNull() ?: it.carbsGoal,
                                fatGoal = fat.toIntOrNull() ?: it.fatGoal,
                            )
                        }
                        onDismiss()
                    },
                )
            }
            ProfileSheet.PREFS -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreferencePill("no_meat", prefs) { prefs = it }
                    PreferencePill("no_gluten", prefs) { prefs = it }
                    PreferencePill("no_lactose", prefs) { prefs = it }
                    PreferencePill("no_sugar", prefs) { prefs = it }
                    PreferencePill("vegan", prefs) { prefs = it }
                    PreferencePill("keto", prefs) { prefs = it }
                    PreferencePill("halal", prefs) { prefs = it }
                    PreferencePill("no_eggs", prefs) { prefs = it }
                }
                CalSnapTextField(
                    value = allergies,
                    onValueChange = { allergies = it.take(120) },
                    label = stringResource(R.string.onboarding_allergies_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                SaveSheetButton(
                    onClick = {
                        onUpdate(false) { it.copy(preferences = prefs, allergies = allergies) }
                        onDismiss()
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ActivityPills(activity: UserProfile.Activity, onActivity: (UserProfile.Activity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActivityPill(UserProfile.Activity.SEDENTARY, activity, Modifier.weight(1f), onActivity)
            ActivityPill(UserProfile.Activity.LIGHT, activity, Modifier.weight(1f), onActivity)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActivityPill(UserProfile.Activity.MODERATE, activity, Modifier.weight(1f), onActivity)
            ActivityPill(UserProfile.Activity.ACTIVE, activity, Modifier.weight(1f), onActivity)
        }
    }
}

@Composable
private fun ActivityPill(
    value: UserProfile.Activity,
    selected: UserProfile.Activity,
    modifier: Modifier = Modifier,
    onActivity: (UserProfile.Activity) -> Unit,
) {
    CalSnapPill(text = value.activityLabel(), selected = value == selected, modifier = modifier, onClick = { onActivity(value) })
}

@Composable
private fun GoalPill(value: UserProfile.Goal, selected: UserProfile.Goal, onGoal: (UserProfile.Goal) -> Unit) {
    CalSnapPill(text = value.goalLabel(), selected = value == selected, onClick = { onGoal(value) })
}

@Composable
private fun PreferencePill(value: String, selected: Set<String>, onPrefs: (Set<String>) -> Unit) {
    CalSnapPill(
        text = prefLabel(value),
        selected = value in selected,
        onClick = { onPrefs(if (value in selected) selected - value else selected + value) },
    )
}

@Composable
private fun SaveSheetButton(onClick: () -> Unit) {
    CalSnapPrimaryButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.save))
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
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.calSnapClickable(
                        pressedScale = 0.97f,
                        sound = CalSnapSoundEffect.SheetOpen,
                        onClick = onClick,
                    )
                } else Modifier,
            )
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
private fun UserProfile.Goal.goalLabel(): String = when (this) {
    UserProfile.Goal.LOSE -> stringResource(R.string.onboarding_goal_lose)
    UserProfile.Goal.MAINTAIN -> stringResource(R.string.onboarding_goal_maintain)
    UserProfile.Goal.GAIN -> stringResource(R.string.onboarding_goal_gain)
}

@Composable
private fun UserProfile.Activity.activityLabel(): String = when (this) {
    UserProfile.Activity.SEDENTARY -> stringResource(R.string.onboarding_activity_sedentary)
    UserProfile.Activity.LIGHT -> stringResource(R.string.onboarding_activity_light)
    UserProfile.Activity.MODERATE -> stringResource(R.string.onboarding_activity_moderate)
    UserProfile.Activity.ACTIVE -> stringResource(R.string.onboarding_activity_active)
}

@Composable
private fun UserProfile.prefsSummary(): String {
    val labels = preferences.mapNotNull { pref ->
        prefLabelOrNull(pref)
    }
    return when {
        labels.isNotEmpty() && allergies.isNotBlank() -> labels.joinToString(", ") + " · " + allergies
        labels.isNotEmpty() -> labels.joinToString(", ")
        allergies.isNotBlank() -> allergies
        else -> stringResource(R.string.settings_profile_no_prefs)
    }
}

@Composable
private fun prefLabel(value: String): String = prefLabelOrNull(value) ?: value

@Composable
private fun prefLabelOrNull(value: String): String? = when (value) {
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
