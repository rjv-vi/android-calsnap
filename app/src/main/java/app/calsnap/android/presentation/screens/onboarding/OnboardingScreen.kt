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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapPill
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapTextField
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var height by remember { mutableStateOf(draft.heightCm.toInt().toString()) }
    var weight by remember { mutableStateOf(draft.weightKg.toInt().toString()) }
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoBlock()
            StepDots(step)
            CalSnapCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(34.dp),
                padding = PaddingValues(18.dp),
                elevation = 20.dp,
            ) {
                AnimatedContent(
                    targetState = step,
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (step > 1) {
                    CalSnapSecondaryButton(
                        onClick = { viewModel.update { it.copy(step = step - 1) } },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.onboarding_back)) }
                }
                CalSnapPrimaryButton(
                    onClick = {
                        if (step < 5) viewModel.update { it.copy(step = step + 1) } else viewModel.finish(onFinished)
                    },
                    enabled = if (step < 5) canNext else canFinish,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (step < 5) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_finish))
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "🍎",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.graphicsLayer { translationY = y },
        )
        Text("CalSnap", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
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
                    .height(5.dp)
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
        Text(stringResource(R.string.onboarding_gender_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        ChoiceRow(
            options = listOf(
                Choice(UserProfile.Gender.MALE, "♂", stringResource(R.string.onboarding_gender_male)),
                Choice(UserProfile.Gender.FEMALE, "♀", stringResource(R.string.onboarding_gender_female)),
            ),
            selected = draft.gender,
            onSelect = { value -> viewModel.update { it.copy(gender = value) } },
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
        StepTitle(stringResource(R.string.onboarding_body_title), stringResource(R.string.onboarding_dob_label))
        DateDrum(
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
        CalSnapPill(text = if (ageFromDraft(draft) > 0) "${ageFromDraft(draft)}" else "—", selected = false, icon = "🎂")
    }
}

@Composable
private fun DateDrum(dob: String, onDob: (String) -> Unit) {
    val parts = parseDob(dob)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.onboarding_dob_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DrumColumn(
                label = stringResource(R.string.onboarding_dob_year),
                value = parts.year.toString(),
                onMinus = { onDob(formatDob(parts.copy(year = (parts.year - 1).coerceIn(1940, 2020)))) },
                onPlus = { onDob(formatDob(parts.copy(year = (parts.year + 1).coerceIn(1940, 2020)))) },
                modifier = Modifier.weight(1.25f),
            )
            DrumColumn(
                label = stringResource(R.string.onboarding_dob_month),
                value = parts.month.toString().padStart(2, '0'),
                onMinus = { onDob(formatDob(parts.copy(month = if (parts.month == 1) 12 else parts.month - 1))) },
                onPlus = { onDob(formatDob(parts.copy(month = if (parts.month == 12) 1 else parts.month + 1))) },
                modifier = Modifier.weight(1f),
            )
            DrumColumn(
                label = stringResource(R.string.onboarding_dob_day),
                value = parts.day.toString().padStart(2, '0'),
                onMinus = { onDob(formatDob(parts.copy(day = if (parts.day == 1) maxDay(parts.year, parts.month) else parts.day - 1))) },
                onPlus = { onDob(formatDob(parts.copy(day = if (parts.day >= maxDay(parts.year, parts.month)) 1 else parts.day + 1))) },
                modifier = Modifier.weight(1f),
            )
        }
        Text(formatDob(parts), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DrumColumn(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        DrumButton("＋", onPlus)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        DrumButton("−", onMinus)
    }
}

@Composable
private fun DrumButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .calSnapClickable(pressedScale = 0.86f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StepActivity(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_activity_title), stringResource(R.string.onboarding_activity_subtitle))
        Text(stringResource(R.string.onboarding_activity_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        ChoiceColumn(
            options = listOf(
                Choice(UserProfile.Activity.SEDENTARY, "🪑", stringResource(R.string.onboarding_activity_sedentary)),
                Choice(UserProfile.Activity.LIGHT, "🚶", stringResource(R.string.onboarding_activity_light)),
                Choice(UserProfile.Activity.MODERATE, "🏃", stringResource(R.string.onboarding_activity_moderate)),
                Choice(UserProfile.Activity.ACTIVE, "💪", stringResource(R.string.onboarding_activity_active)),
            ),
            selected = draft.activity,
            onSelect = { value -> viewModel.update { it.copy(activity = value) } },
        )
    }
}

@Composable
private fun StepGoal(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_goal_title), stringResource(R.string.onboarding_goal_subtitle))
        Text(stringResource(R.string.onboarding_goal_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        ChoiceRow(
            options = listOf(
                Choice(UserProfile.Goal.LOSE, "↓", stringResource(R.string.onboarding_goal_lose)),
                Choice(UserProfile.Goal.MAINTAIN, "=", stringResource(R.string.onboarding_goal_maintain)),
                Choice(UserProfile.Goal.GAIN, "↑", stringResource(R.string.onboarding_goal_gain)),
            ),
            selected = draft.goal,
            onSelect = { value -> viewModel.update { it.copy(goal = value) } },
        )
        SummaryPill("🔥", goalLabel(draft.goal))
    }
}

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
        SummaryPill("📏", "${draft.heightCm.toInt()} ${stringResource(R.string.unit_cm)} · ${draft.weightKg.toInt()} ${stringResource(R.string.unit_kg)}")
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
    val fallback = DobParts(1998, 5, 21)
    if (values.size != 3) return fallback
    val year = values[0].coerceIn(1940, 2020)
    val month = values[1].coerceIn(1, 12)
    val day = values[2].coerceIn(1, maxDay(year, month))
    return DobParts(year, month, day)
}

private fun formatDob(parts: DobParts): String {
    val day = parts.day.coerceIn(1, maxDay(parts.year, parts.month))
    return "${parts.year}-${parts.month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun maxDay(year: Int, month: Int): Int = when (month) {
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private data class Choice<T>(val value: T, val icon: String, val label: String)
