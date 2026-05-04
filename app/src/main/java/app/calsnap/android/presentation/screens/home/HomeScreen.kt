package app.calsnap.android.presentation.screens.home

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.MealType
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapPillTextField
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapSoundEffect
import app.calsnap.android.presentation.components.CalSnapTextField
import app.calsnap.android.presentation.components.LocalCalSnapEffects
import app.calsnap.android.presentation.components.calSnapClickable
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val HomeEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    onAddFood: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val effects = LocalCalSnapEffects.current
    val goal = ui.profile?.kcalGoal ?: 2000
    var showApiSheet by remember { mutableStateOf(false) }
    var detailEntry by remember { mutableStateOf<FoodLogEntity?>(null) }
    var editEntry by remember { mutableStateOf<FoodLogEntity?>(null) }
    var deleteEntry by remember { mutableStateOf<FoodLogEntity?>(null) }
    val apiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(ui.entries, detailEntry?.id, editEntry?.id, deleteEntry?.id) {
        detailEntry = detailEntry?.let { selected -> ui.entries.firstOrNull { it.id == selected.id } }
        editEntry = editEntry?.let { selected -> ui.entries.firstOrNull { it.id == selected.id } }
        deleteEntry = deleteEntry?.let { selected -> ui.entries.firstOrNull { it.id == selected.id } }
    }

    CalSnapScreen(glow = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
        ) {
            AnimatedSection(0) { HomeHeader(ui) }
            if (!ui.hasApiKey) {
                AnimatedSection(1) {
                    ApiMissingBar(
                        onClick = {
                            effects.sound.play(CalSnapSoundEffect.SheetOpen)
                            showApiSheet = true
                        },
                    )
                }
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
                )
            }
            AnimatedSection(4) {
                TodaySection(
                    ui = ui,
                    onAddFood = onAddFood,
                    onOpenEntry = {
                        effects.sound.play(CalSnapSoundEffect.CardTap)
                        detailEntry = it
                    },
                    onToggleFavourite = {
                        effects.sound.play(CalSnapSoundEffect.Select)
                        viewModel.toggleFavourite(it)
                    },
                    onDelete = {
                        effects.sound.play(CalSnapSoundEffect.Delete)
                        deleteEntry = it
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showApiSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                effects.sound.play(CalSnapSoundEffect.SheetClose)
                showApiSheet = false
            },
            sheetState = apiSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            ApiKeySheet(
                onSave = { key ->
                    effects.sound.play(CalSnapSoundEffect.Save)
                    viewModel.saveGeminiKey(key)
                    showApiSheet = false
                },
                onCancel = {
                    effects.sound.play(CalSnapSoundEffect.SheetClose)
                    showApiSheet = false
                },
            )
        }
    }

    detailEntry?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = {
                effects.sound.play(CalSnapSoundEffect.SheetClose)
                detailEntry = null
            },
            sheetState = detailSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.50f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            FoodDetailSheet(
                entry = entry,
                onClose = {
                    effects.sound.play(CalSnapSoundEffect.SheetClose)
                    detailEntry = null
                },
                onEdit = {
                    effects.sound.play(CalSnapSoundEffect.SheetOpen)
                    editEntry = entry
                },
                onDelete = {
                    effects.sound.play(CalSnapSoundEffect.Delete)
                    deleteEntry = entry
                },
                onServingsChange = { viewModel.updateServings(entry, it) },
            )
        }
    }

    editEntry?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = {
                effects.sound.play(CalSnapSoundEffect.SheetClose)
                editEntry = null
            },
            sheetState = editSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            FoodEditSheet(
                entry = entry,
                onClose = {
                    effects.sound.play(CalSnapSoundEffect.SheetClose)
                    editEntry = null
                },
                onSave = { name, portion, kcal, protein, carbs, fat, time, meal ->
                    effects.sound.play(CalSnapSoundEffect.Save)
                    viewModel.updateEntryFromEdit(entry, name, portion, kcal, protein, carbs, fat, time, meal)
                    editEntry = null
                },
            )
        }
    }

    deleteEntry?.let { entry ->
        DeleteConfirmDialog(
            entry = entry,
            onConfirm = {
                effects.sound.play(CalSnapSoundEffect.Delete)
                viewModel.deleteEntry(entry)
                deleteEntry = null
                detailEntry = null
                editEntry = null
            },
            onCancel = {
                effects.sound.play(CalSnapSoundEffect.Back)
                deleteEntry = null
            },
        )
    }
}

@Composable
private fun ApiMissingBar(onClick: () -> Unit) {
    val warn = homeWarnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(warn.copy(alpha = 0.07f))
            .border(BorderStroke(1.5.dp, warn.copy(alpha = 0.18f)), RoundedCornerShape(20.dp))
            .calSnapClickable(pressedScale = 0.98f, sound = null, onClick = onClick)
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
private fun ApiKeySheet(
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val accent = if (isHomeDark()) Color(0xFFFF6618) else Color(0xFFFF5500)
    val helperPrefix = stringResource(R.string.home_api_sheet_sub_prefix)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 10.dp, bottom = 30.dp),
    ) {
        Text(
            stringResource(R.string.home_api_sheet_title),
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.7).sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = buildAnnotatedString {
                append(helperPrefix)
                append(" ")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append("aistudio.google.com/apikey")
                }
            },
            modifier = Modifier.calSnapClickable(pressedScale = 0.98f) {
                runCatching { uriHandler.openUri("https://aistudio.google.com/apikey") }
            },
            style = TextStyle(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
                lineHeight = 19.sp,
            ),
        )
        Spacer(Modifier.height(16.dp))
        CalSnapPillTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            placeholder = "AIza...",
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            minHeight = 58.dp,
        )
        Spacer(Modifier.height(14.dp))
        CalSnapPrimaryButton(
            onClick = { onSave(apiKey.trim()) },
            enabled = apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            height = 58.dp,
        ) {
            Text(stringResource(R.string.save), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(10.dp))
        CalSnapSecondaryButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            height = 58.dp,
        ) {
            Text(stringResource(R.string.cancel), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun FoodDetailSheet(
    entry: FoodLogEntity,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onServingsChange: (Float) -> Unit,
) {
    val servings = entry.servings.takeIf { it > 0f } ?: 1f
    val sheetMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = sheetMaxHeight)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (entry.imagePath.isNullOrBlank()) 160.dp else 220.dp)
                .background(homeSurface2Color()),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji(entry.foodName), style = TextStyle(fontSize = 72.sp))
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.foodName,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.8).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    if (!entry.portion.isNullOrBlank()) {
                        Text(
                            entry.portion,
                            modifier = Modifier.padding(top = 4.dp),
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())),
                        )
                    }
                }
                SheetCloseButton(onClose)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    "${entry.calories}",
                    style = TextStyle(
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-4).sp,
                        lineHeight = 58.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Text(
                    stringResource(R.string.unit_kcal),
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())),
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailMacroTile(stringResource(R.string.macro_protein), entry.protein, macroProteinColor(), Modifier.weight(1f))
                DetailMacroTile(stringResource(R.string.macro_carbs), entry.carbs, macroCarbsColor(), Modifier.weight(1f))
                DetailMacroTile(stringResource(R.string.macro_fat), entry.fat, macroFatColor(), Modifier.weight(1f))
            }
            if (!entry.ingredients.isNullOrBlank()) {
                IngredientsBlock(entry.ingredients)
            }
            DetailTime(entry)
            ServingControl(
                servings = servings,
                onMinus = { onServingsChange(max(0.5f, servings - 0.5f)) },
                onPlus = { onServingsChange(servings + 0.5f) },
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = homeF1Alpha()))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())), RoundedCornerShape(14.dp))
                        .calSnapClickable(pressedScale = 0.97f, onClick = onEdit)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.food_detail_edit),
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(homeErrorColor().copy(alpha = 0.10f))
                        .border(BorderStroke(1.dp, homeErrorColor().copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                        .calSnapClickable(pressedScale = 0.97f, onClick = onDelete)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.food_detail_delete),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = homeErrorColor()),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMacroTile(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(homeSurface2Color())
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${value.roundToInt()}${stringResource(R.string.unit_g)}",
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp, color = color),
        )
        Text(
            label.uppercase(),
            modifier = Modifier.padding(top = 4.dp),
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())),
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun IngredientsBlock(ingredients: String) {
    val items = remember(ingredients) {
        ingredients
            .split(Regex("[,;\\n•]+"))
            .map { it.trim().trim('-', '—') }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
    }
    if (items.isEmpty()) return
    val visibleItems = items.take(12)
    val hiddenCount = items.size - visibleItems.size
    Column(modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)) {
        Text(
            stringResource(R.string.food_detail_ingredients),
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            visibleItems.forEach { ingredient -> IngredientChip(ingredient) }
            if (hiddenCount > 0) IngredientChip("+$hiddenCount")
        }
    }
}

@Composable
private fun IngredientChip(text: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 154.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(homeSurface2Color())
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailTime(entry: FoodLogEntity) {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())),
    )
    Text(
        stringResource(R.string.food_detail_added, detailDate(entry), detailTime(entry)),
        modifier = Modifier.padding(vertical = 12.dp),
        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha())),
    )
}

@Composable
private fun ServingControl(
    servings: Float,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val servingsDisplay by animateFloatAsState(
        targetValue = servings,
        animationSpec = tween(280, easing = HomeEaseOut),
        label = "foodDetailServings",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.food_detail_servings),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityButton(text = "−", primary = false, onClick = onMinus)
            Text(
                "×${formatDecimal(servingsDisplay)}",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onSurface),
                textAlign = TextAlign.Center,
            )
            QuantityButton(text = "+", primary = true, onClick = onPlus)
        }
    }
}

@Composable
private fun QuantityButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val bg = if (primary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = homeF1Alpha())
    val fg = if (primary) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    val border = if (primary) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())
    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(if (primary) 4.dp else 0.dp, RoundedCornerShape(19.dp), clip = false)
            .clip(RoundedCornerShape(19.dp))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(19.dp))
            .calSnapClickable(pressedScale = 0.76f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Light, color = fg, lineHeight = 22.sp))
    }
}

@Composable
private fun FoodEditSheet(
    entry: FoodLogEntity,
    onClose: () -> Unit,
    onSave: (String, String, Int, Float, Float, Float, String, MealType) -> Unit,
) {
    var name by remember(entry.id) { mutableStateOf(entry.foodName) }
    var portion by remember(entry.id) { mutableStateOf(entry.portion.orEmpty()) }
    var calories by remember(entry.id) { mutableStateOf(entry.calories.toString()) }
    var protein by remember(entry.id) { mutableStateOf(formatDecimal(entry.protein)) }
    var carbs by remember(entry.id) { mutableStateOf(formatDecimal(entry.carbs)) }
    var fat by remember(entry.id) { mutableStateOf(formatDecimal(entry.fat)) }
    var time by remember(entry.id) { mutableStateOf(detailTime(entry)) }
    var meal by remember(entry.id) { mutableStateOf(entry.mealType) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 30.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.food_edit_title),
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp, color = MaterialTheme.colorScheme.onSurface),
            )
            SheetCloseButton(onClose)
        }
        CompactTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.food_edit_name),
            placeholder = stringResource(R.string.food_edit_name_placeholder),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactTextField(
                value = portion,
                onValueChange = { portion = it },
                label = stringResource(R.string.food_edit_portion),
                placeholder = stringResource(R.string.food_edit_portion_placeholder),
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = calories,
                onValueChange = { calories = it.filter { c -> c.isDigit() } },
                label = stringResource(R.string.food_edit_calories),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactTextField(
                value = protein,
                onValueChange = { protein = numericText(it) },
                label = stringResource(R.string.food_edit_protein),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = carbs,
                onValueChange = { carbs = numericText(it) },
                label = stringResource(R.string.food_edit_carbs),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactTextField(
                value = fat,
                onValueChange = { fat = numericText(it) },
                label = stringResource(R.string.food_edit_fat),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = time,
                onValueChange = { time = it.take(5) },
                label = stringResource(R.string.food_edit_time),
                placeholder = "20:17",
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            stringResource(R.string.food_edit_meal).uppercase(),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha())),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK, MealType.DINNER).forEach { option ->
                MealEditChip(
                    meal = option,
                    selected = option == meal,
                    onClick = { meal = option },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        CalSnapPrimaryButton(
            onClick = {
                onSave(
                    name.trim(),
                    portion.trim(),
                    calories.toIntOrNull() ?: entry.calories,
                    protein.toFloatOrNull() ?: entry.protein,
                    carbs.toFloatOrNull() ?: entry.carbs,
                    fat.toFloatOrNull() ?: entry.fat,
                    time,
                    meal,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            height = 58.dp,
        ) {
            Text(stringResource(R.string.save), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier.padding(bottom = 10.dp)) {
        Text(
            label.uppercase(),
            modifier = Modifier.padding(bottom = 4.dp),
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha())),
        )
        CalSnapTextField(
            value = value,
            onValueChange = onValueChange,
            label = "",
            placeholder = placeholder,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MealEditChip(meal: MealType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = homeF1Alpha())
    val fg = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    val border = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())
    val title = mealTitle(meal)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(999.dp))
            .calSnapClickable(pressedScale = 0.93f, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${mealEmoji(meal)} $title",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeleteConfirmDialog(entry: FoodLogEntity, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val fallbackFood = stringResource(R.string.food_default_label)
    val kcalUnit = stringResource(R.string.unit_kcal)
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(30.dp, RoundedCornerShape(26.dp), clip = false)
                    .clip(RoundedCornerShape(26.dp))
                    .background(homeSurfaceColor())
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())), RoundedCornerShape(26.dp)),
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                    Text("🗑️", style = TextStyle(fontSize = 42.sp))
                    Text(
                        stringResource(R.string.confirm_delete_diary_title),
                        modifier = Modifier.padding(top = 14.dp),
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp, color = MaterialTheme.colorScheme.onSurface),
                    )
                    Text(
                        "«${entry.foodName.ifBlank { fallbackFood }}» (${entry.calories} $kcalUnit)",
                        modifier = Modifier.padding(top = 8.dp),
                        style = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())),
                    )
                }
                ConfirmButton(text = stringResource(R.string.food_detail_delete).removePrefix("🗑 "), danger = true, onClick = onConfirm)
                ConfirmButton(text = stringResource(R.string.cancel), danger = false, onClick = onCancel)
            }
        }
    }
}

@Composable
private fun ConfirmButton(text: String, danger: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .calSnapClickable(pressedScale = 0.99f, onClick = onClick)
            .padding(vertical = 17.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (danger) homeErrorColor() else MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha()),
            ),
        )
    }
}

@Composable
private fun SheetCloseButton(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = homeF1Alpha()))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB0Alpha())), RoundedCornerShape(16.dp))
            .calSnapClickable(pressedScale = 0.82f, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT1Alpha())))
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
) {
    val remaining = goal - eaten
    val eatenDisplay by animateIntAsState(
        targetValue = eaten,
        animationSpec = tween(520, easing = HomeEaseOut),
        label = "homeCaloriesEaten",
    )
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
                            "$eatenDisplay",
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
        }
    }
}

@Composable
private fun MacroMiniTile(label: String, value: Float, goal: Float, color: Color, modifier: Modifier = Modifier) {
    val valueDisplay by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(420, easing = HomeEaseOut),
        label = "homeMacroValue",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(homeSurface2Color())
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = homeB1Alpha())), RoundedCornerShape(16.dp))
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                "${valueDisplay.roundToInt()}${stringResource(R.string.unit_g)}",
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
        HomeLinearProgress(progress = value / goal.coerceAtLeast(1f), color = color, height = 3.dp, startColor = color)
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
private fun CalorieRing(eaten: Int, goal: Int) {
    val target = (eaten.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(1000, easing = HomeEaseOut),
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
                "${(progress * 100).roundToInt()}%",
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
    startColor: Color = color.copy(alpha = 0.82f),
    animationDurationMillis: Int = 900,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(animationDurationMillis, easing = HomeEaseOut),
        label = "homeLinearProgress",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(RoundedCornerShape(99.dp))
                .background(Brush.horizontalGradient(listOf(startColor, color))),
        )
    }
}

@Composable
private fun TodaySection(
    ui: HomeViewModel.UiState,
    onAddFood: () -> Unit,
    onOpenEntry: (FoodLogEntity) -> Unit,
    onToggleFavourite: (FoodLogEntity) -> Unit,
    onDelete: (FoodLogEntity) -> Unit,
) {
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
                    MealGroup(
                        meal = meal,
                        entries = entries,
                        withDivider = renderedGroups > 0,
                        onOpenEntry = onOpenEntry,
                        onToggleFavourite = onToggleFavourite,
                        onDelete = onDelete,
                    )
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
private fun MealGroup(
    meal: MealType,
    entries: List<FoodLogEntity>,
    withDivider: Boolean,
    onOpenEntry: (FoodLogEntity) -> Unit,
    onToggleFavourite: (FoodLogEntity) -> Unit,
    onDelete: (FoodLogEntity) -> Unit,
) {
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
            FoodRow(
                entry = entry,
                index = index,
                count = entries.size,
                onOpenEntry = onOpenEntry,
                onToggleFavourite = onToggleFavourite,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun FoodRow(
    entry: FoodLogEntity,
    index: Int,
    count: Int,
    onOpenEntry: (FoodLogEntity) -> Unit,
    onToggleFavourite: (FoodLogEntity) -> Unit,
    onDelete: (FoodLogEntity) -> Unit,
) {
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
            .calSnapClickable(pressedScale = 0.985f, sound = null) { onOpenEntry(entry) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji(entry.foodName), style = TextStyle(fontSize = 24.sp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.foodName,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.servings < 0.99f || entry.servings > 1.01f) {
                    Box(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "×${formatDecimal(entry.servings)}",
                            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
            Text(
                foodSubtitle(entry),
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
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 2.dp)) {
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
        Text(
            if (entry.favourite) "⭐" else "☆",
            modifier = Modifier
                .calSnapClickable(pressedScale = 1.4f, sound = null) { onToggleFavourite(entry) }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            style = TextStyle(
                fontSize = 16.sp,
                color = if (entry.favourite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha()),
            ),
        )
        Box(
            modifier = Modifier
                .calSnapClickable(pressedScale = 0.75f, sound = null) { onDelete(entry) }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            TrashIcon()
        }
    }
}

@Composable
private fun TrashIcon() {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeT2Alpha())
    Canvas(modifier = Modifier.size(15.dp)) {
        val stroke = 1.8.dp.toPx()
        drawLine(color, Offset(size.width * 0.15f, size.height * 0.23f), Offset(size.width * 0.85f, size.height * 0.23f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.36f, size.height * 0.10f), Offset(size.width * 0.64f, size.height * 0.10f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.34f, size.height * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.72f, size.height * 0.28f), Offset(size.width * 0.66f, size.height * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.42f, size.height * 0.46f), Offset(size.width * 0.42f, size.height * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.58f, size.height * 0.46f), Offset(size.width * 0.58f, size.height * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.34f, size.height * 0.88f), Offset(size.width * 0.66f, size.height * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
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

private fun foodSubtitle(entry: FoodLogEntity): String {
    val time = Instant.ofEpochMilli(entry.loggedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    val portion = entry.portion.orEmpty().trim()
    return if (portion.isBlank()) time else "$time · $portion"
}

private fun detailDate(entry: FoodLogEntity): String =
    Instant.ofEpochMilli(entry.loggedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE MMM dd yyyy", Locale.ENGLISH))

private fun detailTime(entry: FoodLogEntity): String =
    Instant.ofEpochMilli(entry.loggedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

private fun formatDecimal(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun numericText(input: String): String {
    var dotUsed = false
    return buildString {
        input.replace(',', '.').forEach { char ->
            when {
                char.isDigit() -> append(char)
                char == '.' && !dotUsed -> {
                    append(char)
                    dotUsed = true
                }
            }
        }
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
        listOf("карто", "potato").any(lower::contains) -> "🥔"
        listOf("рис", "греч", "овся", "макарон").any(lower::contains) -> "🍚"
        listOf("понч", "donut").any(lower::contains) -> "🍩"
        listOf("торт", "шокол", "печ", "морож").any(lower::contains) -> "🍰"
        else -> "🍽️"
    }
}
