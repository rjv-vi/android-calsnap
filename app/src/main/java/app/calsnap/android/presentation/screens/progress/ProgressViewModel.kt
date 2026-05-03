package app.calsnap.android.presentation.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.database.entity.WeightEntity
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.repository.FoodLogRepository
import app.calsnap.android.data.repository.UserRepository
import app.calsnap.android.data.repository.WaterRepository
import app.calsnap.android.data.repository.WeightRepository
import app.calsnap.android.domain.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val userRepository: UserRepository,
    foodLogRepository: FoodLogRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {

    data class DaySummary(
        val date: LocalDate,
        val calories: Int,
        val hasLog: Boolean,
    )

    data class WeekDay(
        val date: LocalDate,
        val hasLog: Boolean,
        val isToday: Boolean,
    )

    data class WaterEvent(
        val id: Long,
        val milliliters: Int,
        val loggedAt: Long,
    )

    data class WeightPoint(
        val date: LocalDate,
        val weightKg: Float,
    )

    data class UiState(
        val profile: UserProfile? = null,
        val streak: Int = 0,
        val bmi: Float? = null,
        val days: List<DaySummary> = emptyList(),
        val weekDays: List<WeekDay> = emptyList(),
        val averageCalories7: Int? = null,
        val bestDay: LocalDate? = null,
        val totalEntries: Int = 0,
        val daysWithData30: Int = 0,
        val waterMl: Int = 0,
        val waterGoalMl: Int = 2000,
        val waterBaseGoalMl: Int = 2000,
        val waterHasSalt: Boolean = false,
        val waterEvents: List<WaterEvent> = emptyList(),
        val latestWeightKg: Float? = null,
        val previousWeightKg: Float? = null,
        val weightPoints: List<WeightPoint> = emptyList(),
        val weightDraft: String = "",
    ) {
        val weightDeltaKg: Float? = latestWeightKg?.let { latest -> previousWeightKg?.let { latest - it } }
    }

    private val weightDraft = MutableStateFlow("")

    val ui: StateFlow<UiState> = combine(
        userRepository.profile.catch { emit(null) },
        foodLogRepository.observeLastDays(30).catch { emit(emptyList()) },
        waterRepository.observeToday().catch { emit(emptyList()) },
        weightRepository.observeAll().catch { emit(emptyList()) },
        weightDraft,
    ) { profile, logs, water, weights, draft ->
        val today = LocalDate.now()
        val grouped = logs.mapNotNull { entry ->
            entry.localDateOrNull()?.let { date -> date to entry }
        }.groupBy({ it.first }, { it.second })
        val daySummaries = (27L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            val entries = grouped[date].orEmpty()
            DaySummary(
                date = date,
                calories = entries.sumOf(FoodLogEntity::calories),
                hasLog = entries.isNotEmpty(),
            )
        }
        val weekDays = (6L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            WeekDay(date = date, hasLog = grouped[date].orEmpty().isNotEmpty(), isToday = date == today)
        }
        val loggedDates = grouped.keys
        val streak = StreakCalculator.calculate(today = today) { it in loggedDates }.streak
        val goal = profile?.kcalGoal ?: 2000
        val last7 = (0L..6L).map { offset -> grouped[today.minusDays(offset)].orEmpty() }
        val active7 = last7.filter { it.isNotEmpty() }
        val average7 = active7.takeIf { it.isNotEmpty() }
            ?.let { active -> active.sumOf { entries -> entries.sumOf(FoodLogEntity::calories) } / active.size }
        val bestDay = (0L..29L).mapNotNull { offset ->
            val date = today.minusDays(offset)
            val entries = grouped[date].orEmpty()
            if (entries.isEmpty()) null else date to abs(entries.sumOf(FoodLogEntity::calories) - goal)
        }.minByOrNull { it.second }?.first
        val todayEntries = grouped[today].orEmpty()
        val baseWaterGoal = ((((profile?.weightKg ?: 70f) * 30f) / 50f).roundToInt() * 50).coerceIn(1500, 3500)
        val hasSalt = todayEntries.any { entry ->
            val food = entry.foodName.lowercase(Locale.ROOT)
            saltyWords.any { food.contains(it) }
        }
        val adjustedWaterGoal = if (hasSalt) ((baseWaterGoal * 1.2f / 50f).roundToInt() * 50) else baseWaterGoal
        val latestWeight = weights.firstOrNull()?.weightKg
        val previousWeight = weights.drop(1).firstOrNull()?.weightKg
        UiState(
            profile = profile,
            streak = streak,
            bmi = profile?.let { p ->
                val meters = p.heightCm / 100f
                if (meters > 0f) p.weightKg / (meters * meters) else null
            },
            days = daySummaries,
            weekDays = weekDays,
            averageCalories7 = average7,
            bestDay = bestDay,
            totalEntries = logs.size,
            daysWithData30 = (0L..29L).count { grouped[today.minusDays(it)].orEmpty().isNotEmpty() },
            waterMl = water.sumOf { it.milliliters },
            waterGoalMl = adjustedWaterGoal,
            waterBaseGoalMl = baseWaterGoal,
            waterHasSalt = hasSalt,
            waterEvents = water.map { WaterEvent(id = it.id, milliliters = it.milliliters, loggedAt = it.loggedAt) },
            latestWeightKg = latestWeight,
            previousWeightKg = previousWeight,
            weightPoints = weights.take(30).mapNotNull { entity ->
                entity.localDateOrNull()?.let { WeightPoint(date = it, weightKg = entity.weightKg) }
            }.sortedBy { it.date },
            weightDraft = draft,
        )
    }.catch {
        emit(UiState(weightDraft = weightDraft.value))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun addWater(milliliters: Int) = viewModelScope.launch {
        runCatching { waterRepository.add(milliliters) }
    }

    fun undoLastWater() = viewModelScope.launch {
        val id = ui.value.waterEvents.lastOrNull()?.id ?: return@launch
        runCatching { waterRepository.deleteById(id) }
    }

    fun prepareWeightDraft() {
        val value = ui.value.latestWeightKg ?: ui.value.profile?.weightKg ?: 70f
        weightDraft.value = String.format(Locale.US, "%.1f", value)
    }

    fun stepWeight(deltaKg: Float) {
        val current = weightDraft.value.toFloatOrNull() ?: ui.value.latestWeightKg ?: ui.value.profile?.weightKg ?: 70f
        weightDraft.value = String.format(Locale.US, "%.1f", (current + deltaKg).coerceIn(20f, 300f))
    }

    fun updateWeightDraft(value: String) {
        val normalized = value.replace(',', '.')
        val cleaned = buildString {
            normalized.forEach { ch ->
                if (ch.isDigit() || (ch == '.' && !contains('.'))) append(ch)
            }
        }.take(5)
        weightDraft.update { cleaned }
    }

    fun saveWeight() {
        val kg = weightDraft.value.toFloatOrNull()?.coerceIn(20f, 300f) ?: return
        viewModelScope.launch {
            runCatching {
                weightRepository.add(kg)
                userRepository.updateWeight(kg)
            }.onSuccess { weightDraft.value = "" }
        }
    }

    private fun FoodLogEntity.localDateOrNull(): LocalDate? =
        runCatching { Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()

    private fun WeightEntity.localDateOrNull(): LocalDate? =
        runCatching { Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()

    private companion object {
        val saltyWords = listOf(
            "чипсы", "солен", "солён", "соль", "рыба", "сыр", "колбас", "пицца", "бургер", "хот-дог", "соев", "рамен",
            "chips", "salty", "salt", "fish", "cheese", "sausage", "pizza", "burger", "hot dog", "soy", "ramen",
        )
    }
}
