package app.calsnap.android.presentation.screens.add

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.model.FoodAnalysisResult
import app.calsnap.android.presentation.components.AnimatedSection
import app.calsnap.android.presentation.components.CalSnapCard
import app.calsnap.android.presentation.components.CalSnapIconTile
import app.calsnap.android.presentation.components.CalSnapPill
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

@Composable
fun AddFoodScreen(
    onDismiss: () -> Unit,
    viewModel: AddFoodViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshKeyState() }

    CalSnapScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
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
                    elevation = 18.dp,
                ) {
                    if (!ui.hasApiKey && ui.tab != AddFoodViewModel.Tab.BARCODE) {
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
                    ResultCard(result) {
                        viewModel.confirmAndLog(result, ui.resultSource)
                        onDismiss()
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
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
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AddFoodViewModel.Tab.entries.forEach { tab ->
            CalSnapPill(
                text = when (tab) {
                    AddFoodViewModel.Tab.PHOTO -> stringResource(R.string.add_tab_photo)
                    AddFoodViewModel.Tab.TEXT -> stringResource(R.string.add_tab_text)
                    AddFoodViewModel.Tab.BARCODE -> stringResource(R.string.add_tab_barcode)
                },
                icon = when (tab) {
                    AddFoodViewModel.Tab.PHOTO -> "📸"
                    AddFoodViewModel.Tab.TEXT -> "✨"
                    AddFoodViewModel.Tab.BARCODE -> "🏷️"
                },
                selected = selected == tab,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
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
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(14.dp),
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
private fun ResultCard(result: FoodAnalysisResult, onConfirm: () -> Unit) {
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
