package app.calsnap.android.data.remote

import android.graphics.Bitmap
import app.calsnap.android.data.model.FoodAnalysisResult
import app.calsnap.android.data.preferences.SecureKeyStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over the Google Generative AI SDK. Keeps the SDK surface
 * area at one choke point so we can swap providers later (e.g. Firebase AI
 * Logic or a server proxy) without touching repositories or ViewModels.
 */
@Singleton
class GeminiClient @Inject constructor(
    private val keyStore: SecureKeyStore,
    private val json: Json,
) {
    class NoApiKeyException : IllegalStateException("Gemini API key is not set. Add it in Settings → API.")

    /** Models we fall back through in order, cheapest first. */
    private val defaultFallbackChain = listOf(
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash",
        "gemini-2.5-flash",
    )

    private fun buildModel(name: String, preferJson: Boolean): GenerativeModel {
        val apiKey = keyStore.getGeminiApiKey() ?: throw NoApiKeyException()
        return GenerativeModel(
            modelName        = name,
            apiKey           = apiKey,
            generationConfig = generationConfig {
                temperature     = 0.2f
                maxOutputTokens = 2048
                if (preferJson) responseMimeType = "application/json"
            },
        )
    }

    /**
     * Single-shot structured text generation. Walks the fallback chain
     * if an upstream model is unavailable / rate-limited.
     */
    suspend fun <T> generateJson(
        serializer: DeserializationStrategy<T>,
        prompt: String,
        systemInstruction: String? = null,
        modelName: String? = null,
    ): T {
        val chain = modelName?.let(::listOf) ?: defaultFallbackChain
        var lastError: Throwable? = null
        for (model in chain) {
            runCatching {
                val text = generateText(model, prompt, systemInstruction, preferJson = true)
                return json.decodeFromString(serializer, text)
            }.onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("Gemini: all models failed")
    }

    suspend fun generateText(
        modelName: String,
        prompt: String,
        systemInstruction: String? = null,
        preferJson: Boolean = false,
    ): String {
        val model = buildModel(modelName, preferJson)
        val contents = mutableListOf<Content>().apply {
            if (systemInstruction != null) add(content(role = "user") { text(systemInstruction) })
            add(content { text(prompt) })
        }
        return model.generateContent(*contents.toTypedArray()).text
            ?: error("Gemini returned empty response")
    }

    /** Photo → FoodAnalysisResult. Used by the AddFood/Photo tab. */
    suspend fun analyzeFoodPhoto(
        bitmap: Bitmap,
        userHint: String?,
        modelName: String? = null,
    ): FoodAnalysisResult {
        val chain = modelName?.let(::listOf) ?: defaultFallbackChain
        var lastError: Throwable? = null
        for (model in chain) {
            runCatching {
                val generative = buildModel(model, preferJson = true)
                val result = generative.generateContent(
                    content {
                        image(bitmap)
                        text(buildPhotoPrompt(userHint))
                    },
                )
                val raw = result.text ?: error("empty")
                return json.decodeFromString(FoodAnalysisResult.serializer(), raw)
            }.onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("Gemini photo: all models failed")
    }

    private fun buildPhotoPrompt(userHint: String?): String = buildString {
        append("Ты — русскоязычный ассистент-нутрициолог. Распознай блюдо на фото ")
        append("и оцени его состав. Верни СТРОГИЙ JSON по схеме: ")
        append("{food: string, portion: string, calories: int, protein: float, ")
        append("fat: float, carbs: float, description: string, ingredients: [string]}. ")
        append("Порция — краткая строка вроде \"1 тарелка 300г\". ")
        append("Если не уверен в размере порции, выбери медиану разумного диапазона.")
        if (!userHint.isNullOrBlank()) {
            append(" Подсказка пользователя: «$userHint».")
        }
    }
}
