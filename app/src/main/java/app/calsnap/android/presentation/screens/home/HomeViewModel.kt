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
import kotlinx.coroutines.flow.stateIn
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
        val totalCalories: Int = 0,
        val totalProtein: Float = 0f,
        val totalCarbs: Float = 0f,
        val totalFat: Float = 0f,
        val waterMl: Int = 0,
        val waterGoalMl: Int = 2000,
    )

    val ui: StateFlow<UiState> = combine(
        userRepository.profile,
        logRepository.observeToday(),
        waterRepository.observeToday(),
    ) { profile, entries, water ->
        val waterGoal = ((profile?.weightKg ?: 70f) * 35f).toInt().coerceIn(1500, 3500)
        UiState(
            profile       = profile,
            entries       = entries,
            totalCalories = entries.sumOf { it.calories },
            totalProtein  = entries.fold(0f) { acc, e -> acc + e.protein },
            totalCarbs    = entries.fold(0f) { acc, e -> acc + e.carbs },
            totalFat      = entries.fold(0f) { acc, e -> acc + e.fat },
            waterMl       = water.sumOf { it.milliliters },
            waterGoalMl   = waterGoal,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())
}
