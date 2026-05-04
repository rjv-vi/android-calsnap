package app.calsnap.android.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.MealType
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.repository.FoodLogRepository
import app.calsnap.android.data.repository.UserRepository
import app.calsnap.android.domain.FavouriteFood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class HomeViewModel @Inject constructor(
    userRepository: UserRepository,
    private val logRepository: FoodLogRepository,
    private val keyStore: SecureKeyStore,
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
        val hasApiKey: Boolean = true,
    )

    data class CalendarDay(
        val date: LocalDate,
        val calories: Int,
        val hasLog: Boolean,
    )

    private val selectedDay = MutableStateFlow(LocalDate.now())
    private val apiKeyRevision = MutableStateFlow(0)
    private val selectedDayAndApiKey = combine(selectedDay, apiKeyRevision) { day, _ -> day }
    private val selectedEntries = selectedDay.flatMapLatest { day ->
        combine(
            logRepository.observeDay(day).catch { emit(emptyList()) },
            logRepository.observeFavourites().catch { emit(emptyList()) },
        ) { entries, favourites ->
            entries.withFavouriteMarkers(favourites)
        }
    }

    val ui: StateFlow<UiState> = combine(
        userRepository.profile.catch { emit(null) },
        selectedEntries,
        logRepository.observeLastDays(14).catch { emit(emptyList()) },
        selectedDayAndApiKey,
    ) { profile, entries, lastDays, selected ->
        val today = LocalDate.now()
        val grouped = lastDays.mapNotNull { entry ->
            entry.localDateOrNull()?.let { date -> date to entry }
        }.groupBy({ it.first }, { it.second })
        UiState(
            profile       = profile,
            entries       = entries,
            selectedDay   = selected,
            calendarDays  = (13L downTo 0L).map { offset ->
                val date = today.minusDays(offset)
                val dayEntries = grouped[date].orEmpty()
                CalendarDay(date, dayEntries.sumOf { it.calories }, dayEntries.isNotEmpty())
            },
            totalCalories = entries.sumOf { it.calories },
            totalProtein  = entries.fold(0f) { acc, e -> acc + e.protein },
            totalCarbs    = entries.fold(0f) { acc, e -> acc + e.carbs },
            totalFat      = entries.fold(0f) { acc, e -> acc + e.fat },
            hasApiKey     = safeHasGeminiKey(),
        )
    }.catch {
        emit(UiState(selectedDay = selectedDay.value, hasApiKey = false))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun selectDay(day: LocalDate) {
        selectedDay.update { day }
    }

    fun saveGeminiKey(apiKey: String) {
        keyStore.setGeminiApiKey(apiKey)
        apiKeyRevision.update { it + 1 }
    }

    fun toggleFavourite(entry: FoodLogEntity) = viewModelScope.launch {
        if (logRepository.hasFavouriteLike(entry)) {
            logRepository.clearFavouritesLike(entry)
        } else {
            logRepository.addFavourite(entry)
        }
    }

    fun deleteEntry(entry: FoodLogEntity) = viewModelScope.launch {
        logRepository.delete(entry)
    }

    fun updateEntry(entry: FoodLogEntity) = viewModelScope.launch {
        logRepository.update(entry)
    }

    fun updateServings(entry: FoodLogEntity, servings: Float) = viewModelScope.launch {
        val currentServings = entry.servings.takeIf { it > 0f } ?: 1f
        val nextServings = servings.coerceIn(0.5f, 5f)
        val ratio = nextServings / currentServings
        logRepository.update(
            entry.copy(
                servings = nextServings,
                calories = (entry.calories * ratio).roundToInt().coerceAtLeast(0),
                protein = entry.protein * ratio,
                carbs = entry.carbs * ratio,
                fat = entry.fat * ratio,
                favourite = false,
            ),
        )
    }

    fun updateEntryFromEdit(
        entry: FoodLogEntity,
        foodName: String,
        portion: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        timeText: String,
        mealType: MealType,
    ) = viewModelScope.launch {
        val time = runCatching { LocalTime.parse(timeText) }
            .getOrElse { Instant.ofEpochMilli(entry.loggedAt).atZone(ZoneId.systemDefault()).toLocalTime() }
        val day = Instant.ofEpochMilli(entry.loggedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val loggedAt = LocalDateTime.of(day, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        logRepository.update(
            entry.copy(
                loggedAt = loggedAt,
                foodName = foodName.ifBlank { entry.foodName },
                portion = portion.ifBlank { entry.portion },
                calories = calories.coerceAtLeast(0),
                protein = protein.coerceAtLeast(0f),
                carbs = carbs.coerceAtLeast(0f),
                fat = fat.coerceAtLeast(0f),
                mealType = mealType,
                servings = 1f,
                favourite = false,
            ),
        )
    }

    private fun FoodLogEntity.localDateOrNull(): LocalDate? =
        runCatching { Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()

    private fun safeHasGeminiKey(): Boolean = runCatching { keyStore.hasGeminiKey() }.getOrDefault(false)

    private fun List<FoodLogEntity>.withFavouriteMarkers(favourites: List<FoodLogEntity>): List<FoodLogEntity> {
        val favouriteKeys = favourites.map { FavouriteFood.stableKey(it) }.toSet()
        return map { entry ->
            val favourite = FavouriteFood.stableKey(entry) in favouriteKeys
            if (entry.favourite == favourite) entry else entry.copy(favourite = favourite)
        }
    }
}
