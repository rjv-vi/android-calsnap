package app.calsnap.android.presentation.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.MealType
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapIconTile
import app.calsnap.android.presentation.components.CalSnapPill
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapProgressBar
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak
import app.calsnap.android.ui.theme.MacroCarbs
import app.calsnap.android.ui.theme.MacroFat
import app.calsnap.android.ui.theme.MacroProtein
import app.calsnap.android.ui.theme.MacroWater
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onAddFood: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val goal = ui.profile?.kcalGoal ?: 2000

    CalSnapScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedSection(0) { HomeHeader(ui) }
            AnimatedSection(1) { CalendarStrip(ui.calendarDays, ui.selectedDay, viewModel::selectDay) }
            if (!ui.hasApiKey) {
                AnimatedSection(2) { ApiMissingBar() }
            }
            AnimatedSection(3) { CalorieHeroCard(eaten = ui.totalCalories, goal = goal) }
            AnimatedSection(4) {
                MacroRow(
                    proteinEaten = ui.totalProtein, proteinGoal = ui.profile?.proteinGoal ?: 100,
                    carbsEaten = ui.totalCarbs, carbsGoal = ui.profile?.carbsGoal ?: 250,
                    fatEaten = ui.totalFat, fatGoal = ui.profile?.fatGoal ?: 60,
                )
            }
            AnimatedSection(5) { WaterMiniCard(ui.waterMl, ui.waterGoalMl) }
            AnimatedSection(6) { TodayCard(ui, onAddFood) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ApiMissingBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1AB45309))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🔑", style = MaterialTheme.typography.titleLarge)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.api_key_needed_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color(0xFFB45309))
            Text(stringResource(R.string.api_key_needed_sub), style = MaterialTheme.typography.bodySmall, color = Color(0x99B45309), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeHeader(ui: HomeViewModel.UiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = timeGreeting(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = ui.profile?.name?.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.home_greeting, it) }
                    ?: stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StreakPill(ui.calendarDays)
    }
}

@Composable
private fun StreakPill(days: List<HomeViewModel.CalendarDay>) {
    val streak = days.reversed().takeWhile { it.hasLog }.count()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(CalSnapStreak.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("🔥", style = MaterialTheme.typography.titleSmall)
        Text("$streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CalSnapStreak)
        Text(stringResource(R.string.progress_days), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = CalSnapStreak.copy(alpha = 0.78f))
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
private fun CalendarStrip(days: List<HomeViewModel.CalendarDay>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            val isSelected = day.date == selected
            CalendarDayChip(day = day, selected = isSelected, onClick = { onSelect(day.date) })
        }
    }
}

@Composable
private fun CalendarDayChip(day: HomeViewModel.CalendarDay, selected: Boolean, onClick: () -> Unit) {
    val today = day.date == LocalDate.now()
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .calSnapClickable(pressedScale = 0.86f, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
            fontWeight = FontWeight.Black,
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(
                    when {
                        today -> MaterialTheme.colorScheme.onSurface
                        selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f)
                        day.hasLog -> Color(0x1A16A34A)
                        else -> Color.Transparent
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    today -> MaterialTheme.colorScheme.background
                    day.hasLog -> Color(0xFF16A34A)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Black,
            )
        }
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    when {
                        day.hasLog -> Color(0xFF16A34A)
                        selected -> MaterialTheme.colorScheme.onSurface
                        else -> Color.Transparent
                    },
                ),
        )
    }
}

@Composable
private fun CalorieHeroCard(eaten: Int, goal: Int) {
    val remaining = goal - eaten
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        padding = PaddingValues(22.dp),
        elevation = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.home_calories_eaten_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$eaten", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                    Text(
                        " / $goal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                CalSnapPill(
                    text = if (remaining >= 0) stringResource(R.string.home_remaining, remaining)
                    else stringResource(R.string.home_exceeded, -remaining),
                    selected = false,
                    icon = if (remaining >= 0) "✓" else "!",
                )
            }
            CalorieRing(eaten = eaten, goal = goal)
        }
    }
}

@Composable
private fun CalorieRing(eaten: Int, goal: Int) {
    val target = (eaten.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "homeCalorieRing",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    val primary = if (eaten <= goal) CalSnapStreak else MaterialTheme.colorScheme.error
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
        Canvas(modifier = Modifier.size(104.dp)) {
            val stroke = 10.dp.toPx()
            val ringSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, -90f, 360f, false, topLeft, ringSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(primary, -90f, 360f * progress, false, topLeft, ringSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.unit_kcal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaterMiniCard(waterMl: Int, goalMl: Int) {
    val waterDisplay by animateIntAsState(
        targetValue = waterMl,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "homeWater",
    )
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        padding = PaddingValues(16.dp),
        elevation = 10.dp,
        containerBrush = Brush.verticalGradient(
            listOf(MacroWater.copy(alpha = 0.13f), MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        ),
        borderColor = MacroWater.copy(alpha = 0.18f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = "💧", size = 46.dp, background = MacroWater.copy(alpha = 0.13f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.home_water_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    Text("$waterDisplay / $goalMl ${stringResource(R.string.unit_ml)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                CalSnapProgressBar(
                    progress = waterMl.toFloat() / goalMl.coerceAtLeast(1).toFloat(),
                    color = MacroWater,
                    height = 8.dp,
                )
            }
        }
    }
}

@Composable
private fun MacroRow(
    proteinEaten: Float, proteinGoal: Int,
    carbsEaten: Float, carbsGoal: Int,
    fatEaten: Float, fatGoal: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MacroTile(stringResource(R.string.macro_protein), proteinEaten, proteinGoal.toFloat(), MacroProtein, Modifier.weight(1f))
        MacroTile(stringResource(R.string.macro_carbs), carbsEaten, carbsGoal.toFloat(), MacroCarbs, Modifier.weight(1f))
        MacroTile(stringResource(R.string.macro_fat), fatEaten, fatGoal.toFloat(), MacroFat, Modifier.weight(1f))
    }
}

@Composable
private fun MacroTile(label: String, value: Float, goal: Float, color: Color, modifier: Modifier = Modifier) {
    CalSnapCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        padding = PaddingValues(12.dp),
        elevation = 4.dp,
        containerBrush = Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
        ),
    ) {
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(99.dp)).background(color))
        Spacer(Modifier.height(9.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${value.toInt()} / ${goal.toInt()}${stringResource(R.string.unit_g)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Spacer(Modifier.height(9.dp))
        CalSnapProgressBar(progress = value / goal.coerceAtLeast(1f), color = color, height = 6.dp)
    }
}

@Composable
private fun TodayCard(ui: HomeViewModel.UiState, onAddFood: () -> Unit) {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        padding = PaddingValues(14.dp),
        elevation = 8.dp,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_section_today),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "${ui.entries.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (ui.entries.isEmpty()) {
            EmptyToday(onAddFood)
        } else {
            val orderedMeals = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK, MealType.DINNER)
            orderedMeals.forEach { meal ->
                val entries = ui.entries.filter { it.mealType == meal }
                if (entries.isNotEmpty()) {
                    MealGroup(meal, entries)
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
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🥗", style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.home_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        CalSnapPrimaryButton(onClick = onAddFood, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_add_food))
        }
    }
}

@Composable
private fun MealGroup(meal: MealType, entries: List<FoodLogEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${mealEmoji(meal)} ${mealTitle(meal)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "${entries.sumOf { it.calories }} ${stringResource(R.string.unit_kcal)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
        entries.forEach { entry -> FoodRow(entry) }
    }
}

@Composable
private fun FoodRow(entry: FoodLogEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji(entry.foodName))
        }
        Column(Modifier.weight(1f)) {
            Text(entry.foodName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                entry.portion.orEmpty().ifBlank { "Б ${entry.protein.toInt()} · У ${entry.carbs.toInt()} · Ж ${entry.fat.toInt()}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("${entry.calories}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
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

private fun mealEmoji(meal: MealType): String = when (meal) {
    MealType.BREAKFAST -> "☕"
    MealType.LUNCH -> "🍲"
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
