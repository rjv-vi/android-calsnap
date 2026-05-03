package app.calsnap.android.presentation.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.domain.BmrCalculator
import app.calsnap.android.presentation.components.CalSnapPill
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapTextField
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapInk
import app.calsnap.android.ui.theme.CalSnapStreak
import java.time.LocalDate
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var height by remember { mutableStateOf(if (draft.heightCm > 0f) draft.heightCm.toInt().toString() else "") }
    var weight by remember { mutableStateOf(if (draft.weightKg > 0f) draft.weightKg.toInt().toString() else "") }
    val age = BmrCalculator.ageFromDob(draft.dob)
    val canFinish = draft.name.isNotBlank() && age in 5..120 && draft.heightCm in 80f..240f && draft.weightKg in 25f..300f
    val step = draft.step.coerceIn(1, 5)
    val canNext = when (step) {
        1 -> draft.name.isNotBlank()
        2 -> age in 5..120 && draft.heightCm in 80f..240f && draft.weightKg in 25f..300f
        else -> true
    }

    CalSnapScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .widthIn(max = 430.dp)
                .align(Alignment.TopCenter)
                .padding(top = 54.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoBlock()
            StepDots(step)
            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                transitionSpec = {
                    (fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { it / 5 }) togetherWith
                        (fadeOut(tween(120, easing = FastOutSlowInEasing)) + slideOutHorizontally(tween(170, easing = FastOutSlowInEasing)) { -it / 6 })
                },
                label = "onboardingStep",
            ) { currentStep ->
                when (currentStep) {
                    1 -> StepIdentity(draft, viewModel)
                    2 -> StepBody(draft, viewModel, height, weight, { height = it }, { weight = it })
                    3 -> StepActivity(draft, viewModel)
                    4 -> StepGoal(draft, viewModel)
                    else -> StepPreferences(draft, viewModel)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalSnapPrimaryButton(
                    onClick = {
                        if (step < 5) viewModel.update { it.copy(step = step + 1) } else viewModel.finish(onFinished)
                    },
                    enabled = if (step < 5) canNext else canFinish,
                    modifier = Modifier.fillMaxWidth(),
                    height = 58.dp,
                ) {
                    Text(if (step < 5) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_finish))
                }
                if (step > 1) {
                    CalSnapSecondaryButton(
                        onClick = { viewModel.update { it.copy(step = step - 1) } },
                        modifier = Modifier.fillMaxWidth(),
                        height = 54.dp,
                    ) { Text(stringResource(R.string.onboarding_back)) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LogoBlock() {
    val infinite = rememberInfiniteTransition(label = "logoFloat")
    val y by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoFloatY",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(
            painter = painterResource(R.drawable.calsnap_icon),
            contentDescription = null,
            modifier = Modifier
                .size(86.dp)
                .graphicsLayer { translationY = y },
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Cal", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
            Text("Snap", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = CalSnapStreak)
        }
        Text(
            stringResource(R.string.onboarding_welcome_sub),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index <= step) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun StepIdentity(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_welcome_title), stringResource(R.string.onboarding_name_label))
        CalSnapTextField(
            value = draft.name,
            onValueChange = { n -> viewModel.update { it.copy(name = n) } },
            label = stringResource(R.string.onboarding_name_label),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StepBody(
    draft: OnboardingViewModel.Draft,
    viewModel: OnboardingViewModel,
    height: String,
    weight: String,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_body_title), stringResource(R.string.onboarding_body_subtitle))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            GenderTile(
                icon = "♂",
                label = stringResource(R.string.onboarding_gender_male),
                selected = draft.gender == UserProfile.Gender.MALE,
                onClick = { viewModel.update { it.copy(gender = UserProfile.Gender.MALE) } },
                modifier = Modifier.weight(1f),
            )
            GenderTile(
                icon = "♀",
                label = stringResource(R.string.onboarding_gender_female),
                selected = draft.gender == UserProfile.Gender.FEMALE,
                onClick = { viewModel.update { it.copy(gender = UserProfile.Gender.FEMALE) } },
                modifier = Modifier.weight(1f),
            )
        }
        DobPickerField(
            dob = draft.dob,
            onDob = { value -> viewModel.update { it.copy(dob = value) } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapTextField(
                value = height,
                onValueChange = { value ->
                    val cleaned = value.filter { it.isDigit() }.take(3)
                    onHeight(cleaned)
                    cleaned.toFloatOrNull()?.let { cm -> viewModel.update { it.copy(heightCm = cm) } }
                },
                label = stringResource(R.string.onboarding_height_label),
                suffix = { Text(stringResource(R.string.unit_cm)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            CalSnapTextField(
                value = weight,
                onValueChange = { value ->
                    val cleaned = value.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                    onWeight(cleaned)
                    cleaned.toFloatOrNull()?.let { kg -> viewModel.update { it.copy(weightKg = kg) } }
                },
                label = stringResource(R.string.onboarding_weight_label),
                suffix = { Text(stringResource(R.string.unit_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        if (ageFromDraft(draft) > 0) {
            CalSnapPill(text = "${ageFromDraft(draft)}", selected = false, icon = "🎂")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DobPickerField(dob: String, onDob: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val parts = parseDob(dob)
    val hasDob = dob.isNotBlank()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.onboarding_dob_label).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)), RoundedCornerShape(22.dp))
                .calSnapClickable(pressedScale = 0.97f) { showPicker = true }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (hasDob) displayDob(parts) else stringResource(R.string.onboarding_dob_pick),
                style = MaterialTheme.typography.titleMedium,
                color = if (hasDob) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontWeight = if (hasDob) FontWeight.Black else FontWeight.Medium,
            )
            Text("▣", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
        }
        if (hasDob) {
            Text("✓ ${stringResource(R.string.onboarding_age_hint, ageFromDobParts(parts))}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
        }
    }
    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            scrimColor = Color.Black.copy(alpha = 0.62f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            DobPickerSheet(
                parts = parts,
                onDob = onDob,
                onDone = { showPicker = false },
                onCancel = { showPicker = false },
            )
        }
    }
}

@Composable
private fun DobPickerSheet(parts: DobParts, onDob: (String) -> Unit, onDone: () -> Unit, onCancel: () -> Unit) {
    var current by remember(parts) { mutableStateOf(parts) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.cancel),
                modifier = Modifier
                    .weight(1f)
                    .calSnapClickable(pressedScale = 0.94f, onClick = onCancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.onboarding_dob_label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.onSurface)
                        .calSnapClickable(
                            pressedScale = 0.94f,
                            onClick = {
                                onDob(formatDob(current))
                                onDone()
                            },
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Black)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DrumColumn(
                label = stringResource(R.string.onboarding_dob_day),
                previous = (if (current.day == 1) maxDay(current.year, current.month) else current.day - 1).toString().padStart(2, '0'),
                value = current.day.toString().padStart(2, '0'),
                next = (if (current.day >= maxDay(current.year, current.month)) 1 else current.day + 1).toString().padStart(2, '0'),
                onPrevious = { current = current.copy(day = if (current.day == 1) maxDay(current.year, current.month) else current.day - 1) },
                onNext = { current = current.copy(day = if (current.day >= maxDay(current.year, current.month)) 1 else current.day + 1) },
                modifier = Modifier.weight(1f),
            )
            DrumColumn(
                label = stringResource(R.string.onboarding_dob_month),
                previous = monthName(if (current.month == 1) 12 else current.month - 1),
                value = monthName(current.month),
                next = monthName(if (current.month == 12) 1 else current.month + 1),
                onPrevious = {
                    val nextMonth = if (current.month == 1) 12 else current.month - 1
                    current = current.copy(month = nextMonth, day = current.day.coerceIn(1, maxDay(current.year, nextMonth)))
                },
                onNext = {
                    val nextMonth = if (current.month == 12) 1 else current.month + 1
                    current = current.copy(month = nextMonth, day = current.day.coerceIn(1, maxDay(current.year, nextMonth)))
                },
                modifier = Modifier.weight(1.45f),
            )
            DrumColumn(
                label = stringResource(R.string.onboarding_dob_year),
                previous = (if (current.year <= minBirthYear()) maxBirthYear() else current.year - 1).toString(),
                value = current.year.toString(),
                next = (if (current.year >= maxBirthYear()) minBirthYear() else current.year + 1).toString(),
                onPrevious = {
                    val nextYear = if (current.year <= minBirthYear()) maxBirthYear() else current.year - 1
                    current = current.copy(year = nextYear, day = current.day.coerceIn(1, maxDay(nextYear, current.month)))
                },
                onNext = {
                    val nextYear = if (current.year >= maxBirthYear()) minBirthYear() else current.year + 1
                    current = current.copy(year = nextYear, day = current.day.coerceIn(1, maxDay(nextYear, current.month)))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DrumColumn(
    label: String,
    previous: String,
    value: String,
    next: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        DrumRow(previous, selected = false, onClick = onPrevious)
        DrumRow(value, selected = true, onClick = {})
        DrumRow(next, selected = false, onClick = onNext)
    }
}

@Composable
private fun DrumRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.90f) else Color.Transparent)
            .calSnapClickable(enabled = !selected, pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StepActivity(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_activity_title), stringResource(R.string.onboarding_activity_subtitle))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActivityTile(
                icon = "🛋️",
                label = stringResource(R.string.onboarding_activity_sedentary),
                subtitle = stringResource(R.string.onboarding_activity_sedentary_sub),
                selected = draft.activity == UserProfile.Activity.SEDENTARY,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.SEDENTARY) } },
                modifier = Modifier.weight(1f),
            )
            ActivityTile(
                icon = "🚶",
                label = stringResource(R.string.onboarding_activity_light),
                subtitle = stringResource(R.string.onboarding_activity_light_sub),
                selected = draft.activity == UserProfile.Activity.LIGHT,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.LIGHT) } },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActivityTile(
                icon = "🏃",
                label = stringResource(R.string.onboarding_activity_moderate),
                subtitle = stringResource(R.string.onboarding_activity_moderate_sub),
                selected = draft.activity == UserProfile.Activity.MODERATE,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.MODERATE) } },
                modifier = Modifier.weight(1f),
            )
            ActivityTile(
                icon = "💪",
                label = stringResource(R.string.onboarding_activity_active),
                subtitle = stringResource(R.string.onboarding_activity_active_sub),
                selected = draft.activity == UserProfile.Activity.ACTIVE,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.ACTIVE) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepGoal(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_goal_title), stringResource(R.string.onboarding_goal_subtitle))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GoalTile(
                icon = "📉",
                label = stringResource(R.string.onboarding_goal_lose),
                selected = draft.goal == UserProfile.Goal.LOSE,
                onClick = { viewModel.update { it.copy(goal = UserProfile.Goal.LOSE) } },
                modifier = Modifier.weight(1f),
            )
            GoalTile(
                icon = "⚖️",
                label = stringResource(R.string.onboarding_goal_maintain),
                selected = draft.goal == UserProfile.Goal.MAINTAIN,
                onClick = { viewModel.update { it.copy(goal = UserProfile.Goal.MAINTAIN) } },
                modifier = Modifier.weight(1f),
            )
            GoalTile(
                icon = "💪",
                label = stringResource(R.string.onboarding_goal_gain),
                selected = draft.goal == UserProfile.Goal.GAIN,
                onClick = { viewModel.update { it.copy(goal = UserProfile.Goal.GAIN) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GenderTile(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableTile(selected = selected, onClick = onClick, modifier = modifier.height(132.dp)) {
        Text(icon, style = MaterialTheme.typography.displaySmall, color = if (icon == "♂") Color(0xFF10B8AA) else Color(0xFFC026D3))
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = tileContentColor(selected))
    }
}

@Composable
private fun ActivityTile(
    icon: String,
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableTile(selected = selected, onClick = onClick, modifier = modifier.height(92.dp), horizontal = false) {
        Text("$icon $label", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = tileContentColor(selected))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tileMutedColor(selected))
    }
}

@Composable
private fun GoalTile(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableTile(selected = selected, onClick = onClick, modifier = modifier.height(112.dp)) {
        Text(icon, style = MaterialTheme.typography.headlineLarge)
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = tileContentColor(selected), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SelectableTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    val brush = if (selected) {
        Brush.linearGradient(listOf(CalSnapInk.copy(alpha = if (dark) 0.96f else 1f), Color(0xFF2A2622)))
    } else {
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)))
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .border(BorderStroke(0.5.dp, if (selected) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)), shape)
            .calSnapClickable(pressedScale = 0.92f, onClick = onClick)
            .padding(if (horizontal) 14.dp else 12.dp),
        horizontalAlignment = if (horizontal) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun tileContentColor(selected: Boolean): Color =
    if (selected) Color(0xFFF2F0EB) else MaterialTheme.colorScheme.onSurface

@Composable
private fun tileMutedColor(selected: Boolean): Color =
    if (selected) Color(0xCCF2F0EB) else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StepPreferences(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    val prefOptions = listOf(
        Choice("no_meat", "🚫🥩", stringResource(R.string.pref_no_meat)),
        Choice("no_gluten", "🚫🌾", stringResource(R.string.pref_no_gluten)),
        Choice("no_lactose", "🚫🥛", stringResource(R.string.pref_no_lactose)),
        Choice("no_sugar", "🚫🍭", stringResource(R.string.pref_no_sugar)),
        Choice("vegan", "🌱", stringResource(R.string.pref_vegan)),
        Choice("keto", "🥑", stringResource(R.string.pref_keto)),
        Choice("halal", "☪️", stringResource(R.string.pref_halal)),
        Choice("no_eggs", "🚫🥚", stringResource(R.string.pref_no_eggs)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_preferences_title), stringResource(R.string.onboarding_preferences_subtitle))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            prefOptions.forEach { choice ->
                PreferenceChip(
                    choice = choice,
                    selected = choice.value in draft.preferences,
                    onClick = {
                        viewModel.update {
                            val next = if (choice.value in it.preferences) it.preferences - choice.value else it.preferences + choice.value
                            it.copy(preferences = next)
                        }
                    },
                )
            }
        }
        CalSnapTextField(
            value = draft.allergies,
            onValueChange = { value -> viewModel.update { it.copy(allergies = value) } },
            label = stringResource(R.string.onboarding_allergies_label),
            placeholder = stringResource(R.string.onboarding_allergies_placeholder),
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        SummaryPill("👤", draft.name.ifBlank { "—" })
        SummaryPill(
            "📏",
            if (draft.heightCm > 0f && draft.weightKg > 0f) {
                "${draft.heightCm.toInt()} ${stringResource(R.string.unit_cm)} · ${draft.weightKg.toInt()} ${stringResource(R.string.unit_kg)}"
            } else {
                "—"
            },
        )
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryPill(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon)
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { choice ->
            ChoiceCard(choice, selected == choice.value, { onSelect(choice.value) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun <T> ChoiceColumn(
    options: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { choice ->
            ChoiceCard(choice, selected == choice.value, { onSelect(choice.value) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun <T> ChoiceCard(choice: Choice<T>, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            .calSnapClickable(pressedScale = 0.94f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(choice.icon, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        Text(
            choice.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun PreferenceChip(choice: Choice<String>, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            .calSnapClickable(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(choice.icon)
        Text(
            choice.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun goalLabel(goal: UserProfile.Goal): String = when (goal) {
    UserProfile.Goal.LOSE -> stringResource(R.string.onboarding_goal_lose)
    UserProfile.Goal.MAINTAIN -> stringResource(R.string.onboarding_goal_maintain)
    UserProfile.Goal.GAIN -> stringResource(R.string.onboarding_goal_gain)
}

private fun ageFromDraft(draft: OnboardingViewModel.Draft): Int = BmrCalculator.ageFromDob(draft.dob)

private data class DobParts(val year: Int, val month: Int, val day: Int)

private fun parseDob(dob: String): DobParts {
    val values = dob.split('-').mapNotNull { it.toIntOrNull() }
    val now = LocalDate.now()
    val fallback = DobParts(now.year - 20, now.monthValue, now.dayOfMonth.coerceIn(1, maxDay(now.year - 20, now.monthValue)))
    if (values.size != 3) return fallback
    val year = values[0].coerceIn(minBirthYear(), maxBirthYear())
    val month = values[1].coerceIn(1, 12)
    val day = values[2].coerceIn(1, maxDay(year, month))
    return DobParts(year, month, day)
}

private fun displayDob(parts: DobParts): String =
    "${parts.day.toString().padStart(2, '0')}.${parts.month.toString().padStart(2, '0')}.${parts.year}"

private fun ageFromDobParts(parts: DobParts): Int = BmrCalculator.ageFromDob(formatDob(parts))

private fun formatDob(parts: DobParts): String {
    val day = parts.day.coerceIn(1, maxDay(parts.year, parts.month))
    return "${parts.year}-${parts.month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun monthName(month: Int): String {
    val ru = Locale.getDefault().language == "ru"
    return when (month.coerceIn(1, 12)) {
        1 -> if (ru) "Январь" else "January"
        2 -> if (ru) "Февраль" else "February"
        3 -> if (ru) "Март" else "March"
        4 -> if (ru) "Апрель" else "April"
        5 -> if (ru) "Май" else "May"
        6 -> if (ru) "Июнь" else "June"
        7 -> if (ru) "Июль" else "July"
        8 -> if (ru) "Август" else "August"
        9 -> if (ru) "Сентябрь" else "September"
        10 -> if (ru) "Октябрь" else "October"
        11 -> if (ru) "Ноябрь" else "November"
        else -> if (ru) "Декабрь" else "December"
    }
}

private fun minBirthYear(): Int = LocalDate.now().year - 120

private fun maxBirthYear(): Int = LocalDate.now().year - 5

private fun maxDay(year: Int, month: Int): Int = when (month) {
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private data class Choice<T>(val value: T, val icon: String, val label: String)
