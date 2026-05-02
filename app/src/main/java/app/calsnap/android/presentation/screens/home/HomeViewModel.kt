package app.calsnap.android.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.repository.FoodLogRepository
import app.calsnap.android.data.repository.UserRepository
import app.calsnap.android.data.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    userRepository: UserRepository,
    logRepository: FoodLogRepository,
    waterRepository: WaterRepository,
) : ViewModel() {

    data class UiState(
        val profile: UserProfile? = null,
        val entries: List<FoodLogEntity> = emptyList(),
        val selectedDay: LocalDate = LocalDate.now(),
        val calendarDays: List<CalendarDay> = emptyList(),
        val totalCalories: Int = 0,
        val totalProtein: Float = 0f,
        val totalCarbs: Float = 0f,
        val totalFat: Float = 0f,
        val waterMl: Int = 0,
        val waterGoalMl: Int = 2000,
    )

    data class CalendarDay(
        val date: LocalDate,
        val calories: Int,
        val hasLog: Boolean,
    )

    private val selectedDay = MutableStateFlow(LocalDate.now())
    private val selectedEntries = selectedDay.flatMapLatest(logRepository::observeDay)

    val ui: StateFlow<UiState> = combine(
        userRepository.profile,
        selectedEntries,
        waterRepository.observeToday(),
        logRepository.observeLastDays(7),
        selectedDay,
    ) { profile, entries, water, lastDays, selected ->
        val waterGoal = ((profile?.weightKg ?: 70f) * 35f).toInt().coerceIn(1500, 3500)
        val today = LocalDate.now()
        val grouped = lastDays.groupBy { it.localDate() }
        UiState(
            profile       = profile,
            entries       = entries,
            selectedDay   = selected,
            calendarDays  = (6L downTo 0L).map { offset ->
                val date = today.minusDays(offset)
                val dayEntries = grouped[date].orEmpty()
                CalendarDay(date, dayEntries.sumOf { it.calories }, dayEntries.isNotEmpty())
            },
            totalCalories = entries.sumOf { it.calories },
            totalProtein  = entries.fold(0f) { acc, e -> acc + e.protein },
            totalCarbs    = entries.fold(0f) { acc, e -> acc + e.carbs },
            totalFat      = entries.fold(0f) { acc, e -> acc + e.fat },
            waterMl       = water.sumOf { it.milliliters },
            waterGoalMl   = waterGoal,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun selectDay(day: LocalDate) {
        selectedDay.update { day }
    }

    private fun FoodLogEntity.localDate(): LocalDate =
        Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault()).toLocalDate()
}
