package app.calsnap.android.presentation.screens.progress

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapPillTextField
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapProgressBar
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak
import app.calsnap.android.ui.theme.MacroWater
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val ProgressEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private data class DrinkButtonSpec(
    val icon: String,
    val labelRes: Int,
    val milliliters: Int,
)

private val ProgressDrinkButtons = listOf(
    DrinkButtonSpec("💧", R.string.progress_drink_water, 250),
    DrinkButtonSpec("🍵", R.string.progress_drink_tea, 200),
    DrinkButtonSpec("☕", R.string.progress_drink_coffee, 150),
    DrinkButtonSpec("🧃", R.string.progress_drink_juice, 200),
    DrinkButtonSpec("🥛", R.string.progress_drink_milk, 200),
    DrinkButtonSpec("🫗", R.string.progress_drink_other, 200),
)

@Composable
private fun ProgressCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(20.dp),
    elevation: Dp = 12.dp,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(0.5.dp, borderColor), shape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var showWeightSheet by remember { mutableStateOf(false) }
    val weightSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    CalSnapScreen(glow = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedSection(0) { ProgressHeader(ui) }
            AnimatedSection(1) { StreakWeekCard(ui) }
            AnimatedSection(2) { BmiCard(ui.bmi) }
            AnimatedSection(3) { StatsGrid(ui) }
            AnimatedSection(4) { WaterCard(ui = ui, onAdd = viewModel::addWater, onUndo = viewModel::undoLastWater) }
            AnimatedSection(5) { HeatmapCard(ui.days, ui.profile?.kcalGoal ?: 2000) }
            AnimatedSection(6) {
                WeightCard(
                    ui = ui,
                    onLogWeight = {
                        viewModel.prepareWeightDraft()
                        showWeightSheet = true
                    },
                )
            }
        }
    }

    if (showWeightSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWeightSheet = false },
            sheetState = weightSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            WeightLogSheet(
                draft = ui.weightDraft,
                latestWeight = ui.latestWeightKg,
                delta = ui.weightDraft.toFloatOrNull()?.let { draft -> ui.latestWeightKg?.let { draft - it } },
                onDraft = viewModel::updateWeightDraft,
                onStep = viewModel::stepWeight,
                onSave = {
                    viewModel.saveWeight()
                    showWeightSheet = false
                },
                onCancel = { showWeightSheet = false },
            )
        }
    }
}

@Composable
private fun ProgressHeader(ui: ProgressViewModel.UiState) {
    val month = remember {
        DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()).format(LocalDate.now())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.progress_title),
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                month,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${ui.days.count { it.hasLog }} / 28",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)),
            )
        }
    }
}

@Composable
private fun StreakWeekCard(ui: ProgressViewModel.UiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(36.dp), clip = false)
            .clip(RoundedCornerShape(36.dp))
            .background(Brush.linearGradient(listOf(CalSnapStreak, Color(0xFFC84000))))
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
                Text("🔥", style = TextStyle(fontSize = 42.sp))
                Text(
                    ui.streak.toString(),
                    style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-3).sp, color = Color.White),
                )
                Text(
                    stringResource(R.string.progress_streak_label),
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = Color.White.copy(alpha = 0.72f)),
                    textAlign = TextAlign.Center,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.progress_this_week),
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.68f)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    ui.weekDays.forEach { day -> WeekDot(day, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun WeekDot(day: ProgressViewModel.WeekDay, modifier: Modifier = Modifier) {
    val label = remember(day.date) { DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(day.date).uppercase(Locale.getDefault()) }
    val bg = when {
        day.hasLog -> Color.White.copy(alpha = 0.88f)
        else -> Color.White.copy(alpha = 0.16f)
    }
    val fg = if (day.hasLog) Color(0xFFC84000) else Color.White.copy(alpha = 0.52f)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                BorderStroke(if (day.isToday && !day.hasLog) 2.dp else 0.dp, if (day.isToday) Color.White.copy(alpha = 0.65f) else Color.Transparent),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, color = fg))
    }
}

@Composable
private fun BmiCard(bmi: Float?) {
    ProgressCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        padding = PaddingValues(20.dp),
        elevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(0.86f)) {
                Text(
                    stringResource(R.string.progress_bmi_label).uppercase(Locale.getDefault()),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.46f)),
                )
                Spacer(Modifier.height(4.dp))
                val animatedBmi by animateFloatAsState(
                    targetValue = bmi ?: 0f,
                    animationSpec = tween(800, easing = ProgressEaseOut),
                    label = "progressBmi",
                )
                Text(
                    if (bmi == null) "—" else String.format(Locale.US, "%.1f", animatedBmi),
                    style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-2.5).sp, color = MaterialTheme.colorScheme.onSurface),
                )
                BmiCategoryPill(bmi)
            }
            BmiScale(bmi, modifier = Modifier.weight(1.25f))
        }
    }
}

@Composable
private fun BmiCategoryPill(bmi: Float?) {
    val (text, bg, fg) = when {
        bmi == null -> Triple(stringResource(R.string.progress_bmi_no_data), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), MaterialTheme.colorScheme.onSurfaceVariant)
        bmi < 18.5f -> Triple(stringResource(R.string.progress_bmi_low), Color(0xFF3B82F6).copy(alpha = 0.12f), Color(0xFF1D4ED8))
        bmi < 25f -> Triple(stringResource(R.string.progress_bmi_normal), Color(0xFF16A34A).copy(alpha = 0.12f), Color(0xFF16A34A))
        bmi < 30f -> Triple(stringResource(R.string.progress_bmi_high), Color(0xFFF59E0B).copy(alpha = 0.14f), Color(0xFFB45309))
        else -> Triple(stringResource(R.string.progress_bmi_very_high), Color(0xFFEF4444).copy(alpha = 0.13f), Color(0xFFC62626))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg))
    }
}

@Composable
private fun BmiScale(bmi: Float?, modifier: Modifier = Modifier) {
    val targetPct = (((bmi ?: 16f) - 16f) / (40f - 16f)).coerceIn(0.04f, 0.96f)
    val pct by animateFloatAsState(targetValue = targetPct, animationSpec = tween(800, easing = ProgressEaseOut), label = "bmiNeedle")
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF16A34A), Color(0xFFF59E0B), Color(0xFFEF4444)))),
            )
            Box(modifier = Modifier.fillMaxWidth(pct).height(20.dp), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                        .shadow(5.dp, CircleShape, clip = false),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("16", "18.5", "25", "30", "40").forEach {
                Text(it, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)))
            }
        }
    }
}

@Composable
private fun StatsGrid(ui: ProgressViewModel.UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(stringResource(R.string.progress_stat_avg_kcal), ui.averageCalories7?.toString() ?: "—", stringResource(R.string.progress_stat_for_7_days), Modifier.weight(1f))
            StatCard(stringResource(R.string.progress_stat_best_day), ui.bestDay?.let { formatShortDate(it) } ?: "—", stringResource(R.string.progress_stat_closest_to_goal), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(stringResource(R.string.progress_stat_records), ui.totalEntries.toString(), stringResource(R.string.progress_stat_total_meals), Modifier.weight(1f))
            StatCard(stringResource(R.string.progress_stat_days_with_data), ui.daysWithData30.toString(), stringResource(R.string.progress_stat_of_30), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    ProgressCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        padding = PaddingValues(16.dp),
        elevation = 9.dp,
    ) {
        Text(label.uppercase(Locale.getDefault()), style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        Spacer(Modifier.height(8.dp))
        Text(value, style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.4).sp, color = MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f)))
    }
}

@Composable
private fun WaterCard(
    ui: ProgressViewModel.UiState,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
) {
    val progress = (ui.waterMl.toFloat() / ui.waterGoalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val waterDisplay by animateIntAsState(
        targetValue = ui.waterMl,
        animationSpec = tween(520, easing = ProgressEaseOut),
        label = "progressWater",
    )
    ProgressCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        padding = PaddingValues(0.dp),
        elevation = 12.dp,
        borderColor = MacroWater.copy(alpha = 0.16f),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(MacroWater.copy(alpha = 0.08f), Color.Transparent)))
                    .padding(20.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(MacroWater))
                            Text(
                                stringResource(R.string.progress_water_balance).uppercase(Locale.getDefault()),
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)),
                            )
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                waterDisplay.toString(),
                                style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-3).sp, color = MaterialTheme.colorScheme.onSurface),
                            )
                            Text(
                                stringResource(R.string.unit_ml),
                                modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
                                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            )
                        }
                        Text(
                            "${stringResource(R.string.progress_water_of)} ${ui.waterGoalMl} ${stringResource(R.string.unit_ml)}",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f)),
                        )
                    }
                    WaterRing(progress)
                }
                Spacer(Modifier.height(14.dp))
                WaterWave(progress)
            }
            Column(Modifier.padding(horizontal = 18.dp)) {
                ProgressDrinkButtons.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { spec ->
                            DrinkButton(spec = spec, onClick = { onAdd(spec.milliliters) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
                if (ui.waterHasSalt) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.13f))
                            .border(BorderStroke(0.5.dp, Color(0xFFB45309).copy(alpha = 0.20f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.progress_water_salt_hint, ui.waterGoalMl),
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309)),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            WaterTimeline(events = ui.waterEvents, onUndo = onUndo)
        }
    }
}

@Composable
private fun WaterRing(progress: Float) {
    val animated by animateFloatAsState(targetValue = progress, animationSpec = tween(900, easing = ProgressEaseOut), label = "waterRing")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
        Canvas(Modifier.size(72.dp)) {
            val stroke = 5.dp.toPx()
            val ringSize = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2)
            drawCircle(MacroWater.copy(alpha = 0.12f), radius = size.minDimension / 2f - stroke, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                color = MacroWater,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = ringSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animated * 100).roundToInt()}%", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Black, color = MacroWater))
            Text(stringResource(R.string.progress_water_goal_word), style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)))
        }
    }
}

@Composable
private fun WaterWave(progress: Float) {
    CalSnapProgressBar(
        progress = progress,
        color = if (progress >= 1f) Color(0xFF22C55E) else MacroWater,
        track = MaterialTheme.colorScheme.onSurface.copy(alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.25f) 0.10f else 0.06f),
        height = 6.dp,
    )
}

@Composable
private fun DrinkButton(spec: DrinkButtonSpec, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)), RoundedCornerShape(14.dp))
            .calSnapClickable(pressedScale = 0.82f, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(spec.icon, style = TextStyle(fontSize = 20.sp))
        Text(
            stringResource(spec.labelRes),
            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("+${spec.milliliters}", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, color = MacroWater))
    }
}

@Composable
private fun WaterTimeline(events: List<ProgressViewModel.WaterEvent>, onUndo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.13f)), RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .padding(horizontal = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 11.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.progress_water_history_today).uppercase(Locale.getDefault()),
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)),
            )
            if (events.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .calSnapClickable(pressedScale = 0.90f, onClick = onUndo)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("↩", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MacroWater))
                    Text(stringResource(R.string.progress_water_undo), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MacroWater))
                }
            }
        }
        if (events.isEmpty()) {
            Text(
                stringResource(R.string.progress_water_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 14.dp),
                style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
                textAlign = TextAlign.Center,
            )
        } else {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                events.asReversed().take(8).forEach { event -> WaterEventChip(event) }
            }
        }
    }
}

@Composable
private fun WaterEventChip(event: ProgressViewModel.WaterEvent) {
    Column(
        modifier = Modifier
            .widthIn(min = 58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MacroWater.copy(alpha = 0.07f))
            .border(BorderStroke(0.5.dp, MacroWater.copy(alpha = 0.12f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("💧", style = TextStyle(fontSize = 16.sp))
        Text("${event.milliliters} ${stringResource(R.string.unit_ml)}", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black, color = MacroWater))
        Text(formatTime(event.loggedAt), style = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)))
    }
}

@Composable
private fun HeatmapCard(days: List<ProgressViewModel.DaySummary>, goal: Int) {
    ProgressCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), padding = PaddingValues(20.dp), elevation = 12.dp) {
        Text(stringResource(R.string.progress_heatmap), style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp, color = MaterialTheme.colorScheme.onSurface))
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val ratio = day.calories.toFloat() / goal.coerceAtLeast(1).toFloat()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(heatColor(ratio, day.hasLog)),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            HeatLegendItem(heatColor(0f, false), stringResource(R.string.progress_heat_none))
            HeatLegendItem(CalSnapStreak.copy(alpha = 0.20f), stringResource(R.string.progress_heat_low))
            HeatLegendItem(CalSnapStreak.copy(alpha = 0.62f), stringResource(R.string.progress_heat_normal))
            HeatLegendItem(CalSnapStreak, stringResource(R.string.progress_heat_goal))
            HeatLegendItem(Color(0xFFEF4444), stringResource(R.string.progress_heat_over))
        }
    }
}

@Composable
private fun HeatLegendItem(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color).border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)), RoundedCornerShape(3.dp)))
        Text(label, style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)))
    }
}

@Composable
private fun WeightCard(ui: ProgressViewModel.UiState, onLogWeight: () -> Unit) {
    ProgressCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), padding = PaddingValues(20.dp), elevation = 12.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.progress_weight_dynamics), style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp, color = MaterialTheme.colorScheme.onSurface))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f))
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)), RoundedCornerShape(24.dp))
                    .calSnapClickable(pressedScale = 0.92f, onClick = onLogWeight)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(stringResource(R.string.progress_weight_log), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
            }
        }
        Spacer(Modifier.height(18.dp))
        if (ui.weightPoints.size < 2) {
            Text(
                stringResource(R.string.progress_weight_empty_daily),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
                textAlign = TextAlign.Center,
            )
        } else {
            WeightChart(ui.weightPoints)
        }
        ui.weightDeltaKg?.let { delta ->
            Spacer(Modifier.height(14.dp))
            PaceCard(delta)
        }
    }
}

@Composable
private fun WeightChart(points: List<ProgressViewModel.WeightPoint>) {
    val chartPoints = points.takeLast(30)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.08f else 0.05f)
    val lineColor = MaterialTheme.colorScheme.onSurface
    val fillTop = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.12f else 0.07f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        if (chartPoints.size < 2) return@Canvas
        val padLeft = 18.dp.toPx()
        val padRight = 18.dp.toPx()
        val padTop = 14.dp.toPx()
        val padBottom = 22.dp.toPx()
        val values = chartPoints.map { it.weightKg }
        val min = values.minOrNull() ?: return@Canvas
        val max = values.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0.05f } ?: 1f
        val minP = min - range * 0.15f
        val maxP = max + range * 0.15f
        val plotW = size.width - padLeft - padRight
        val plotH = size.height - padTop - padBottom
        fun x(index: Int) = padLeft + (index / (chartPoints.lastIndex.toFloat())) * plotW
        fun y(value: Float) = padTop + plotH - ((value - minP) / (maxP - minP)) * plotH
        repeat(4) { step ->
            val yy = padTop + plotH * step / 3f
            drawLine(gridColor, Offset(padLeft, yy), Offset(size.width - padRight, yy), strokeWidth = 1.dp.toPx())
        }
        val coords = values.mapIndexed { index, value -> Offset(x(index), y(value)) }
        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            coords.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fillPath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            coords.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(coords.last().x, size.height - padBottom)
            lineTo(coords.first().x, size.height - padBottom)
            close()
        }
        drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(fillTop, Color.Transparent)))
        drawPath(path = linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        coords.forEach { point ->
            drawCircle(surfaceColor, radius = 5.dp.toPx(), center = point)
            drawCircle(lineColor, radius = 3.5.dp.toPx(), center = point)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        chartPoints.firstOrNull()?.let { point -> Text("${point.weightKg.formatKg()} · ${formatShortDate(point.date)}", style = weightAxisStyle()) }
        chartPoints.lastOrNull()?.let { point -> Text("${point.weightKg.formatKg()} · ${formatShortDate(point.date)}", style = weightAxisStyle()) }
    }
}

@Composable
private fun weightAxisStyle() = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f))

@Composable
private fun PaceCard(delta: Float) {
    val (icon, text, color) = when {
        delta > 0.05f -> Triple("↗️", stringResource(R.string.progress_weight_delta_up, delta), Color(0xFFB45309))
        delta < -0.05f -> Triple("↘️", stringResource(R.string.progress_weight_delta_down, delta), Color(0xFF16A34A))
        else -> Triple("→", stringResource(R.string.progress_weight_delta_same), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, style = TextStyle(fontSize = 24.sp))
        Column {
            Text(stringResource(R.string.progress_weight_pace_title), style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface))
            Text(text, style = TextStyle(fontSize = 13.sp, color = color, lineHeight = 18.sp))
        }
    }
}

@Composable
private fun WeightLogSheet(
    draft: String,
    latestWeight: Float?,
    delta: Float?,
    onDraft: (String) -> Unit,
    onStep: (Float) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .padding(horizontal = 22.dp)
            .padding(top = 4.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.progress_weight_log_title), style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.7).sp, color = MaterialTheme.colorScheme.onSurface))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.progress_weight_log_sub), style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundStepButton("−", onClick = { onStep(-0.1f) })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(draft.ifBlank { latestWeight?.formatKg() ?: "70.0" }, style = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp, color = MaterialTheme.colorScheme.onSurface))
                Text(stringResource(R.string.unit_kg), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            RoundStepButton("+", onClick = { onStep(0.1f) })
        }
        Spacer(Modifier.height(14.dp))
        delta?.let {
            Text(weightTrendText(it), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant))
            Spacer(Modifier.height(12.dp))
        }
        CalSnapPillTextField(
            value = draft,
            onValueChange = onDraft,
            placeholder = stringResource(R.string.progress_weight_input),
            suffix = stringResource(R.string.unit_kg),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        CalSnapPrimaryButton(
            onClick = onSave,
            enabled = draft.toFloatOrNull() != null,
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
        ) {
            Text(stringResource(R.string.progress_save), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(10.dp))
        CalSnapSecondaryButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), height = 52.dp) {
            Text(stringResource(R.string.cancel), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun RoundStepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface)
            .calSnapClickable(pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.background))
    }
}

@Composable
private fun weightTrendText(delta: Float): String = when {
    delta > 0.05f -> stringResource(R.string.progress_weight_delta_up, delta)
    delta < -0.05f -> stringResource(R.string.progress_weight_delta_down, delta)
    else -> stringResource(R.string.progress_weight_delta_same)
}

@Composable
private fun heatColor(ratio: Float, hasLog: Boolean): Color = when {
    !hasLog -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    ratio > 1.50f -> Color(0xFFC62626)
    ratio > 1.25f -> Color(0xFFEF4444).copy(alpha = 0.72f)
    ratio > 1.10f -> Color(0xFFEF4444).copy(alpha = 0.48f)
    ratio > 1.00f -> Color(0xFFEF4444).copy(alpha = 0.22f)
    ratio > 0.90f -> CalSnapStreak
    ratio > 0.60f -> CalSnapStreak.copy(alpha = 0.68f)
    ratio > 0.30f -> CalSnapStreak.copy(alpha = 0.38f)
    else -> CalSnapStreak.copy(alpha = 0.20f)
}

private fun formatTime(epochMs: Long): String = runCatching {
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}.getOrDefault("—")

private fun formatShortDate(date: LocalDate): String = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()).format(date)

private fun Float.formatKg(): String = String.format(Locale.US, "%.1f", this)
