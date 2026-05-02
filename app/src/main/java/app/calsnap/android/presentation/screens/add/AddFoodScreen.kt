package app.calsnap.android.presentation.screens.add

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.model.FoodAnalysisResult

@Composable
fun AddFoodScreen(
    onDismiss: () -> Unit,
    viewModel: AddFoodViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshKeyState() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_food_title), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.add_food_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }

            AddTabs(selected = ui.tab, onSelect = viewModel::selectTab)

            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!ui.hasApiKey && ui.tab != AddFoodViewModel.Tab.BARCODE) {
                        ApiKeyMissingCard()
                    } else {
                        AnimatedContent(targetState = ui.tab, label = "addTab") { tab ->
                            when (tab) {
                                AddFoodViewModel.Tab.PHOTO -> PhotoTab(viewModel, ui.loading)
                                AddFoodViewModel.Tab.TEXT -> TextTab(viewModel, ui.loading)
                                AddFoodViewModel.Tab.BARCODE -> BarcodeTab(viewModel, ui.loading)
                            }
                        }
                    }
                    if (ui.loading) LoadingRow()
                    ui.error?.let { ErrorCard(it) }
                }
            }

            ui.result?.let { result ->
                ResultCard(result) {
                    viewModel.confirmAndLog(result, ui.resultSource)
                    onDismiss()
                }
            }
            Spacer(Modifier.height(84.dp))
        }
    }
}

@Composable
private fun AddTabs(selected: AddFoodViewModel.Tab, onSelect: (AddFoodViewModel.Tab) -> Unit) {
    val tabs = AddFoodViewModel.Tab.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (tab) {
                        AddFoodViewModel.Tab.PHOTO -> stringResource(R.string.add_tab_photo)
                        AddFoodViewModel.Tab.TEXT -> stringResource(R.string.add_tab_text)
                        AddFoodViewModel.Tab.BARCODE -> stringResource(R.string.add_tab_barcode)
                    },
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BarcodeTab(viewModel: AddFoodViewModel, loading: Boolean) {
    var code by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🏷️", style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.add_barcode_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.add_barcode_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(18) },
            label = { Text(stringResource(R.string.add_barcode_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.lookupBarcode(code) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📸", style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.add_photo_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.add_photo_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = hint,
            onValueChange = { hint = it },
            label = { Text(stringResource(R.string.add_photo_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Button(
            onClick = { picker.launch("image/*") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !loading,
        ) {
            Text(stringResource(R.string.add_pick_photo))
        }
        selectedName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun TextTab(viewModel: AddFoodViewModel, loading: Boolean) {
    var input by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("✨", style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.add_text_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.add_text_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.add_text_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        Button(
            onClick = { viewModel.analyzeText(input) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = input.isNotBlank() && !loading,
        ) {
            Text(stringResource(R.string.add_analyze_with_ai))
        }
    }
}

@Composable
private fun ApiKeyMissingCard() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🔑", style = MaterialTheme.typography.displayMedium)
        Text(stringResource(R.string.api_key_needed_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.api_key_needed_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingRow() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
        Text(stringResource(R.string.add_analyzing), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun ResultCard(result: FoodAnalysisResult, onConfirm: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("✅ ${result.food}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            if (result.portion.isNotBlank()) Text(result.portion, style = MaterialTheme.typography.bodyMedium)
            Text("${result.calories} ${stringResource(R.string.unit_kcal)}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroPill("Б", result.protein)
                MacroPill("У", result.carbs)
                MacroPill("Ж", result.fat)
            }
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.add_confirm_add))
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, value: Float) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text("$label ${value.toInt()}${stringResource(R.string.unit_g)}", fontWeight = FontWeight.Bold)
    }
}
