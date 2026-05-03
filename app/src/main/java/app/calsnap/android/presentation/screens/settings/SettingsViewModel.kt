package app.calsnap.android.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.preferences.UserPreferences
import app.calsnap.android.data.remote.GeminiClient
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
) : ViewModel() {

    data class UiState(
        val darkTheme: Boolean? = null,
        val language: String = "ru",
        val hasGeminiKey: Boolean = false,
        val selectedModel: String = "gemini-2.0-flash-lite",
        val models: List<GeminiClient.GeminiModelInfo> = emptyList(),
        val modelsLoading: Boolean = false,
        val modelsError: String? = null,
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

    private fun safeHasGeminiKey(): Boolean = runCatching { keyStore.hasGeminiKey() }.getOrDefault(false)
}
