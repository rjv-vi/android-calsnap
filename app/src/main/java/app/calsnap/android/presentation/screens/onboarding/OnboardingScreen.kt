package app.calsnap.android.presentation.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.domain.BmrCalculator

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
    val step = draft.step.coerceIn(1, 4)
    val canNext = when (step) {
        1 -> draft.name.isNotBlank()
        2 -> age in 5..120 && draft.heightCm in 80f..240f && draft.weightKg in 25f..300f
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("🍎 CalSnap", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.onboarding_welcome_sub), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { step / 4f },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)),
        )

        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedContent(targetState = step, label = "onboardingStep") { currentStep ->
                    when (currentStep) {
                        1 -> StepIdentity(draft, viewModel)
                        2 -> StepBody(draft, viewModel, height, weight, { height = it }, { weight = it })
                        3 -> StepGoals(draft, viewModel)
                        else -> StepFinish(draft, age)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { viewModel.update { it.copy(step = step - 1) } },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(R.string.onboarding_back)) }
            }
            Button(
                onClick = {
                    if (step < 4) viewModel.update { it.copy(step = step + 1) } else viewModel.finish(onFinished)
                },
                enabled = if (step < 4) canNext else canFinish,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(if (step < 4) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_finish))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepIdentity(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = draft.name,
            onValueChange = { n -> viewModel.update { it.copy(name = n) } },
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.onboarding_gender_label), style = MaterialTheme.typography.titleSmall)
        ChoiceRow(
            options = listOf(
                UserProfile.Gender.MALE to stringResource(R.string.onboarding_gender_male),
                UserProfile.Gender.FEMALE to stringResource(R.string.onboarding_gender_female),
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
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.onboarding_body_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = draft.dob,
            onValueChange = { value -> viewModel.update { it.copy(dob = value.take(10)) } },
            label = { Text(stringResource(R.string.onboarding_dob_label)) },
            placeholder = { Text("1998-05-21") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = height,
                onValueChange = { value ->
                    val cleaned = value.filter { it.isDigit() }.take(3)
                    onHeight(cleaned)
                    cleaned.toFloatOrNull()?.let { cm -> viewModel.update { it.copy(heightCm = cm) } }
                },
                label = { Text(stringResource(R.string.onboarding_height_label)) },
                suffix = { Text(stringResource(R.string.unit_cm)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { value ->
                    val cleaned = value.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                    onWeight(cleaned)
                    cleaned.toFloatOrNull()?.let { kg -> viewModel.update { it.copy(weightKg = kg) } }
                },
                label = { Text(stringResource(R.string.onboarding_weight_label)) },
                suffix = { Text(stringResource(R.string.unit_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepGoals(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.onboarding_goal_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_activity_label), style = MaterialTheme.typography.titleSmall)
        ChoiceColumn(
            options = listOf(
                UserProfile.Activity.SEDENTARY to stringResource(R.string.onboarding_activity_sedentary),
                UserProfile.Activity.LIGHT to stringResource(R.string.onboarding_activity_light),
                UserProfile.Activity.MODERATE to stringResource(R.string.onboarding_activity_moderate),
                UserProfile.Activity.ACTIVE to stringResource(R.string.onboarding_activity_active),
            ),
            selected = draft.activity,
            onSelect = { value -> viewModel.update { it.copy(activity = value) } },
        )
        Text(stringResource(R.string.onboarding_goal_label), style = MaterialTheme.typography.titleSmall)
        ChoiceRow(
            options = listOf(
                UserProfile.Goal.LOSE to stringResource(R.string.onboarding_goal_lose),
                UserProfile.Goal.MAINTAIN to stringResource(R.string.onboarding_goal_maintain),
                UserProfile.Goal.GAIN to stringResource(R.string.onboarding_goal_gain),
            ),
            selected = draft.goal,
            onSelect = { value -> viewModel.update { it.copy(goal = value) } },
        )
        OutlinedTextField(
            value = draft.allergies,
            onValueChange = { value -> viewModel.update { it.copy(allergies = value) } },
            label = { Text(stringResource(R.string.onboarding_allergies_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}

@Composable
private fun StepFinish(draft: OnboardingViewModel.Draft, age: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.onboarding_finish_title), style = MaterialTheme.typography.headlineMedium)
        SummaryPill("👤", draft.name.ifBlank { "—" })
        SummaryPill("🎂", if (age > 0) "$age" else "—")
        SummaryPill("📏", "${draft.heightCm.toInt()} ${stringResource(R.string.unit_cm)} · ${draft.weightKg.toInt()} ${stringResource(R.string.unit_kg)}")
    }
}

@Composable
private fun SummaryPill(icon: String, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(14.dp),
    ) { Text("$icon  $text", style = MaterialTheme.typography.titleSmall) }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun <T> ChoiceColumn(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
