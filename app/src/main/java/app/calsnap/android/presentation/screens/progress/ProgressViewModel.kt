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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    userRepository: UserRepository,
    foodLogRepository: FoodLogRepository,
    waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {

    data class DaySummary(
        val date: LocalDate,
        val calories: Int,
        val hasLog: Boolean,
    )

    data class UiState(
        val profile: UserProfile? = null,
        val streak: Int = 0,
        val bmi: Float? = null,
        val days: List<DaySummary> = emptyList(),
        val waterMl: Int = 0,
        val waterGoalMl: Int = 2000,
        val weights: List<WeightEntity> = emptyList(),
        val weightDraft: String = "",
    )

    private val weightDraft = MutableStateFlow("")

    val ui: StateFlow<UiState> = combine(
        userRepository.profile,
        foodLogRepository.observeLastDays(28),
        waterRepository.observeToday(),
        weightRepository.observeAll(),
        weightDraft,
    ) { profile, logs, water, weights, draft ->
        val today = LocalDate.now()
        val grouped = logs.groupBy { it.localDate() }
        val daySummaries = (27L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            val entries = grouped[date].orEmpty()
            DaySummary(
                date = date,
                calories = entries.sumOf(FoodLogEntity::calories),
                hasLog = entries.isNotEmpty(),
            )
        }
        val loggedDates = grouped.keys
        val streak = StreakCalculator.calculate(today = today) { it in loggedDates }.streak
        val waterGoal = ((profile?.weightKg ?: 70f) * 35f).toInt().coerceIn(1500, 3500)
        UiState(
            profile = profile,
            streak = streak,
            bmi = profile?.let { p ->
                val meters = p.heightCm / 100f
                if (meters > 0f) p.weightKg / (meters * meters) else null
            },
            days = daySummaries,
            waterMl = water.sumOf { it.milliliters },
            waterGoalMl = waterGoal,
            weights = weights,
            weightDraft = draft,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun addWater(milliliters: Int) = viewModelScope.launch {
        waterRepository.add(milliliters)
    }

    fun updateWeightDraft(value: String) {
        weightDraft.update { value.filter { it.isDigit() || it == '.' }.take(5) }
    }

    fun saveWeight() {
        val kg = weightDraft.value.toFloatOrNull() ?: return
        viewModelScope.launch {
            weightRepository.add(kg)
            weightDraft.value = ""
        }
    }

    private fun FoodLogEntity.localDate(): LocalDate =
        Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault()).toLocalDate()
}
