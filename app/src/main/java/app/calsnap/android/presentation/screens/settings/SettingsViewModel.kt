package app.calsnap.android.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.preferences.UserPreferences
import app.calsnap.android.data.remote.GeminiClient
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.repository.DataExportRepository
import app.calsnap.android.domain.BmrCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val keyStore: SecureKeyStore,
    private val geminiClient: GeminiClient,
    private val dataExportRepository: DataExportRepository,
) : ViewModel() {

    data class ExportPayload(
        val fileName: String,
        val mimeType: String,
        val content: String,
    )

    data class UiState(
        val darkTheme: Boolean? = null,
        val language: String = "ru",
        val soundOn: Boolean = true,
        val hapticOn: Boolean = true,
        val profile: UserProfile? = null,
        val hasGeminiKey: Boolean = false,
        val selectedModel: String = "gemini-2.0-flash-lite",
        val models: List<GeminiClient.GeminiModelInfo> = emptyList(),
        val modelsLoading: Boolean = false,
        val modelsError: String? = null,
        val exportLoading: Boolean = false,
        val exportPayload: ExportPayload? = null,
        val exportError: String? = null,
    )

    private val _ui = MutableStateFlow(UiState(hasGeminiKey = safeHasGeminiKey()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.darkTheme.collect { v -> _ui.update { it.copy(darkTheme = v) } }
        }
        viewModelScope.launch {
            prefs.language.collect { v -> _ui.update { it.copy(language = v) } }
        }
        viewModelScope.launch {
            prefs.soundOn.collect { v -> _ui.update { it.copy(soundOn = v) } }
        }
        viewModelScope.launch {
            prefs.hapticOn.collect { v -> _ui.update { it.copy(hapticOn = v) } }
        }
        viewModelScope.launch {
            prefs.profile.collect { v -> _ui.update { it.copy(profile = v) } }
        }
        viewModelScope.launch {
            prefs.geminiModel.collect { v -> _ui.update { it.copy(selectedModel = v) } }
        }
    }

    fun saveGeminiKey(apiKey: String) {
        keyStore.setGeminiApiKey(apiKey)
        _ui.update { it.copy(hasGeminiKey = safeHasGeminiKey()) }
        loadGeminiModels()
    }

    fun clearGeminiKey() {
        keyStore.setGeminiApiKey(null)
        _ui.update { it.copy(hasGeminiKey = false, models = emptyList(), modelsError = null) }
    }

    fun loadGeminiModels() {
        if (!safeHasGeminiKey()) return
        _ui.update { it.copy(modelsLoading = true, modelsError = null) }
        viewModelScope.launch {
            runCatching { geminiClient.fetchModels() }
                .onSuccess { models -> _ui.update { it.copy(modelsLoading = false, models = models) } }
                .onFailure { error -> _ui.update { it.copy(modelsLoading = false, modelsError = error.message) } }
        }
    }

    fun selectGeminiModel(modelId: String) = viewModelScope.launch {
        prefs.setGeminiModel(modelId)
    }

    fun setDarkTheme(on: Boolean?) = viewModelScope.launch { prefs.setDarkTheme(on) }
    fun setLanguage(code: String)  = viewModelScope.launch { prefs.setLanguage(code) }
    fun setSoundOn(on: Boolean) = viewModelScope.launch { prefs.setSoundOn(on) }
    fun setHapticOn(on: Boolean) = viewModelScope.launch { prefs.setHapticOn(on) }
    fun updateProfile(recalculateTargets: Boolean = false, update: (UserProfile) -> UserProfile) {
        viewModelScope.launch {
            val current = _ui.value.profile ?: return@launch
            val updated = update(current)
            prefs.saveProfile(if (recalculateTargets) updated.withCalculatedTargets() else updated)
        }
    }
    fun prepareJsonExport() = prepareExport("json")
    fun prepareCsvExport() = prepareExport("csv")
    fun consumeExport() = _ui.update { it.copy(exportPayload = null) }
    fun resetAllData() = viewModelScope.launch { dataExportRepository.resetAll() }

    private fun safeHasGeminiKey(): Boolean = runCatching { keyStore.hasGeminiKey() }.getOrDefault(false)

    private fun prepareExport(type: String) {
        _ui.update { it.copy(exportLoading = true, exportError = null, exportPayload = null) }
        viewModelScope.launch {
            runCatching {
                val date = java.time.LocalDate.now().toString()
                if (type == "json") {
                    ExportPayload(
                        fileName = "calsnap-backup-$date.json",
                        mimeType = "application/json",
                        content = dataExportRepository.exportJsonString(),
                    )
                } else {
                    ExportPayload(
                        fileName = "calsnap-$date.csv",
                        mimeType = "text/csv",
                        content = dataExportRepository.exportCsvString(),
                    )
                }
            }.onSuccess { payload ->
                _ui.update { it.copy(exportLoading = false, exportPayload = payload) }
            }.onFailure { error ->
                _ui.update { it.copy(exportLoading = false, exportError = error.message) }
            }
        }
    }

    private fun UserProfile.withCalculatedTargets(): UserProfile {
        val age = BmrCalculator.ageFromDob(dob)
        val kcal = (BmrCalculator.tdee(gender, weightKg, heightCm, age, activity) + goal.kcalDelta).coerceAtLeast(1200)
        val proteinG = (weightKg * 1.8f).toInt()
        val fatG = ((kcal * 0.25f) / 9f).toInt()
        val carbsG = ((kcal - proteinG * 4 - fatG * 9) / 4f).toInt()
        return copy(kcalGoal = kcal, proteinGoal = proteinG, carbsGoal = carbsG, fatGoal = fatG)
    }
}
