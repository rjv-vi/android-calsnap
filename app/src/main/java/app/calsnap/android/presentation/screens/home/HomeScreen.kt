package app.calsnap.android.presentation.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R

@Composable
fun HomeScreen(
    onAddFood: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val goal = ui.profile?.kcalGoal ?: 2000

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFood,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_add_food)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = ui.profile?.name?.let { stringResource(R.string.home_greeting, it) } ?: stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
            )
            CalorieHeroCard(
                eaten = ui.totalCalories,
                goal = goal,
            )
            MacroRow(
                proteinEaten = ui.totalProtein, proteinGoal = ui.profile?.proteinGoal ?: 100,
                carbsEaten = ui.totalCarbs, carbsGoal = ui.profile?.carbsGoal ?: 250,
                fatEaten = ui.totalFat, fatGoal = ui.profile?.fatGoal ?: 60,
            )
            WaterMiniCard(ui.waterMl, ui.waterGoalMl)
            TodayCard(ui)
            Spacer(Modifier.height(84.dp))
        }
    }
}

@Composable
private fun CalorieHeroCard(eaten: Int, goal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CalorieRing(eaten = eaten, goal = goal)
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_calories_eaten_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$eaten", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    Text(" / $goal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    text = if (eaten <= goal) stringResource(R.string.home_remaining, goal - eaten)
                    else stringResource(R.string.home_exceeded, eaten - goal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (eaten <= goal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CalorieRing(eaten: Int, goal: Int) {
    val progress = (eaten.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val primary = if (eaten <= goal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(116.dp)) {
        Canvas(modifier = Modifier.size(116.dp)) {
            val stroke = 12.dp.toPx()
            val size = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, -90f, 360f, false, topLeft, size, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(primary, -90f, 360f * progress, false, topLeft, size, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.unit_kcal), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun WaterMiniCard(waterMl: Int, goalMl: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.home_water_title), style = MaterialTheme.typography.titleSmall)
                Text("$waterMl / $goalMl ${stringResource(R.string.unit_ml)}", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (waterMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)),
            )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MacroChip(stringResource(R.string.macro_protein), proteinEaten, proteinGoal.toFloat(), Color(0xFFE85D5D), Modifier.weight(1f))
        MacroChip(stringResource(R.string.macro_carbs), carbsEaten, carbsGoal.toFloat(), Color(0xFF3B82F6), Modifier.weight(1f))
        MacroChip(stringResource(R.string.macro_fat), fatEaten, fatGoal.toFloat(), Color(0xFFFACC15), Modifier.weight(1f))
    }
}

@Composable
private fun MacroChip(label: String, value: Float, goal: Float, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(99.dp)).background(color))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = "${value.toInt()} / ${goal.toInt()} ${stringResource(R.string.unit_g)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TodayCard(ui: HomeViewModel.UiState) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_section_today),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
            if (ui.entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🥗", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.home_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                ui.entries.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(foodEmoji(entry.foodName))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(entry.foodName, style = MaterialTheme.typography.titleSmall)
                            Text(entry.portion.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${entry.calories} ${stringResource(R.string.unit_kcal)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
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
