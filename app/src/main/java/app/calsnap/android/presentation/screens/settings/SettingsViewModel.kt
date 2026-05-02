package app.calsnap.android.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.preferences.UserPreferences
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
) : ViewModel() {

    data class UiState(
        val darkTheme: Boolean? = null,
        val language: String = "ru",
        val hasGeminiKey: Boolean = false,
    )

    private val _ui = MutableStateFlow(UiState(hasGeminiKey = keyStore.hasGeminiKey()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.darkTheme.collect { v -> _ui.update { it.copy(darkTheme = v) } }
        }
        viewModelScope.launch {
            prefs.language.collect { v -> _ui.update { it.copy(language = v) } }
        }
    }

    fun saveGeminiKey(apiKey: String) {
        keyStore.setGeminiApiKey(apiKey)
        _ui.update { it.copy(hasGeminiKey = keyStore.hasGeminiKey()) }
    }

    fun clearGeminiKey() {
        keyStore.setGeminiApiKey(null)
        _ui.update { it.copy(hasGeminiKey = false) }
    }

    fun setDarkTheme(on: Boolean?) = viewModelScope.launch { prefs.setDarkTheme(on) }
    fun setLanguage(code: String)  = viewModelScope.launch { prefs.setLanguage(code) }
}
