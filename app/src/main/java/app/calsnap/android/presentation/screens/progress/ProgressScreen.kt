package app.calsnap.android.presentation.screens.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapIconTile
import app.calsnap.android.presentation.components.CalSnapPill
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapProgressBar
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapTextField
import app.calsnap.android.ui.theme.CalSnapStreak
import app.calsnap.android.ui.theme.MacroWater

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    CalSnapScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedSection(0) { ProgressHeader(ui) }
            AnimatedSection(1) { HeroStats(ui) }
            AnimatedSection(2) { WaterCard(ui.waterMl, ui.waterGoalMl, viewModel::addWater) }
            AnimatedSection(3) { HeatmapCard(ui.days, ui.profile?.kcalGoal ?: 2000) }
            AnimatedSection(4) {
                WeightCard(
                    draft = ui.weightDraft,
                    latest = ui.weights.firstOrNull()?.weightKg,
                    onDraft = viewModel::updateWeightDraft,
                    onSave = viewModel::saveWeight,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProgressHeader(ui: ProgressViewModel.UiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(
                text = "${ui.days.count { it.hasLog }} / 28",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
        CalSnapPill(text = "${ui.streak} ${stringResource(R.string.progress_days)}", selected = true, icon = "🔥")
    }
}

@Composable
private fun HeroStats(ui: ProgressViewModel.UiState) {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        padding = PaddingValues(18.dp),
        elevation = 20.dp,
        containerBrush = Brush.verticalGradient(
            listOf(CalSnapStreak.copy(alpha = 0.16f), MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        ),
        borderColor = CalSnapStreak.copy(alpha = 0.22f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
    val target = (streak.coerceAtMost(14)) / 14f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(820, easing = FastOutSlowInEasing),
        label = "streakRing",
    )
    val track = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
        Canvas(Modifier.size(132.dp)) {
            val stroke = 13.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            drawCircle(track, radius = radius, center = center, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                color = CalSnapStreak,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$streak", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.progress_days), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmallMetric(icon: String, title: String, value: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalSnapIconTile(icon = icon, size = 42.dp, background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaterCard(waterMl: Int, goalMl: Int, onAdd: (Int) -> Unit) {
    val waterDisplay by animateIntAsState(
        targetValue = waterMl,
        animationSpec = tween(460, easing = FastOutSlowInEasing),
        label = "progressWater",
    )
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        padding = PaddingValues(16.dp),
        elevation = 14.dp,
        containerBrush = Brush.verticalGradient(listOf(MacroWater.copy(alpha = 0.14f), MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))),
        borderColor = MacroWater.copy(alpha = 0.20f),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalSnapIconTile(icon = "💧", size = 46.dp, background = MacroWater.copy(alpha = 0.13f))
                Column {
                    Text(stringResource(R.string.progress_water), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("$waterDisplay / $goalMl ${stringResource(R.string.unit_ml)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            CalSnapPill(text = "${((waterMl.toFloat() / goalMl.coerceAtLeast(1).toFloat()) * 100).toInt()}%", selected = false)
        }
        Spacer(Modifier.height(14.dp))
        CalSnapProgressBar(
            progress = waterMl.toFloat() / goalMl.coerceAtLeast(1).toFloat(),
            color = MacroWater,
            height = 10.dp,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(250, 500, 750).forEach { ml ->
                CalSnapSecondaryButton(onClick = { onAdd(ml) }, modifier = Modifier.weight(1f), height = 48.dp) {
                    Text("+$ml")
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(days: List<ProgressViewModel.DaySummary>, goal: Int) {
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), padding = PaddingValues(16.dp), elevation = 14.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🗓️ ${stringResource(R.string.progress_heatmap)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("${days.count { it.hasLog }}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val progress = (day.calories.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(heatColor(progress, day.hasLog)),
                        )
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
    CalSnapCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), padding = PaddingValues(16.dp), elevation = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = "📈", size = 46.dp, background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.progress_weight), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    text = latest?.let { stringResource(R.string.progress_weight_latest, it) }
                        ?: stringResource(R.string.progress_weight_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CalSnapTextField(
                value = draft,
                onValueChange = onDraft,
                label = stringResource(R.string.progress_weight_input),
                suffix = { Text(stringResource(R.string.unit_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            CalSnapPrimaryButton(
                onClick = onSave,
                enabled = draft.toFloatOrNull() != null,
                modifier = Modifier.width(112.dp),
                height = 58.dp,
            ) {
                Text(stringResource(R.string.progress_save))
            }
        }
    }
}

@Composable
private fun heatColor(progress: Float, hasLog: Boolean): Color = when {
    !hasLog -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    progress >= 0.95f -> CalSnapStreak
    progress >= 0.65f -> CalSnapStreak.copy(alpha = 0.72f)
    else -> CalSnapStreak.copy(alpha = 0.34f)
}

@Composable
private fun bmiLabel(bmi: Float?): String = when {
    bmi == null -> "—"
    bmi < 18.5f -> stringResource(R.string.progress_bmi_low)
    bmi < 25f -> stringResource(R.string.progress_bmi_normal)
    bmi < 30f -> stringResource(R.string.progress_bmi_high)
    else -> stringResource(R.string.progress_bmi_very_high)
}
