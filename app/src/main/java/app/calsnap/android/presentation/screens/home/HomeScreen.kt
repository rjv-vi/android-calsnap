package app.calsnap.android.presentation.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.MealType
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.calSnapClickable
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@Composable
fun HomeScreen(
    onAddFood: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val goal = ui.profile?.kcalGoal ?: 2000

    CalSnapScreen(glow = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
        ) {
            AnimatedSection(0) { HomeHeader(ui) }
            if (!ui.hasApiKey) {
                AnimatedSection(1) { ApiMissingBar() }
            }
            AnimatedSection(2) { CalendarStrip(ui.calendarDays, ui.selectedDay, goal, viewModel::selectDay) }
            AnimatedSection(3) {
                CalorieHeroCard(
                    eaten = ui.totalCalories,
                    goal = goal,
                    proteinEaten = ui.totalProtein,
                    proteinGoal = ui.profile?.proteinGoal ?: 100,
                    carbsEaten = ui.totalCarbs,
                    carbsGoal = ui.profile?.carbsGoal ?: 250,
                    fatEaten = ui.totalFat,
                    fatGoal = ui.profile?.fatGoal ?: 60,
                    waterMl = ui.waterMl,
                    waterGoalMl = ui.waterGoalMl,
                )
            }
            AnimatedSection(4) { TodaySection(ui, onAddFood) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ApiMissingBar() {
    val warn = homeWarnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(warn.copy(alpha = 0.07f))
            .border(BorderStroke(1.5.dp, warn.copy(alpha = 0.18f)), RoundedCornerShape(20.dp))
            .calSnapClickable(pressedScale = 0.98f, onClick = {})
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🔑", style = TextStyle(fontSize = 22.sp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_api_bar_title),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = warn),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.home_api_bar_sub),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = warn.copy(alpha = 0.60f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Text("›", style = TextStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface))
    }
}

@Composable
private fun HomeHeader(ui: HomeViewModel.UiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = timeGreeting(),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
                ),
                maxLines = 1,
            )
            Text(
                text = (ui.profile?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)) + "!",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.8).sp,
                    lineHeight = 35.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StreakPill(ui.calendarDays)
    }
}

@Composable
private fun StreakPill(days: List<HomeViewModel.CalendarDay>) {
    val dark = isHomeDark()
    val streakColor = if (dark) Color(0xFFFF6618) else Color(0xFFFF5500)
    val streak = days.reversed().takeWhile { it.hasLog }.count()
    Row(
        modifier = Modifier
            .shadow(12.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(28.dp))
            .background(streakColor.copy(alpha = if (dark) 0.12f else 0.10f))
            .border(
                BorderStroke(1.5.dp, streakColor.copy(alpha = if (dark) 0.28f else 0.22f)),
                RoundedCornerShape(28.dp),
            )
            .calSnapClickable(pressedScale = 0.91f, onClick = {})
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("🔥", style = TextStyle(fontSize = 16.sp))
        Text(
            "$streak",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = streakColor,
            ),
        )
        Text(
            stringResource(R.string.progress_days),
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = streakColor.copy(alpha = 0.80f)),
        )
    }
}

@Composable
private fun timeGreeting(): String {
    val hour = LocalTime.now().hour
    return stringResource(
        when {
            hour < 6 -> R.string.greet_night
            hour < 12 -> R.string.greet_morning
            hour < 18 -> R.string.greet_day
            else -> R.string.greet_evening
        },
    )
}

@Composable
private fun CalendarStrip(
    days: List<HomeViewModel.CalendarDay>,
    selected: LocalDate,
    goal: Int,
    onSelect: (LocalDate) -> Unit,
) {
    val scroll = rememberScrollState()
    LaunchedEffect(days.size) {
        delay(30)
        scroll.scrollTo(scroll.maxValue)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            CalendarDayChip(
                day = day,
                selected = day.date == selected,
                overGoal = day.calories > goal * 1.05f,
                onClick = { onSelect(day.date) },
            )
        }
    }
}

@Composable
private fun CalendarDayChip(
    day: HomeViewModel.CalendarDay,
    selected: Boolean,
    overGoal: Boolean,
    onClick: () -> Unit,
) {
    val today = day.date == LocalDate.now()
    val ok = homeOkColor()
    val err = homeErrorColor()
    val circleColor = when {
        today -> MaterialTheme.colorScheme.onSurface
        selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = homeF1Alpha())
        day.hasLog && overGoal -> err.copy(alpha = 0.10f)
        day.hasLog -> ok.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val textColor = when {
        today -> MaterialTheme.colorScheme.background
        selected -> MaterialTheme.colorScheme.onSurface
        day.hasLog && overGoal -> err
        day.hasLog -> ok
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())
    }
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(width = 40.dp, height = 62.dp)
            .calSnapClickable(pressedScale = 0.86f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = weekdayLabel(day.date),
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha()),
            ),
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(if (today) Modifier.shadow(10.dp, RoundedCornerShape(17.dp), clip = false) else Modifier)
                .clip(RoundedCornerShape(17.dp))
                .background(circleColor)
                .then(
                    if (selected && !today) {
                        Modifier.border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())),
                            RoundedCornerShape(17.dp),
                        )
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (today || selected || day.hasLog) FontWeight.Black else FontWeight.Medium,
                    color = textColor,
                ),
            )
        }
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    when {
                        day.hasLog && overGoal -> err
                        day.hasLog -> ok
                        else -> Color.Transparent
                    },
                ),
        )
    }
}

@Composable
private fun CalorieHeroCard(
    eaten: Int,
    goal: Int,
    proteinEaten: Float,
    proteinGoal: Int,
    carbsEaten: Float,
    carbsGoal: Int,
    fatEaten: Float,
    fatGoal: Int,
    waterMl: Int,
    waterGoalMl: Int,
) {
    val remaining = goal - eaten
    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp)
            .shadow(24.dp, shape, clip = false)
            .clip(shape)
            .background(homeSurfaceColor())
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())), shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isHomeDark()) 0.08f else 0.75f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$eaten",
                            style = TextStyle(
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-4).sp,
                                lineHeight = 58.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Text(
                            "/",
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha()),
                            ),
                            modifier = Modifier.padding(start = 3.dp, end = 5.dp, bottom = 8.dp),
                        )
                        Text(
                            "$goal",
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
                            ),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_calories_eaten_label),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = homeF2Alpha()))
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())),
                                RoundedCornerShape(28.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            if (remaining >= 0) stringResource(R.string.home_remaining, remaining)
                            else stringResource(R.string.home_exceeded, -remaining),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
                CalorieRing(eaten = eaten, goal = goal)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroMiniTile(stringResource(R.string.macro_protein), proteinEaten, proteinGoal.toFloat(), macroProteinColor(), Modifier.weight(1f))
                MacroMiniTile(stringResource(R.string.macro_carbs), carbsEaten, carbsGoal.toFloat(), macroCarbsColor(), Modifier.weight(1f))
                MacroMiniTile(stringResource(R.string.macro_fat), fatEaten, fatGoal.toFloat(), macroFatColor(), Modifier.weight(1f))
            }
            WaterStrip(waterMl = waterMl, goalMl = waterGoalMl)
        }
    }
}

@Composable
private fun MacroMiniTile(label: String, value: Float, goal: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(homeSurface2Color())
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())), RoundedCornerShape(18.dp))
            .padding(start = 12.dp, end = 12.dp, top = 13.dp, bottom = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                "${value.toInt()}${stringResource(R.string.unit_g)}",
                color = color,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
            )
            Text(
                "${goal.toInt()}${stringResource(R.string.unit_g)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha()),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }
        Spacer(Modifier.height(8.dp))
        HomeLinearProgress(progress = value / goal.coerceAtLeast(1f), color = color, height = 3.dp)
        Text(
            label.uppercase(),
            modifier = Modifier.padding(top = 6.dp),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
            ),
        )
    }
}

@Composable
private fun WaterStrip(waterMl: Int, goalMl: Int) {
    val waterDisplay by animateIntAsState(
        targetValue = waterMl,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "homeWaterInline",
    )
    val water = Color(0xFF3B82F6)
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(water.copy(alpha = 0.07f))
            .calSnapClickable(pressedScale = 0.98f, onClick = {})
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("💧", style = TextStyle(fontSize = 13.sp))
        HomeLinearProgress(
            progress = waterMl.toFloat() / goalMl.coerceAtLeast(1).toFloat(),
            color = water,
            height = 5.dp,
            trackColor = water.copy(alpha = 0.15f),
            modifier = Modifier.weight(1f),
        )
        Text(
            "$waterDisplay / $goalMl ${stringResource(R.string.unit_ml)}",
            color = water,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun CalorieRing(eaten: Int, goal: Int) {
    val target = (eaten.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "homeCalorieRing",
    )
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = homeF1Alpha())
    val primary = if (eaten <= goal * 1.05f) MaterialTheme.colorScheme.onSurface else homeErrorColor()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val stroke = 7.dp.toPx()
            val ringSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, -90f, 360f, false, topLeft, ringSize, style = Stroke(stroke, cap = StrokeCap.Butt))
            drawArc(primary, -90f, 360f * progress, false, topLeft, ringSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(progress * 100).toInt()}%",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
                stringResource(R.string.home_pct_sub),
                style = TextStyle(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha()),
                ),
            )
        }
    }
}

@Composable
private fun HomeLinearProgress(
    progress: Float,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeF1Alpha()),
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(99.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.82f), color))),
        )
    }
}

@Composable
private fun TodaySection(ui: HomeViewModel.UiState, onAddFood: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayLabel(ui.selectedDay),
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
        if (ui.entries.isEmpty()) {
            EmptyToday(onAddFood)
        } else {
            val orderedMeals = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK, MealType.DINNER)
            var renderedGroups = 0
            orderedMeals.forEach { meal ->
                val entries = ui.entries.filter { it.mealType == meal }
                if (entries.isNotEmpty()) {
                    MealGroup(meal, entries, withDivider = renderedGroups > 0)
                    renderedGroups += 1
                }
            }
        }
    }
}

@Composable
private fun EmptyToday(onAddFood: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .calSnapClickable(pressedScale = 0.96f, onClick = onAddFood)
            .padding(top = 52.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🥗", style = TextStyle(fontSize = 52.sp))
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.home_empty_hint),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 25.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MealGroup(meal: MealType, entries: List<FoodLogEntity>, withDivider: Boolean) {
    Column(modifier = Modifier.padding(bottom = 2.dp)) {
        if (withDivider) {
            Box(
                modifier = Modifier
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = mealEmoji(meal),
                style = TextStyle(fontSize = 14.sp),
            )
            Text(
                text = mealTitle(meal).uppercase(),
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
                ),
            )
            Text(
                "${entries.sumOf { it.calories }} ${stringResource(R.string.unit_kcal)}",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha()),
                ),
            )
        }
        entries.forEachIndexed { index, entry ->
            FoodRow(entry, index = index, count = entries.size)
        }
    }
}

@Composable
private fun FoodRow(entry: FoodLogEntity, index: Int, count: Int) {
    val shape = when {
        count == 1 -> RoundedCornerShape(20.dp)
        index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        index == count - 1 -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        else -> RoundedCornerShape(0.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .then(if (index == count - 1) Modifier.padding(bottom = 12.dp) else Modifier)
            .shadow(if (count == 1 || index == count - 1) 8.dp else 0.dp, shape, clip = false)
            .clip(shape)
            .background(homeSurfaceColor())
            .calSnapClickable(pressedScale = 0.985f, onClick = {})
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(4.dp, RoundedCornerShape(13.dp), clip = false)
                .clip(RoundedCornerShape(13.dp))
                .background(homeSurface2Color())
                .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji(entry.foodName), style = TextStyle(fontSize = 24.sp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                entry.foodName,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.portion.orEmpty().ifBlank { "—" },
                style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${stringResource(R.string.macro_p_short)}:${entry.protein.toInt()}${stringResource(R.string.unit_g)}", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = macroProteinColor()))
                Text("${stringResource(R.string.macro_c_short)}:${entry.carbs.toInt()}${stringResource(R.string.unit_g)}", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = macroCarbsColor()))
                Text("${stringResource(R.string.macro_f_short)}:${entry.fat.toInt()}${stringResource(R.string.unit_g)}", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = macroFatColor()))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${entry.calories}",
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
                stringResource(R.string.unit_kcal),
                style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha())),
            )
        }
    }
}

@Composable
private fun mealTitle(meal: MealType): String = stringResource(
    when (meal) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.SNACK -> R.string.meal_snack
        MealType.DINNER -> R.string.meal_dinner
    },
)

@Composable
private fun dayLabel(selected: LocalDate): String {
    val today = LocalDate.now()
    return when (selected) {
        today -> stringResource(R.string.home_section_today)
        today.minusDays(1) -> stringResource(R.string.label_yesterday)
        else -> selected.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMMM", Locale.getDefault()))
    }
}

private fun weekdayLabel(date: LocalDate): String {
    val ru = Locale.getDefault().language == "ru"
    return when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> if (ru) "ПН" else "MON"
        java.time.DayOfWeek.TUESDAY -> if (ru) "ВТ" else "TUE"
        java.time.DayOfWeek.WEDNESDAY -> if (ru) "СР" else "WED"
        java.time.DayOfWeek.THURSDAY -> if (ru) "ЧТ" else "THU"
        java.time.DayOfWeek.FRIDAY -> if (ru) "ПТ" else "FRI"
        java.time.DayOfWeek.SATURDAY -> if (ru) "СБ" else "SAT"
        java.time.DayOfWeek.SUNDAY -> if (ru) "ВС" else "SUN"
    }
}

@Composable
private fun isHomeDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.25f

@Composable
private fun homeSurfaceColor(): Color = if (isHomeDark()) Color(0xFF1A1916) else Color.White

@Composable
private fun homeSurface2Color(): Color = if (isHomeDark()) Color(0xFF231F1B) else Color(0xFFF7F5F1)

@Composable
private fun homeWarnColor(): Color = if (isHomeDark()) Color(0xFFD97706) else Color(0xFFB45309)

@Composable
private fun homeOkColor(): Color = if (isHomeDark()) Color(0xFF22C55E) else Color(0xFF16A34A)

@Composable
private fun homeErrorColor(): Color = if (isHomeDark()) Color(0xFFE03535) else Color(0xFFC62626)

@Composable
private fun macroProteinColor(): Color = if (isHomeDark()) Color(0xFFF07055) else Color(0xFFB84530)

@Composable
private fun macroCarbsColor(): Color = if (isHomeDark()) Color(0xFFF0A840) else Color(0xFF8B6020)

@Composable
private fun macroFatColor(): Color = if (isHomeDark()) Color(0xFF5B9CF6) else Color(0xFF1A50AE)

@Composable
private fun homeT1Alpha(): Float = if (isHomeDark()) 0.74f else 0.70f

@Composable
private fun homeT2Alpha(): Float = if (isHomeDark()) 0.50f else 0.52f

@Composable
private fun homeF1Alpha(): Float = if (isHomeDark()) 0.08f else 0.065f

@Composable
private fun homeF2Alpha(): Float = if (isHomeDark()) 0.046f else 0.040f

@Composable
private fun homeB0Alpha(): Float = 0.14f

@Composable
private fun homeB1Alpha(): Float = 0.085f

private fun mealEmoji(meal: MealType): String = when (meal) {
    MealType.BREAKFAST -> "🌅"
    MealType.LUNCH -> "☀️"
    MealType.SNACK -> "🍏"
    MealType.DINNER -> "🌙"
}

private fun foodEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        listOf("кофе", "чай", "вода", "сок", "молоко").any(lower::contains) -> "🥤"
        listOf("кур", "мяс", "гов", "свин", "рыб").any(lower::contains) -> "🍗"
        listOf("салат", "огур", "помид", "овощ").any(lower::contains) -> "🥗"
        listOf("рис", "греч", "овся", "макарон").any(lower::contains) -> "🍚"
        listOf("торт", "шокол", "печ", "морож").any(lower::contains) -> "🍰"
        else -> "🍽️"
    }
}
