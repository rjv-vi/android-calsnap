package app.calsnap.android.presentation.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.repository.FoodLogRepository
import app.calsnap.android.data.repository.UserRepository
import app.calsnap.android.data.repository.WaterRepository
import app.calsnap.android.data.remote.GeminiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val gemini: GeminiClient,
    private val keyStore: SecureKeyStore,
    private val userRepository: UserRepository,
    private val foodLogRepository: FoodLogRepository,
    private val waterRepository: WaterRepository,
) : ViewModel() {

    data class ChatMessage(val text: String, val fromUser: Boolean)

    data class UiState(
        val hasApiKey: Boolean = false,
        val input: String = "",
        val loading: Boolean = false,
        val error: String? = null,
        val messages: List<ChatMessage> = emptyList(),
    )

    private val _ui = MutableStateFlow(
        UiState(
            hasApiKey = keyStore.hasGeminiKey(),
            messages = listOf(ChatMessage("Привет! Я помогу разобрать питание, воду и цели на сегодня.", false)),
        ),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun updateInput(value: String) = _ui.update { it.copy(input = value) }

    fun send() {
        val text = _ui.value.input.trim()
        if (text.isBlank() || _ui.value.loading) return
        _ui.update {
            it.copy(
                input = "",
                loading = true,
                error = null,
                messages = it.messages + ChatMessage(text, true),
            )
        }
        viewModelScope.launch {
            runCatching {
                val profile = userRepository.profile.first()
                val entries = foodLogRepository.observeToday().first()
                val water = waterRepository.observeToday().first().sumOf { it.milliliters }
                val food = entries.joinToString("; ") { "${it.foodName} ${it.calories} ккал" }.ifBlank { "пока нет записей" }
                val system = buildString {
                    append("Ты русскоязычный AI-нутрициолог CalSnap. ")
                    append("Отвечай коротко, дружелюбно и практично. ")
                    append("Профиль: ${profile?.name ?: "пользователь"}, цель ${profile?.kcalGoal ?: 2000} ккал, ")
                    append("белки ${profile?.proteinGoal ?: 100}г, углеводы ${profile?.carbsGoal ?: 250}г, жиры ${profile?.fatGoal ?: 60}г. ")
                    append("Сегодня: $food. Вода: ${water}мл.")
                }
                gemini.generateText(
                    modelName = "gemini-2.0-flash-lite",
                    prompt = text,
                    systemInstruction = system,
                )
            }.onSuccess { answer ->
                _ui.update { it.copy(loading = false, messages = it.messages + ChatMessage(answer, false)) }
            }.onFailure { error ->
                _ui.update { it.copy(loading = false, error = error.message) }
            }
        }
    }

    fun refreshKeyState() = _ui.update { it.copy(hasApiKey = keyStore.hasGeminiKey()) }
}
