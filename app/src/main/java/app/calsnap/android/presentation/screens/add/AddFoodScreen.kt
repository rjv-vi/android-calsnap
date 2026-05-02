package app.calsnap.android.presentation.screens.add

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_food_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            val tabs = AddFoodViewModel.Tab.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { i, tab ->
                    SegmentedButton(
                        selected = ui.tab == tab,
                        onClick  = { viewModel.selectTab(tab) },
                        shape    = SegmentedButtonDefaults.itemShape(index = i, count = tabs.size),
                    ) {
                        Text(
                            text = when (tab) {
                                AddFoodViewModel.Tab.PHOTO   -> stringResource(R.string.add_tab_photo)
                                AddFoodViewModel.Tab.TEXT    -> stringResource(R.string.add_tab_text)
                                AddFoodViewModel.Tab.BARCODE -> stringResource(R.string.add_tab_barcode)
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (!ui.hasApiKey && ui.tab != AddFoodViewModel.Tab.BARCODE) {
                ApiKeyMissingCard()
            } else {
                when (ui.tab) {
                    AddFoodViewModel.Tab.PHOTO   -> PhotoTab(viewModel)
                    AddFoodViewModel.Tab.TEXT    -> TextTab(viewModel, ui.loading)
                    AddFoodViewModel.Tab.BARCODE -> BarcodeTab(viewModel, ui.loading)
                }
            }
            if (ui.loading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            ui.result?.let { result ->
                Spacer(Modifier.height(16.dp))
                ResultCard(result) {
                    viewModel.confirmAndLog(result, ui.resultSource)
                    onDismiss()
                }
            }
            ui.error?.let { err ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BarcodeTab(viewModel: AddFoodViewModel, loading: Boolean) {
    var code by remember { mutableStateOf("") }
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(18) },
        label = { Text(stringResource(R.string.add_barcode_hint)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { viewModel.lookupBarcode(code) },
        modifier = Modifier.fillMaxWidth(),
        enabled = code.length >= 8 && !loading,
    ) {
        Text(stringResource(R.string.add_lookup_barcode))
    }
}

@Composable
private fun PhotoTab(viewModel: AddFoodViewModel) {
    val context = LocalContext.current
    var hint by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedName = uri.lastPathSegment
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.onSuccess { bitmap ->
            viewModel.analyzePhoto(bitmap, hint)
        }.onFailure { error ->
            viewModel.setError(error.message)
        }
    }
    OutlinedTextField(
        value = hint,
        onValueChange = { hint = it },
        label = { Text(stringResource(R.string.add_photo_hint)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { picker.launch("image/*") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.add_pick_photo))
    }
    selectedName?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ApiKeyMissingCard() {
    Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🔑", style = MaterialTheme.typography.displayMedium)
        Text(
            text = stringResource(R.string.api_key_needed_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.api_key_needed_sub),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PhotoTabStub() {
    // TODO: wire CameraX + ML Kit barcode fallback. See TASKS.md § Photo AI.
    Text(stringResource(R.string.add_photo_stub))
}

@Composable
private fun TextTab(viewModel: AddFoodViewModel, loading: Boolean) {
    var input by remember { mutableStateOf("") }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text(stringResource(R.string.add_text_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { viewModel.analyzeText(input) },
        modifier = Modifier.fillMaxWidth(),
        enabled = input.isNotBlank() && !loading,
    ) {
        Text(stringResource(R.string.add_analyze_with_ai))
    }
}

@Composable
private fun BarcodeTabStub() {
    // TODO: wire ML Kit barcode scanner. See TASKS.md § Barcode.
    Text(stringResource(R.string.add_barcode_stub))
}

@Composable
private fun ResultCard(result: FoodAnalysisResult, onConfirm: () -> Unit) {
    Column {
        Text(result.food, style = MaterialTheme.typography.titleLarge)
        if (result.portion.isNotBlank()) {
            Text(result.portion, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text("${result.calories} ${stringResource(R.string.unit_kcal)}", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Б ${result.protein} · У ${result.carbs} · Ж ${result.fat} ${stringResource(R.string.unit_g)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_confirm_add))
        }
    }
}
