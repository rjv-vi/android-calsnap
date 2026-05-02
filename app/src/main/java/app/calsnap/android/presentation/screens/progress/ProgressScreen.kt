package app.calsnap.android.presentation.screens.progress

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineLarge)
            HeroStats(ui)
            WaterCard(ui.waterMl, ui.waterGoalMl, viewModel::addWater)
            HeatmapCard(ui.days, ui.profile?.kcalGoal ?: 2000)
            WeightCard(
                draft = ui.weightDraft,
                latest = ui.weights.firstOrNull()?.weightKg,
                onDraft = viewModel::updateWeightDraft,
                onSave = viewModel::saveWeight,
            )
            Spacer(Modifier.height(84.dp))
        }
    }
}

@Composable
private fun HeroStats(ui: ProgressViewModel.UiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StreakRing(ui.streak)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallMetric("⚖️", stringResource(R.string.progress_bmi), ui.bmi?.let { "%.1f".format(it) } ?: "—", bmiLabel(ui.bmi))
                SmallMetric("🔥", stringResource(R.string.progress_streak), "${ui.streak}", stringResource(R.string.progress_days))
            }
        }
    }
}

@Composable
private fun StreakRing(streak: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(128.dp)) {
        Canvas(Modifier.size(128.dp)) {
            val stroke = 13.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            drawCircle(track, radius = radius, center = center, style = Stroke(stroke))
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * ((streak.coerceAtMost(14)) / 14f),
                useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2),
                style = Stroke(stroke),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$streak", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.progress_days), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SmallMetric(icon: String, title: String, value: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Text(icon) }
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaterCard(waterMl: Int, goalMl: Int, onAdd: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("💧 ${stringResource(R.string.progress_water)}", style = MaterialTheme.typography.titleMedium)
                Text("$waterMl / $goalMl ${stringResource(R.string.unit_ml)}", fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { (waterMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(250, 500, 750).forEach { ml ->
                    Button(onClick = { onAdd(ml) }, modifier = Modifier.weight(1f)) { Text("+$ml") }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(days: List<ProgressViewModel.DaySummary>, goal: Int) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🗓️ ${stringResource(R.string.progress_heatmap)}", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                days.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        week.forEach { day ->
                            val progress = (day.calories.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(11.dp))
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
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📈 ${stringResource(R.string.progress_weight)}", style = MaterialTheme.typography.titleMedium)
            Text(
                text = latest?.let { stringResource(R.string.progress_weight_latest, it) }
                    ?: stringResource(R.string.progress_weight_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.width(112.dp).height(56.dp),
                ) { Text(stringResource(R.string.progress_save)) }
            }
        }
    }
}

@Composable
private fun heatColor(progress: Float, hasLog: Boolean): Color = when {
    !hasLog -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    progress >= 0.95f -> MaterialTheme.colorScheme.primary
    progress >= 0.65f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
}

@Composable
private fun bmiLabel(bmi: Float?): String = when {
    bmi == null -> "—"
    bmi < 18.5f -> stringResource(R.string.progress_bmi_low)
    bmi < 25f -> stringResource(R.string.progress_bmi_normal)
    bmi < 30f -> stringResource(R.string.progress_bmi_high)
    else -> stringResource(R.string.progress_bmi_very_high)
}
