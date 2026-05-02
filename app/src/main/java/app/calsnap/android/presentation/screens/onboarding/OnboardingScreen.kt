package app.calsnap.android.presentation.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.domain.BmrCalculator

/**
 * v1 stub: a single text field + finish button so the flow compiles and
 * users can get into Home. The full 5-step drum-picker flow lives in
 * TASKS.md § Onboarding and lands in a follow-up session.
 */
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_sub),
            style = MaterialTheme.typography.bodyMedium,
        )

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
                    height = value.filter { it.isDigit() }.take(3)
                    height.toFloatOrNull()?.let { cm -> viewModel.update { it.copy(heightCm = cm) } }
                },
                label = { Text(stringResource(R.string.onboarding_height_label)) },
                suffix = { Text(stringResource(R.string.unit_cm)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { value ->
                    weight = value.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                    weight.toFloatOrNull()?.let { kg -> viewModel.update { it.copy(weightKg = kg) } }
                },
                label = { Text(stringResource(R.string.onboarding_weight_label)) },
                suffix = { Text(stringResource(R.string.unit_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

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

        Button(
            onClick = { viewModel.finish(onFinished) },
            enabled = canFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_finish))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
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
