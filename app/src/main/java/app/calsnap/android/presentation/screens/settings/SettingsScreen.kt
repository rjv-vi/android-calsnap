package app.calsnap.android.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.core.content.ContextCompat
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
    var apiKeySheet by remember { mutableStateOf(false) }
    var modelSheet by remember { mutableStateOf(false) }
    var resetConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val toast = LocalCalSnapToastHost.current
    val effects = LocalCalSnapEffects.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        toast.show(
            context.getString(
                if (granted) R.string.settings_notifications_enabled
                else R.string.settings_notifications_permission_needed,
            ),
        )
    }
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
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.onSuccess(viewModel::importJson)
                .onFailure { toast.show(it.message ?: context.getString(R.string.settings_import_failed)) }
        }
    }

    LaunchedEffect(ui.exportPayload) {
        val payload = ui.exportPayload ?: return@LaunchedEffect
        if (payload.mimeType == "application/json") jsonLauncher.launch(payload.fileName)
        else csvLauncher.launch(payload.fileName)
    }
    LaunchedEffect(ui.exportError) {
        ui.exportError?.let { toast.show(it) }
    }
    LaunchedEffect(ui.importError, ui.importDone) {
        ui.importError?.let { toast.show(it) }
        if (ui.importDone) toast.show(context.getString(R.string.settings_import_done))
        if (ui.importError != null || ui.importDone) viewModel.consumeImportResult()
    }

    CalSnapScreen(glow = false) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            SettingsHeader()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                AnimatedSection(0) {
                    SettingsSection(label = stringResource(R.string.settings_section_appearance)) {
                        AppearanceCard(
                            darkTheme = ui.darkTheme,
                            soundOn = ui.soundOn,
                            hapticOn = ui.hapticOn,
                            language = ui.language,
                            onTheme = viewModel::setDarkTheme,
                            onSound = viewModel::setSoundOn,
                            onHaptic = viewModel::setHapticOn,
                            onLanguage = viewModel::setLanguage,
                        )
                    }
                }
                AnimatedSection(1) {
                    SettingsSection(label = stringResource(R.string.settings_section_profile)) {
                        ProfileCard(profile = ui.profile, onEdit = { profileSheet = it })
                    }
                }
                AnimatedSection(2) {
                    SettingsSection(label = stringResource(R.string.settings_section_nutrition)) {
                        NutritionCard(profile = ui.profile, onEdit = { profileSheet = it })
                    }
                }
                AnimatedSection(3) {
                    SettingsSection(label = stringResource(R.string.settings_section_api)) {
                        ApiCard(
                            hasKey = ui.hasGeminiKey,
                            selectedModel = ui.selectedModel,
                            onApi = { apiKeySheet = true },
                            onModel = {
                                modelSheet = true
                                viewModel.loadGeminiModels()
                            },
                        )
                    }
                }
                AnimatedSection(4) {
                    SettingsSection(label = stringResource(R.string.settings_section_notifications)) {
                        NotificationsCard(
                            onOpen = {
                                effects.sound.play(CalSnapSoundEffect.NotificationSave)
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    toast.show(context.getString(R.string.settings_notifications_enabled))
                                } else {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        )
                    }
                }
                AnimatedSection(5) {
                    SettingsSection(label = stringResource(R.string.settings_section_data)) {
                        DataCard(
                            loading = ui.exportLoading || ui.importLoading,
                            onExportCsv = viewModel::prepareCsvExport,
                            onExportJson = viewModel::prepareJsonExport,
                            onImportJson = { importLauncher.launch(arrayOf("application/json", "text/json", "text/*")) },
                            onReset = { resetConfirm = true },
                        )
                    }
                }
                AnimatedSection(6) {
                    SettingsSection(label = stringResource(R.string.settings_section_about)) {
                        AboutCard()
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
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
    if (apiKeySheet) {
        ModalBottomSheet(
            onDismissRequest = { apiKeySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.settings_api_key), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                ApiKeyCard(
                    hasKey = ui.hasGeminiKey,
                    onSave = viewModel::saveGeminiKey,
                    onClear = viewModel::clearGeminiKey,
                )
                Spacer(Modifier.height(18.dp))
            }
        }
    }
    if (modelSheet) {
        ModalBottomSheet(
            onDismissRequest = { modelSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.settings_model_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                ModelCard(
                    hasKey = ui.hasGeminiKey,
                    selectedModel = ui.selectedModel,
                    models = ui.models,
                    loading = ui.modelsLoading,
                    error = ui.modelsError,
                    onLoad = viewModel::loadGeminiModels,
                    onSelect = viewModel::selectGeminiModel,
                )
                Spacer(Modifier.height(18.dp))
            }
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
    SettingsGroupCard {
        SettingsValueRow(
            icon = "Aa",
            iconBrush = Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))),
            title = stringResource(R.string.settings_profile_name),
            value = profile?.name?.takeIf { it.isNotBlank() } ?: "—",
            onClick = { onEdit(ProfileSheet.NAME) },
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "cm",
            iconBrush = Brush.linearGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A))),
            title = stringResource(R.string.settings_profile_params),
            value = "›",
            onClick = { onEdit(ProfileSheet.PARAMS) },
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🎯",
            iconBrush = Brush.linearGradient(listOf(Color(0xFFFF9040), Color(0xFFFF4500))),
            title = stringResource(R.string.settings_profile_goal),
            value = profile?.goalLabel() ?: "—",
            onClick = { onEdit(ProfileSheet.GOAL) },
        )
    }
}

@Composable
private fun NutritionCard(profile: UserProfile?, onEdit: (ProfileSheet) -> Unit) {
    SettingsGroupCard {
        SettingsValueRow(
            icon = "🔥",
            iconBrush = Brush.linearGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626))),
            title = stringResource(R.string.settings_profile_kcal),
            subtitle = stringResource(R.string.settings_profile_kcal_sub),
            value = profile?.let { "${it.kcalGoal} ${stringResource(R.string.unit_kcal)}" } ?: "—",
            onClick = { onEdit(ProfileSheet.NUTRITION) },
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🥗",
            iconBrush = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
            title = stringResource(R.string.settings_profile_prefs),
            subtitle = profile?.prefsSummary() ?: stringResource(R.string.settings_profile_no_prefs),
            value = "›",
            onClick = { onEdit(ProfileSheet.PREFS) },
        )
    }
}

@Composable
private fun AppearanceCard(
    darkTheme: Boolean?,
    soundOn: Boolean,
    hapticOn: Boolean,
    language: String,
    onTheme: (Boolean?) -> Unit,
    onSound: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
) {
    SettingsGroupCard {
        ToggleValueRow(
            icon = "🌙",
            iconBrush = Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFF475569))),
            title = stringResource(R.string.settings_dark_theme),
            checked = darkTheme == true,
            onChange = { onTheme(it) },
        )
        SettingsDivider()
        ToggleValueRow(
            icon = "🔊",
            iconBrush = Brush.linearGradient(listOf(CalSnapStreak, Color(0xFFFF7A1A))),
            title = stringResource(R.string.settings_sounds),
            subtitle = stringResource(R.string.settings_sounds_sub),
            checked = soundOn,
            onChange = onSound,
        )
        SettingsDivider()
        ToggleValueRow(
            icon = "📳",
            iconBrush = Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))),
            title = stringResource(R.string.settings_haptics),
            subtitle = stringResource(R.string.settings_haptics_sub),
            checked = hapticOn,
            onChange = onHaptic,
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🌐",
            iconBrush = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1E40AF))),
            title = stringResource(R.string.settings_language),
            subtitle = "Russian / English",
            value = language.uppercase(),
            onClick = { onLanguage(if (language == "ru") "en" else "ru") },
        )
    }
}

@Composable
private fun ApiCard(
    hasKey: Boolean,
    selectedModel: String,
    onApi: () -> Unit,
    onModel: () -> Unit,
) {
    SettingsGroupCard {
        SettingsValueRow(
            icon = "🔑",
            iconBrush = Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF4F46E5))),
            title = stringResource(R.string.settings_api_key),
            subtitle = stringResource(if (hasKey) R.string.settings_api_set_short else R.string.settings_api_not_set),
            value = "›",
            onClick = onApi,
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🧠",
            iconBrush = Brush.linearGradient(listOf(Color(0xFFC084FC), Color(0xFF9333EA))),
            title = stringResource(R.string.settings_model_title),
            subtitle = selectedModel,
            value = "›",
            onClick = onModel,
        )
    }
}

@Composable
private fun NotificationsCard(onOpen: () -> Unit) {
    SettingsGroupCard {
        SettingsValueRow(
            icon = "🔔",
            iconBrush = Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFEF4444))),
            title = stringResource(R.string.settings_reminders),
            subtitle = stringResource(R.string.settings_reminders_sub),
            value = "›",
            onClick = onOpen,
        )
    }
}

@Composable
private fun AboutCard() {
    SettingsGroupCard {
        SettingsValueRow(
            icon = "📊",
            iconBrush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
            title = stringResource(R.string.settings_widgets),
            subtitle = stringResource(R.string.settings_widgets_sub),
            value = "›",
        )
        SettingsDivider()
        SettingsValueRow(
            icon = "🍎",
            iconBrush = Brush.linearGradient(listOf(CalSnapStreak, Color(0xFFFF9A3C))),
            title = stringResource(R.string.settings_authors),
            subtitle = "RJV · Rizan",
            value = "›",
        )
    }
}

@Composable
private fun DataCard(
    loading: Boolean,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onReset: () -> Unit,
) {
    SettingsGroupCard {
        DataActionRow(
            icon = "⊞",
            title = stringResource(R.string.settings_export_csv),
            color = Color(0xFF22C55E),
            value = if (loading) "…" else null,
            onClick = onExportCsv,
        )
        DataActionRow(
            icon = "⇩",
            title = stringResource(R.string.settings_export_json),
            color = Color(0xFF3B82F6),
            value = if (loading) "…" else null,
            onClick = onExportJson,
        )
        DataActionRow(
            icon = "⇧",
            title = stringResource(R.string.settings_import_json),
            color = Color(0xFF22C55E),
            value = if (loading) "…" else null,
            onClick = onImportJson,
        )
        SettingsValueRow(
            icon = "🗑️",
            iconBrush = Brush.linearGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626))),
            title = stringResource(R.string.settings_reset_all),
            titleColor = Color(0xFFFF4D5A),
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
    CalSnapPrimaryButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), sound = CalSnapSoundEffect.Save) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), thickness = 0.5.dp)
    }
}

@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        SectionLabel(label)
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        padding = PaddingValues(0.dp),
        containerBrush = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ),
        ),
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        elevation = 12.dp,
        content = content,
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
            sound = CalSnapSoundEffect.Save,
        ) {
            Text(stringResource(R.string.settings_api_save))
        }
        if (hasKey) {
            Spacer(Modifier.height(8.dp))
            CalSnapSecondaryButton(onClick = onClear, modifier = Modifier.fillMaxWidth(), sound = CalSnapSoundEffect.Delete) {
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
        CalSnapSecondaryButton(onClick = onLoad, enabled = hasKey && !loading, modifier = Modifier.fillMaxWidth(), sound = CalSnapSoundEffect.Select) {
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
            .calSnapClickable(pressedScale = 0.97f, sound = CalSnapSoundEffect.Select, onClick = onSelect)
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
            SettingsToggle(checked = checked)
        }
    }
}

@Composable
private fun SettingsValueRow(
    icon: String,
    title: String,
    value: String? = null,
    subtitle: String? = null,
    iconBrush: Brush = Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFF475569))),
    iconTextColor: Color = Color.White,
    titleColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(
                if (onClick != null) {
                    Modifier.calSnapClickable(
                        pressedScale = 0.97f,
                        sound = CalSnapSoundEffect.SheetOpen,
                        onClick = onClick,
                    )
                } else Modifier,
            )
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIconTile(
            icon = icon,
            brush = iconBrush,
            textColor = iconTextColor,
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor ?: MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        value?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ToggleValueRow(
    icon: String,
    iconBrush: Brush,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .calSnapClickable(
                pressedScale = 0.989f,
                sound = CalSnapSoundEffect.Toggle,
                haptic = CalSnapHapticEffect.Tick,
                onClick = { onChange(!checked) },
            )
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIconTile(icon = icon, brush = iconBrush)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        SettingsToggle(checked = checked)
    }
}

@Composable
private fun SettingsToggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) Color(0xFF22C55E) else MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(1.dp, if (checked) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)), RoundedCornerShape(16.dp)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = if (checked) 23.dp else 3.dp)
                .size(25.dp)
                .shadow(4.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun SettingsIconTile(
    icon: String,
    brush: Brush,
    textColor: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .shadow(6.dp, RoundedCornerShape(10.dp), clip = false)
            .clip(RoundedCornerShape(10.dp))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, color = textColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DataActionRow(
    icon: String,
    title: String,
    color: Color,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 61.dp)
            .then(if (onClick != null) Modifier.calSnapClickable(pressedScale = 0.97f, onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(icon, color = color, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(title, modifier = Modifier.weight(1f), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        value?.let { Text(it, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(start = 58.dp),
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
            SettingsToggle(checked = darkTheme == true)
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
