package app.calsnap.android.presentation.screens.add

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.FoodAnalysisResult
import app.calsnap.android.data.model.MealType
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.remote.GeminiClient
import app.calsnap.android.data.remote.OpenFoodFactsApi
import app.calsnap.android.data.repository.FoodLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val gemini: GeminiClient,
    private val keyStore: SecureKeyStore,
    private val foodLogRepository: FoodLogRepository,
    private val openFoodFactsApi: OpenFoodFactsApi,
) : ViewModel() {

    enum class Tab { PHOTO, TEXT, BARCODE, FAVOURITES }

    data class UiState(
        val tab: Tab = Tab.PHOTO,
        val hasApiKey: Boolean = false,
        val loading: Boolean = false,
        val result: FoodAnalysisResult? = null,
        val resultSource: FoodLogEntity.Source = FoodLogEntity.Source.TEXT_AI,
        val favourites: List<FoodLogEntity> = emptyList(),
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState(hasApiKey = keyStore.hasGeminiKey()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            foodLogRepository.observeFavourites().catch { emit(emptyList()) }.collect { favourites ->
                _ui.update { it.copy(favourites = favourites) }
            }
        }
    }

    fun selectTab(tab: Tab) = _ui.update { it.copy(tab = tab, result = null, error = null) }
    fun refreshKeyState() = _ui.update { it.copy(hasApiKey = keyStore.hasGeminiKey()) }
    fun resetTransientState() = _ui.update { it.copy(loading = false, result = null, error = null) }

    fun analyzeText(text: String) {
        if (text.isBlank()) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                gemini.generateJson(
                    serializer = FoodAnalysisResult.serializer(),
                    prompt = "Описание пользователя: \"$text\". Оцени одну реалистичную порцию, калории и БЖУ. Верни только JSON.",
                    systemInstruction = "Ты русскоязычный нутрициолог CalSnap. JSON-схема: {\"food\":\"string\",\"portion\":\"string\",\"calories\":0,\"protein\":0,\"fat\":0,\"carbs\":0,\"description\":\"string\",\"ingredients\":[\"string\"]}. Не добавляй markdown.",
                )
            }.onSuccess { r ->
                _ui.update { it.copy(loading = false, result = r, resultSource = FoodLogEntity.Source.TEXT_AI) }
            }.onFailure { t ->
                _ui.update { it.copy(loading = false, error = t.message) }
            }
        }
    }

    fun analyzePhoto(bitmap: Bitmap, hint: String?) {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                gemini.analyzeFoodPhoto(bitmap, hint)
            }.onSuccess { r ->
                _ui.update { it.copy(loading = false, result = r, resultSource = FoodLogEntity.Source.PHOTO_AI) }
            }.onFailure { t ->
                _ui.update { it.copy(loading = false, error = t.message) }
            }
        }
    }

    fun lookupBarcode(code: String) {
        val clean = code.filter { it.isDigit() }
        if (clean.isBlank()) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val product = openFoodFactsApi.fetchProduct(clean).product ?: error("Продукт не найден")
                val nutriments = product.nutriments
                FoodAnalysisResult(
                    food = product.product_name_ru
                        ?: product.product_name
                        ?: product.brands
                        ?: clean,
                    portion = product.quantity ?: "100 г",
                    calories = nutriments?.energy_kcal_100g?.roundToInt() ?: 0,
                    protein = nutriments?.proteins_100g ?: 0f,
                    carbs = nutriments?.carbohydrates_100g ?: 0f,
                    fat = nutriments?.fat_100g ?: 0f,
                    description = product.brands.orEmpty(),
                )
            }.onSuccess { r ->
                _ui.update { it.copy(loading = false, result = r, resultSource = FoodLogEntity.Source.BARCODE) }
            }.onFailure { t ->
                _ui.update { it.copy(loading = false, error = t.message) }
            }
        }
    }

    fun setError(message: String?) = _ui.update { it.copy(error = message) }

    fun confirmAndLog(result: FoodAnalysisResult, source: FoodLogEntity.Source, saveFavourite: Boolean = false) {
        val now = LocalDateTime.now()
        viewModelScope.launch {
            runCatching {
                foodLogRepository.add(
                    FoodLogEntity(
                        loggedAt  = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        foodName  = result.food,
                        portion   = result.portion,
                        calories  = result.calories,
                        protein   = result.protein,
                        fat       = result.fat,
                        carbs     = result.carbs,
                        mealType  = MealType.forHour(now.hour),
                        ingredients = result.ingredients.takeIf { it.isNotEmpty() }?.joinToString(", "),
                        source    = source,
                        favourite = saveFavourite,
                    ),
                )
            }.onFailure { error ->
                _ui.update { it.copy(error = error.message) }
            }
        }
    }

    fun logFavourite(entry: FoodLogEntity) {
        val now = LocalDateTime.now()
        viewModelScope.launch {
            foodLogRepository.add(
                entry.copy(
                    id = 0,
                    loggedAt = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    mealType = MealType.forHour(now.hour),
                    source = FoodLogEntity.Source.FAVOURITE,
                    favourite = false,
                ),
            )
        }
    }

    fun removeFavourite(entry: FoodLogEntity) {
        viewModelScope.launch {
            foodLogRepository.update(entry.copy(favourite = false))
        }
    }
}
