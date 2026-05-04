package app.calsnap.android.presentation.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.calsnap.android.R
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.domain.BmrCalculator
import app.calsnap.android.presentation.components.CalSnapAppleLogo
import app.calsnap.android.presentation.components.CalSnapPillTextField
import app.calsnap.android.presentation.components.CalSnapPrimaryButton
import app.calsnap.android.presentation.components.CalSnapScreen
import app.calsnap.android.presentation.components.CalSnapSecondaryButton
import app.calsnap.android.presentation.components.CalSnapStepDots
import app.calsnap.android.presentation.components.CalSnapSoundEffect
import app.calsnap.android.presentation.components.CalSnapWheelPicker
import app.calsnap.android.presentation.components.LocalCalSnapEffects
import app.calsnap.android.presentation.components.calSnapClickable
import app.calsnap.android.ui.theme.CalSnapStreak
import java.time.LocalDate
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val effects = LocalCalSnapEffects.current
    var height by remember(draft.heightCm) {
        mutableStateOf(if (draft.heightCm > 0f) draft.heightCm.toInt().toString() else "")
    }
    var weight by remember(draft.weightKg) {
        mutableStateOf(if (draft.weightKg > 0f) draft.weightKg.toInt().toString() else "")
    }
    val age = BmrCalculator.ageFromDob(draft.dob)
    val canFinish = draft.name.isNotBlank() && age in 5..120 && draft.heightCm in 80f..240f && draft.weightKg in 25f..300f
    val step = draft.step.coerceIn(1, 5)
    val canNext = when (step) {
        1 -> draft.name.isNotBlank()
        2 -> age in 5..120 && draft.heightCm in 80f..240f && draft.weightKg in 25f..300f
        else -> true
    }

    CalSnapScreen(glow = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .widthIn(max = 430.dp)
                .align(Alignment.TopCenter)
                .padding(top = 44.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoBlock()
            Spacer(Modifier.height(30.dp))
            CalSnapStepDots(step = step)
            Spacer(Modifier.height(26.dp))
            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                transitionSpec = {
                    (fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { it / 5 }) togetherWith
                        (fadeOut(tween(120, easing = FastOutSlowInEasing)) + slideOutHorizontally(tween(170, easing = FastOutSlowInEasing)) { -it / 6 })
                },
                label = "onboardingStep",
            ) { currentStep ->
                when (currentStep) {
                    1 -> StepIdentity(draft, viewModel)
                    2 -> StepBody(draft, viewModel, height, weight, { height = it }, { weight = it })
                    3 -> StepActivity(draft, viewModel)
                    4 -> StepGoal(draft, viewModel)
                    else -> StepPreferences(draft, viewModel)
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalSnapPrimaryButton(
                    onClick = {
                        if (step < 5) {
                            effects.sound.play(CalSnapSoundEffect.ObNext)
                            viewModel.update { it.copy(step = step + 1) }
                        } else {
                            effects.sound.play(CalSnapSoundEffect.ObFinish)
                            viewModel.finish(onFinished)
                        }
                    },
                    sound = null,
                    enabled = if (step < 5) canNext else canFinish,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                ) {
                    Text(
                        if (step < 5) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_finish),
                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
                    )
                }
                if (step > 1) {
                    CalSnapSecondaryButton(
                        onClick = {
                            effects.sound.play(CalSnapSoundEffect.Back)
                            viewModel.update { it.copy(step = step - 1) }
                        },
                        sound = null,
                        modifier = Modifier.fillMaxWidth(),
                        height = 52.dp,
                    ) {
                        Text(
                            stringResource(R.string.onboarding_back),
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoBlock() {
    val infinite = rememberInfiniteTransition(label = "logoFloat")
    val floatY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoFloatY",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.graphicsLayer { translationY = floatY }) {
            CalSnapAppleLogo(iconSize = 68.dp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "Cal",
                style = TextStyle(
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2.8).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
                "Snap",
                style = TextStyle(
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2.8).sp,
                    color = CalSnapStreak,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.onboarding_tagline),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.2).sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Text(
            subtitle,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun StepIdentity(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            title = stringResource(R.string.onboarding_welcome_title),
            subtitle = stringResource(R.string.onboarding_name_subtitle),
        )
        CalSnapPillTextField(
            value = draft.name,
            onValueChange = { value -> viewModel.update { it.copy(name = value.take(40)) } },
            placeholder = stringResource(R.string.onboarding_name_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StepBody(
    draft: OnboardingViewModel.Draft,
    viewModel: OnboardingViewModel,
    heightValue: String,
    weightValue: String,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            title = stringResource(R.string.onboarding_body_title),
            subtitle = stringResource(R.string.onboarding_body_subtitle),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            GenderTile(
                icon = "♂️",
                iconColor = Color(0xFF10B8AA),
                label = stringResource(R.string.onboarding_gender_male),
                selected = draft.gender == UserProfile.Gender.MALE,
                onClick = { viewModel.update { it.copy(gender = UserProfile.Gender.MALE) } },
                modifier = Modifier.weight(1f),
            )
            GenderTile(
                icon = "♀️",
                iconColor = Color(0xFFC026D3),
                label = stringResource(R.string.onboarding_gender_female),
                selected = draft.gender == UserProfile.Gender.FEMALE,
                onClick = { viewModel.update { it.copy(gender = UserProfile.Gender.FEMALE) } },
                modifier = Modifier.weight(1f),
            )
        }
        DobPickerField(
            dob = draft.dob,
            onDob = { value -> viewModel.update { it.copy(dob = value) } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CalSnapPillTextField(
                value = heightValue,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() }.take(3)
                    onHeight(cleaned)
                    viewModel.update { it.copy(heightCm = cleaned.toFloatOrNull() ?: 0f) }
                },
                placeholder = stringResource(R.string.onboarding_height_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            CalSnapPillTextField(
                value = weightValue,
                onValueChange = { raw ->
                    val cleaned = raw.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                    onWeight(cleaned)
                    viewModel.update { it.copy(weightKg = cleaned.toFloatOrNull() ?: 0f) }
                },
                placeholder = stringResource(R.string.onboarding_weight_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GenderTile(
    icon: String,
    iconColor: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    val brush = if (selected) {
        Brush.linearGradient(
            colors = if (dark) listOf(Color(0xFF3A3630), Color(0xFF1A1916))
            else listOf(Color(0xFF2E2A25), Color(0xFF141210)),
        )
    } else {
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
        )
    }
    val labelColor = if (selected) Color(0xFFF2F0EB)
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .height(122.dp)
            .clip(shape)
            .background(brush)
            .border(
                BorderStroke(
                    0.5.dp,
                    if (selected) Color.White.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                ),
                shape,
            )
            .calSnapClickable(pressedScale = 0.92f, sound = CalSnapSoundEffect.Select, onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            icon,
            style = TextStyle(fontSize = 32.sp, color = if (selected) Color(0xFFF2F0EB) else iconColor),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            label,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
            ),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DobPickerField(dob: String, onDob: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val parts = parseDob(dob)
    val hasDob = dob.isNotBlank()
    val shape = RoundedCornerShape(22.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.onboarding_dob_label).uppercase(),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
                    shape,
                )
                .calSnapClickable(pressedScale = 0.97f, sound = CalSnapSoundEffect.SheetOpen) { showPicker = true }
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (hasDob) displayDob(parts) else stringResource(R.string.onboarding_dob_pick),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = if (hasDob) FontWeight.Bold else FontWeight.Normal,
                    color = if (hasDob) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f),
                ),
            )
            Text(
                "📅",
                style = TextStyle(fontSize = 14.sp),
            )
        }
        if (hasDob) {
            Text(
                "✓ " + stringResource(R.string.onboarding_age_hint, ageFromDobParts(parts)),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A),
                ),
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.52f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            DobPickerSheet(
                initial = parts,
                onConfirm = { confirmed ->
                    onDob(formatDob(confirmed))
                    showPicker = false
                },
                onCancel = { showPicker = false },
            )
        }
    }
}

@Composable
private fun DobPickerSheet(
    initial: DobParts,
    onConfirm: (DobParts) -> Unit,
    onCancel: () -> Unit,
) {
    var day by remember(initial) { mutableStateOf(initial.day) }
    var month by remember(initial) { mutableStateOf(initial.month) }
    var year by remember(initial) { mutableStateOf(initial.year) }

    val dayItems = remember { (1..31).map { pad(it) } }
    val monthItems = remember { (1..12).map { monthName(it) } }
    val yearItems = remember { (maxBirthYear() downTo minBirthYear()).map { it.toString() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .calSnapClickable(pressedScale = 0.95f, onClick = onCancel),
            ) {
                Text(
                    stringResource(R.string.cancel),
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            Text(
                stringResource(R.string.onboarding_dob_label),
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.onSurface)
                        .calSnapClickable(pressedScale = 0.94f) {
                            val clampedDay = day.coerceIn(1, maxDay(year, month))
                            onConfirm(DobParts(year, month, clampedDay))
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text(
                        stringResource(R.string.done),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
            ) {
                DobWheelLabel(
                    label = stringResource(R.string.onboarding_dob_day).uppercase(),
                    modifier = Modifier.weight(1f),
                )
                DobWheelLabel(
                    label = stringResource(R.string.onboarding_dob_month).uppercase(),
                    modifier = Modifier.weight(1.4f),
                )
                DobWheelLabel(
                    label = stringResource(R.string.onboarding_dob_year).uppercase(),
                    modifier = Modifier.weight(1f),
                )
            }
            DobWheelsRow(
                dayItems = dayItems,
                monthItems = monthItems,
                yearItems = yearItems,
                dayIndex = (day - 1).coerceIn(0, dayItems.lastIndex),
                monthIndex = (month - 1).coerceIn(0, monthItems.lastIndex),
                yearIndex = (maxBirthYear() - year).coerceIn(0, yearItems.lastIndex),
                onDay = { idx -> day = (idx + 1).coerceIn(1, 31) },
                onMonth = { idx -> month = (idx + 1).coerceIn(1, 12) },
                onYear = { idx -> year = (maxBirthYear() - idx).coerceIn(minBirthYear(), maxBirthYear()) },
            )
        }
    }
}

@Composable
private fun DobWheelLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        modifier = modifier,
        style = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DobWheelsRow(
    dayItems: List<String>,
    monthItems: List<String>,
    yearItems: List<String>,
    dayIndex: Int,
    monthIndex: Int,
    yearIndex: Int,
    onDay: (Int) -> Unit,
    onMonth: (Int) -> Unit,
    onYear: (Int) -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val itemHeight = 44.dp
    val maskHeight = 88.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f))
                .border(
                    BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                    RoundedCornerShape(12.dp),
                ),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            DobWheelColumn(
                items = dayItems,
                selectedIndex = dayIndex,
                onSelect = onDay,
                modifier = Modifier.weight(1f),
            )
            DobWheelColumn(
                items = monthItems,
                selectedIndex = monthIndex,
                onSelect = onMonth,
                modifier = Modifier.weight(1.4f),
            )
            DobWheelColumn(
                items = yearItems,
                selectedIndex = yearIndex,
                onSelect = onYear,
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maskHeight)
                .background(
                    Brush.verticalGradient(
                        0f to surfaceColor,
                        0.2f to surfaceColor,
                        1f to surfaceColor.copy(alpha = 0f),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(maskHeight)
                .background(
                    Brush.verticalGradient(
                        0f to surfaceColor.copy(alpha = 0f),
                        0.8f to surfaceColor,
                        1f to surfaceColor,
                    ),
                ),
        )
    }
}

@Composable
private fun DobWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    CalSnapWheelPicker(
        items = items,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier.fillMaxWidth(),
        itemHeight = 44.dp,
        visibleCount = 5,
        showSelectionBand = false,
        showEdgeMasks = false,
    )
}

@Composable
private fun StepActivity(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            title = stringResource(R.string.onboarding_activity_title),
            subtitle = stringResource(R.string.onboarding_activity_subtitle),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActivityTile(
                icon = "🛋️",
                label = stringResource(R.string.onboarding_activity_sedentary),
                subtitle = stringResource(R.string.onboarding_activity_sedentary_sub),
                selected = draft.activity == UserProfile.Activity.SEDENTARY,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.SEDENTARY) } },
                modifier = Modifier.weight(1f),
            )
            ActivityTile(
                icon = "🚶",
                label = stringResource(R.string.onboarding_activity_light),
                subtitle = stringResource(R.string.onboarding_activity_light_sub),
                selected = draft.activity == UserProfile.Activity.LIGHT,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.LIGHT) } },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActivityTile(
                icon = "🏃",
                label = stringResource(R.string.onboarding_activity_moderate),
                subtitle = stringResource(R.string.onboarding_activity_moderate_sub),
                selected = draft.activity == UserProfile.Activity.MODERATE,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.MODERATE) } },
                modifier = Modifier.weight(1f),
            )
            ActivityTile(
                icon = "💪",
                label = stringResource(R.string.onboarding_activity_active),
                subtitle = stringResource(R.string.onboarding_activity_active_sub),
                selected = draft.activity == UserProfile.Activity.ACTIVE,
                onClick = { viewModel.update { it.copy(activity = UserProfile.Activity.ACTIVE) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActivityTile(
    icon: String,
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableTile(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(88.dp),
        paddingHorizontal = 14.dp,
        paddingVertical = 14.dp,
        centered = false,
    ) {
        Text(
            "$icon $label",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFFF2F0EB) else MaterialTheme.colorScheme.onSurface,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = if (selected) Color(0xFFF2F0EB).copy(alpha = 0.78f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun StepGoal(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            title = stringResource(R.string.onboarding_goal_title),
            subtitle = stringResource(R.string.onboarding_goal_subtitle),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GoalTile(
                icon = "📉",
                label = stringResource(R.string.onboarding_goal_lose),
                selected = draft.goal == UserProfile.Goal.LOSE,
                onClick = { viewModel.update { it.copy(goal = UserProfile.Goal.LOSE) } },
                modifier = Modifier.weight(1f),
            )
            GoalTile(
                icon = "⚖️",
                label = stringResource(R.string.onboarding_goal_maintain),
                selected = draft.goal == UserProfile.Goal.MAINTAIN,
                onClick = { viewModel.update { it.copy(goal = UserProfile.Goal.MAINTAIN) } },
                modifier = Modifier.weight(1f),
            )
            GoalTile(
                icon = "💪",
                label = stringResource(R.string.onboarding_goal_gain),
                selected = draft.goal == UserProfile.Goal.GAIN,
                onClick = { viewModel.update { it.copy(goal = UserProfile.Goal.GAIN) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GoalTile(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableTile(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(116.dp),
        paddingHorizontal = 8.dp,
        paddingVertical = 16.dp,
        centered = true,
    ) {
        Text(icon, style = TextStyle(fontSize = 28.sp))
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFFF2F0EB) else MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SelectableTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    paddingHorizontal: androidx.compose.ui.unit.Dp = 12.dp,
    paddingVertical: androidx.compose.ui.unit.Dp = 12.dp,
    centered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.25f
    val brush = if (selected) {
        Brush.linearGradient(
            colors = if (dark) listOf(Color(0xFF3A3630), Color(0xFF1A1916))
            else listOf(Color(0xFF2E2A25), Color(0xFF141210)),
        )
    } else {
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)),
        )
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .border(
                BorderStroke(
                    0.5.dp,
                    if (selected) Color.White.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                ),
                shape,
            )
            .calSnapClickable(pressedScale = 0.93f, sound = CalSnapSoundEffect.Select, onClick = onClick)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StepPreferences(draft: OnboardingViewModel.Draft, viewModel: OnboardingViewModel) {
    val prefs = listOf(
        "no_meat" to ("🚫🥩" to stringResource(R.string.pref_no_meat)),
        "no_gluten" to ("🚫🌾" to stringResource(R.string.pref_no_gluten)),
        "no_lactose" to ("🚫🥛" to stringResource(R.string.pref_no_lactose)),
        "no_sugar" to ("🚫🍭" to stringResource(R.string.pref_no_sugar)),
        "vegan" to ("🌱" to stringResource(R.string.pref_vegan)),
        "keto" to ("🥑" to stringResource(R.string.pref_keto)),
        "halal" to ("☪️" to stringResource(R.string.pref_halal)),
        "no_eggs" to ("🚫🥚" to stringResource(R.string.pref_no_eggs)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            title = stringResource(R.string.onboarding_preferences_title),
            subtitle = stringResource(R.string.onboarding_preferences_subtitle),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            prefs.forEach { (key, iconLabel) ->
                val (icon, label) = iconLabel
                PreferenceChip(
                    icon = icon,
                    label = label,
                    selected = key in draft.preferences,
                    onClick = {
                        viewModel.update {
                            val next = if (key in it.preferences) it.preferences - key
                            else it.preferences + key
                            it.copy(preferences = next)
                        }
                    },
                )
            }
        }
        Text(
            stringResource(R.string.onboarding_allergies_label),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        CalSnapPillTextField(
            value = draft.allergies,
            onValueChange = { value -> viewModel.update { it.copy(allergies = value.take(120)) } },
            placeholder = stringResource(R.string.onboarding_allergies_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PreferenceChip(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    val background = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val border = if (selected) Color.Transparent
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.5.dp, border), shape)
            .calSnapClickable(pressedScale = 0.93f, sound = CalSnapSoundEffect.Select, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(icon, style = TextStyle(fontSize = 13.sp))
        Text(
            label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.background
                else MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

// ── helpers ─────────────────────────────────────────────────────────────────

private data class DobParts(val year: Int, val month: Int, val day: Int)

private fun parseDob(dob: String): DobParts {
    val now = LocalDate.now()
    val values = dob.split('-').mapNotNull { it.toIntOrNull() }
    val fallback = DobParts(
        now.year - 20,
        now.monthValue,
        now.dayOfMonth.coerceAtMost(maxDay(now.year - 20, now.monthValue)),
    )
    if (values.size != 3) return fallback
    val year = values[0].coerceIn(minBirthYear(), maxBirthYear())
    val month = values[1].coerceIn(1, 12)
    val day = values[2].coerceIn(1, maxDay(year, month))
    return DobParts(year, month, day)
}

private fun displayDob(parts: DobParts): String =
    "${pad(parts.day)}.${pad(parts.month)}.${parts.year}"

private fun ageFromDobParts(parts: DobParts): Int =
    BmrCalculator.ageFromDob(formatDob(parts))

private fun formatDob(parts: DobParts): String {
    val day = parts.day.coerceIn(1, maxDay(parts.year, parts.month))
    return "${parts.year}-${pad(parts.month)}-${pad(day)}"
}

private fun pad(value: Int): String = value.toString().padStart(2, '0')

private fun monthName(month: Int): String {
    val ru = Locale.getDefault().language == "ru"
    return when (month.coerceIn(1, 12)) {
        1 -> if (ru) "Январь" else "January"
        2 -> if (ru) "Февраль" else "February"
        3 -> if (ru) "Март" else "March"
        4 -> if (ru) "Апрель" else "April"
        5 -> if (ru) "Май" else "May"
        6 -> if (ru) "Июнь" else "June"
        7 -> if (ru) "Июль" else "July"
        8 -> if (ru) "Август" else "August"
        9 -> if (ru) "Сентябрь" else "September"
        10 -> if (ru) "Октябрь" else "October"
        11 -> if (ru) "Ноябрь" else "November"
        else -> if (ru) "Декабрь" else "December"
    }
}

private fun minBirthYear(): Int = LocalDate.now().year - 120
private fun maxBirthYear(): Int = LocalDate.now().year - 5

private fun maxDay(year: Int, month: Int): Int = when (month) {
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}
