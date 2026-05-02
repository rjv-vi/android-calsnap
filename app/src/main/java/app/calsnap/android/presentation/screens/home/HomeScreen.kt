package app.calsnap.android.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    text = ui.profile?.name?.let {
                        stringResource(R.string.home_greeting, it)
                    } ?: stringResource(R.string.app_name),
                )
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFood,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_add_food)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            CalorieSummaryCard(
                eaten   = ui.totalCalories,
                goal    = ui.profile?.kcalGoal ?: 2000,
            )
            Spacer(Modifier.height(16.dp))
            MacroRow(
                proteinEaten = ui.totalProtein, proteinGoal = ui.profile?.proteinGoal ?: 100,
                carbsEaten   = ui.totalCarbs,   carbsGoal   = ui.profile?.carbsGoal   ?: 250,
                fatEaten     = ui.totalFat,     fatGoal     = ui.profile?.fatGoal     ?: 60,
            )
            Spacer(Modifier.height(12.dp))
            WaterMiniCard(ui.waterMl, ui.waterGoalMl)
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_section_today),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (ui.entries.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🥗", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.home_empty_hint))
                    }
                }
            } else {
                // TODO: MealGroupList — ports PWA's meal-grouped list (v1.1).
                ui.entries.forEach { entry ->
                    Text(
                        text = "• ${entry.foodName} · ${entry.calories} ${stringResource(R.string.unit_kcal)}",
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterMiniCard(waterMl: Int, goalMl: Int) {
    Card(Modifier.fillMaxWidth()) {
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
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CalorieSummaryCard(eaten: Int, goal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_calories_eaten_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$eaten",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "  / $goal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = if (eaten <= goal)
                    stringResource(R.string.home_remaining, goal - eaten)
                else
                    stringResource(R.string.home_exceeded, eaten - goal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MacroRow(
    proteinEaten: Float, proteinGoal: Int,
    carbsEaten: Float,   carbsGoal: Int,
    fatEaten: Float,     fatGoal: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MacroChip(stringResource(R.string.macro_protein), proteinEaten, proteinGoal.toFloat(), Modifier.weight(1f))
        MacroChip(stringResource(R.string.macro_carbs),   carbsEaten,   carbsGoal.toFloat(),   Modifier.weight(1f))
        MacroChip(stringResource(R.string.macro_fat),     fatEaten,     fatGoal.toFloat(),     Modifier.weight(1f))
    }
}

@Composable
private fun MacroChip(label: String, value: Float, goal: Float, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${value.toInt()} / ${goal.toInt()} ${stringResource(R.string.unit_g)}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
