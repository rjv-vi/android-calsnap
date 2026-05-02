package app.calsnap.android.presentation.screens.progress

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.progress_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = stringResource(R.string.progress_streak),
                    value = "${ui.streak}",
                    subtitle = stringResource(R.string.progress_days),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = stringResource(R.string.progress_bmi),
                    value = ui.bmi?.let { "%.1f".format(it) } ?: "—",
                    subtitle = bmiLabel(ui.bmi),
                    modifier = Modifier.weight(1f),
                )
            }

            WaterCard(ui.waterMl, ui.waterGoalMl, viewModel::addWater)
            HeatmapCard(ui.days, ui.profile?.kcalGoal ?: 2000)
            WeightCard(
                draft = ui.weightDraft,
                latest = ui.weights.firstOrNull()?.weightKg,
                onDraft = viewModel::updateWeightDraft,
                onSave = viewModel::saveWeight,
            )
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.displaySmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaterCard(waterMl: Int, goalMl: Int, onAdd: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.progress_water), style = MaterialTheme.typography.titleMedium)
                Text("$waterMl / $goalMl ${stringResource(R.string.unit_ml)}")
            }
            LinearProgressIndicator(
                progress = { (waterMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(250, 500, 750).forEach { ml ->
                    Button(onClick = { onAdd(ml) }, modifier = Modifier.weight(1f)) {
                        Text("+$ml")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(days: List<ProgressViewModel.DaySummary>, goal: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.progress_heatmap), style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                days.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { day ->
                            val progress = (day.calories.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(heatColor(progress, day.hasLog)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightCard(
    draft: String,
    latest: Float?,
    onDraft: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.progress_weight), style = MaterialTheme.typography.titleMedium)
            Text(
                text = latest?.let { stringResource(R.string.progress_weight_latest, it) }
                    ?: stringResource(R.string.progress_weight_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraft,
                    label = { Text(stringResource(R.string.progress_weight_input)) },
                    suffix = { Text(stringResource(R.string.unit_kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onSave,
                    enabled = draft.toFloatOrNull() != null,
                    modifier = Modifier.width(112.dp),
                ) {
                    Text(stringResource(R.string.progress_save))
                }
            }
        }
    }
}

@Composable
private fun heatColor(progress: Float, hasLog: Boolean): Color = when {
    !hasLog -> MaterialTheme.colorScheme.surfaceVariant
    progress >= 0.95f -> MaterialTheme.colorScheme.primary
    progress >= 0.65f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
}

@Composable
private fun bmiLabel(bmi: Float?): String = when {
    bmi == null -> "—"
    bmi < 18.5f -> stringResource(R.string.progress_bmi_low)
    bmi < 25f -> stringResource(R.string.progress_bmi_normal)
    bmi < 30f -> stringResource(R.string.progress_bmi_high)
    else -> stringResource(R.string.progress_bmi_very_high)
}
