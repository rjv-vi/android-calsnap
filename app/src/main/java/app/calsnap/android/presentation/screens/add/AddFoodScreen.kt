package app.calsnap.android.presentation.screens.add

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.FoodAnalysisResult
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapIconTile
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapProgressBar
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapTextField
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak
import app.calsnap.android.ui.theme.MacroCarbs
import app.calsnap.android.ui.theme.MacroFat
import app.calsnap.android.ui.theme.MacroProtein
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AddFoodScreen(
    onDismiss: () -> Unit,
    sheetMode: Boolean = false,
    viewModel: AddFoodViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshKeyState() }
    val close = {
        viewModel.resetTransientState()
        onDismiss()
    }

    if (sheetMode) {
        AddFoodContent(ui, viewModel, close, sheetMode = true)
    } else {
        CalSnapScreen {
            AddFoodContent(ui, viewModel, close, sheetMode = false)
        }
    }
}

@Composable
private fun AddFoodContent(
    ui: AddFoodViewModel.UiState,
    viewModel: AddFoodViewModel,
    onDismiss: () -> Unit,
    sheetMode: Boolean,
) {
    Column(
        modifier = Modifier
            .then(if (sheetMode) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = if (sheetMode) 8.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedSection(0) { Header(onDismiss) }
        AnimatedSection(1) { AddTabs(selected = ui.tab, onSelect = viewModel::selectTab) }
        AnimatedSection(2) {
            CalSnapCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(32.dp),
                padding = PaddingValues(18.dp),
                containerBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)),
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                elevation = 18.dp,
            ) {
                if (!ui.hasApiKey && ui.tab != AddFoodViewModel.Tab.BARCODE && ui.tab != AddFoodViewModel.Tab.FAVOURITES) {
                    ApiKeyMissingCard()
                } else {
                    AnimatedContent(
                        targetState = ui.tab,
                        transitionSpec = {
                            (fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 6 }) togetherWith
                                (fadeOut(tween(110, easing = FastOutSlowInEasing)) + slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { -it / 8 })
                        },
                        label = "addTab",
                    ) { tab ->
                        when (tab) {
                            AddFoodViewModel.Tab.PHOTO -> PhotoTab(viewModel, ui.loading)
                            AddFoodViewModel.Tab.TEXT -> TextTab(viewModel, ui.loading)
                            AddFoodViewModel.Tab.BARCODE -> BarcodeTab(viewModel, ui.loading)
                            AddFoodViewModel.Tab.FAVOURITES -> FavouritesTab(
                                favourites = ui.favourites,
                                onAdd = { entry, multiplier ->
                                    viewModel.logFavourite(entry, multiplier)
                                    onDismiss()
                                },
                                onRemove = viewModel::removeFavourite,
                            )
                        }
                    }
                }
                if (ui.loading) {
                    Spacer(Modifier.height(14.dp))
                    LoadingRow()
                }
                ui.error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorCard(it)
                }
            }
        }
        ui.result?.let { result ->
            AnimatedSection(3) {
                ResultCard(
                    result = result,
                    onConfirm = {
                        viewModel.confirmAndLog(result, ui.resultSource)
                        onDismiss()
                    },
                    onConfirmFavourite = {
                        viewModel.confirmAndLog(result, ui.resultSource, saveFavourite = true)
                        onDismiss()
                    },
                )
            }
        }
        Spacer(Modifier.height(if (sheetMode) 10.dp else 24.dp))
    }
}

@Composable
private fun Header(onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.add_food_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(
                stringResource(R.string.add_food_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.74f))
                .calSnapClickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
        }
    }
}

@Composable
private fun AddTabs(selected: AddFoodViewModel.Tab, onSelect: (AddFoodViewModel.Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        AddFoodViewModel.Tab.entries.forEach { tab ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .calSnapClickable(pressedScale = 0.93f, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (tab) {
                    AddFoodViewModel.Tab.PHOTO -> stringResource(R.string.add_tab_photo)
                    AddFoodViewModel.Tab.TEXT -> stringResource(R.string.add_tab_text)
                    AddFoodViewModel.Tab.BARCODE -> stringResource(R.string.add_tab_barcode)
                    AddFoodViewModel.Tab.FAVOURITES -> stringResource(R.string.add_tab_favourites)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BarcodeTab(viewModel: AddFoodViewModel, loading: Boolean) {
    var code by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TabIntro("🏷️", stringResource(R.string.add_barcode_title), stringResource(R.string.add_barcode_subtitle))
        CalSnapTextField(
            value = code,
            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(18) },
            label = stringResource(R.string.add_barcode_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        CalSnapPrimaryButton(
            onClick = { viewModel.lookupBarcode(code) },
            modifier = Modifier.fillMaxWidth(),
            enabled = code.length >= 8 && !loading,
        ) {
            Text(stringResource(R.string.add_lookup_barcode))
        }
    }
}

@Composable
private fun PhotoTab(viewModel: AddFoodViewModel, loading: Boolean) {
    val context = LocalContext.current
    var hint by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedName = uri.lastPathSegment
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
        }.onSuccess { bitmap ->
            viewModel.analyzePhoto(bitmap, hint)
        }.onFailure { error ->
            viewModel.setError(error.message)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TabIntro("📸", stringResource(R.string.add_photo_title), stringResource(R.string.add_photo_subtitle))
        CalSnapTextField(
            value = hint,
            onValueChange = { hint = it },
            label = stringResource(R.string.add_photo_hint),
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        CalSnapPrimaryButton(
            onClick = { picker.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        ) {
            Text(stringResource(R.string.add_pick_photo))
        }
        selectedName?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TextTab(viewModel: AddFoodViewModel, loading: Boolean) {
    var input by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TabIntro("✨", stringResource(R.string.add_text_title), stringResource(R.string.add_text_subtitle))
        CalSnapTextField(
            value = input,
            onValueChange = { input = it },
            label = stringResource(R.string.add_text_placeholder),
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
        )
        CalSnapPrimaryButton(
            onClick = { viewModel.analyzeText(input) },
            modifier = Modifier.fillMaxWidth(),
            enabled = input.isNotBlank() && !loading,
        ) {
            Text(stringResource(R.string.add_analyze_with_ai))
        }
    }
}

@Composable
private fun FavouritesTab(
    favourites: List<FoodLogEntity>,
    onAdd: (FoodLogEntity, Float) -> Unit,
    onRemove: (FoodLogEntity) -> Unit,
) {
    var selectedId by remember(favourites) { mutableStateOf<Long?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FavouriteIntro()
        if (favourites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.add_favourites_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            favourites.forEach { entry ->
                val selected = selectedId == entry.id
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FavouriteRow(
                        entry = entry,
                        selected = selected,
                        onAdd = { selectedId = if (selected) null else entry.id },
                        onRemove = { onRemove(entry) },
                    )
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) + shrinkVertically(tween(160, easing = FastOutSlowInEasing)),
                    ) {
                        FavouritePortionPanel(entry = entry, onAdd = { multiplier -> onAdd(entry, multiplier) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FavouriteRow(entry: FoodLogEntity, selected: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .calSnapClickable(pressedScale = 0.98f, onClick = onAdd)
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji(entry.foodName), style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.foodName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${entry.calories} ${stringResource(R.string.unit_kcal)}" + entry.portion.orEmpty().let { if (it.isBlank()) "" else " · $it" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "✕",
            modifier = Modifier
                .calSnapClickable(pressedScale = 0.85f, onClick = onRemove)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
        )
        Text(
            if (selected) stringResource(R.string.add_favourite_selected) else stringResource(R.string.add_favourite_choose_portion),
            modifier = Modifier
                .calSnapClickable(pressedScale = 0.94f, onClick = onAdd)
                .padding(horizontal = 2.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun FavouritePortionPanel(entry: FoodLogEntity, onAdd: (Float) -> Unit) {
    var multiplier by remember(entry.id) { mutableStateOf(1f) }
    val calories by animateFloatAsState(entry.calories * multiplier, tween(260, easing = FastOutSlowInEasing), label = "favCalories")
    val protein by animateFloatAsState(entry.protein * multiplier, tween(260, easing = FastOutSlowInEasing), label = "favProtein")
    val carbs by animateFloatAsState(entry.carbs * multiplier, tween(260, easing = FastOutSlowInEasing), label = "favCarbs")
    val fat by animateFloatAsState(entry.fat * multiplier, tween(260, easing = FastOutSlowInEasing), label = "favFat")
    val options = listOf(
        PortionOption(0.5f, "0.5×", stringResource(R.string.add_favourite_choice_less)),
        PortionOption(1f, "1×", stringResource(R.string.add_favourite_choice_same)),
        PortionOption(1.5f, "1.5×", stringResource(R.string.add_favourite_choice_more)),
        PortionOption(2f, "2×", stringResource(R.string.add_favourite_choice_double)),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeutralIconTile(icon = foodEmoji(entry.foodName), size = 52.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.add_favourite_portion_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    stringResource(R.string.add_favourite_portion_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                calories.roundToInt().toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.unit_kcal),
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.add_favourite_selected_size, formatMultiplier(multiplier)),
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 9.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                PortionOptionChip(
                    option = option,
                    selected = multiplier == option.multiplier,
                    onClick = { multiplier = option.multiplier },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.add_favourite_base_portion),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PortionStepButton("−") { multiplier = (multiplier - 0.5f).coerceAtLeast(0.5f) }
                Text(
                    "${formatMultiplier(multiplier)}×",
                    modifier = Modifier.padding(horizontal = 2.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PortionStepButton("+") { multiplier = (multiplier + 0.5f).coerceAtMost(5f) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeutralMacroPill("Б", protein, Modifier.weight(1f))
            NeutralMacroPill("У", carbs, Modifier.weight(1f))
            NeutralMacroPill("Ж", fat, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        CalSnapPrimaryButton(onClick = { onAdd(multiplier) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_favourite_add_portion))
        }
    }
}

@Composable
private fun FavouriteIntro() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NeutralIconTile(icon = "⭐")
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.add_favourites_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.add_favourites_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NeutralIconTile(icon: String, size: androidx.compose.ui.unit.Dp = 52.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3f))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
    }
}

private data class PortionOption(val multiplier: Float, val label: String, val caption: String)

@Composable
private fun PortionOptionChip(option: PortionOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    val sub = if (selected) MaterialTheme.colorScheme.background.copy(alpha = 0.74f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.0f else 0.16f)), RoundedCornerShape(16.dp))
            .calSnapClickable(pressedScale = 0.92f, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(option.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = fg)
        Text(option.caption, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NeutralMacroPill(label: String, value: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        Text("${value.toInt()}${stringResource(R.string.unit_g)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PortionStepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .calSnapClickable(pressedScale = 0.78f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TabIntro(icon: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CalSnapIconTile(icon = icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ApiKeyMissingCard() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CalSnapIconTile(icon = "🔑", size = 58.dp)
        Text(stringResource(R.string.api_key_needed_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(
            stringResource(R.string.api_key_needed_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp, color = CalSnapStreak)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.add_analyzing), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            CalSnapProgressBar(progress = 0.72f, color = CalSnapStreak, height = 6.dp)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        padding = PaddingValues(14.dp),
        containerBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.errorContainer)),
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.28f),
        elevation = 4.dp,
    ) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ResultCard(result: FoodAnalysisResult, onConfirm: () -> Unit, onConfirmFavourite: () -> Unit) {
    CalSnapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        padding = PaddingValues(18.dp),
        containerBrush = Brush.verticalGradient(
            listOf(CalSnapStreak.copy(alpha = 0.18f), MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        ),
        borderColor = CalSnapStreak.copy(alpha = 0.24f),
        elevation = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalSnapIconTile(icon = "✅", size = 54.dp, background = CalSnapStreak.copy(alpha = 0.12f))
            Column(Modifier.weight(1f)) {
                Text(result.food, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (result.portion.isNotBlank()) {
                    Text(result.portion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("${result.calories}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.unit_kcal), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroPill("Б", result.protein, MacroProtein, Modifier.weight(1f))
            MacroPill("У", result.carbs, MacroCarbs, Modifier.weight(1f))
            MacroPill("Ж", result.fat, MacroFat, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        CalSnapSecondaryButton(onClick = onConfirmFavourite, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_save_favourite))
        }
        Spacer(Modifier.height(10.dp))
        CalSnapPrimaryButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_confirm_add))
        }
    }
}

@Composable
private fun MacroPill(label: String, value: Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        Text("${value.toInt()}${stringResource(R.string.unit_g)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
    }
}

private fun formatMultiplier(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

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
