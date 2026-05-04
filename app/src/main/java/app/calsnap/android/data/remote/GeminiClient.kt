package app.calsnap.android.data.remote

import android.graphics.Bitmap
import app.calsnap.android.data.model.FoodAnalysisResult
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(
    private val keyStore: SecureKeyStore,
    private val preferences: UserPreferences,
    private val json: Json,
    private val client: OkHttpClient,
) {
    data class GeminiModelInfo(
        val id: String,
        val name: String,
        val description: String,
    )

    class NoApiKeyException : IllegalStateException("Gemini API key is not set. Add it in Settings → API.")
    class GeminiApiException(val statusCode: Int, message: String) : IllegalStateException(message)

    private val defaultFallbackChain = listOf(
        "gemini-flash-lite-latest",
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash",
        "gemini-2.5-flash",
    )

    suspend fun fetchModels(): List<GeminiModelInfo> {
        val apiKey = keyStore.getGeminiApiKey() ?: throw NoApiKeyException()
        val request = Request.Builder()
            .url("$BASE_URL/models?key=$apiKey&pageSize=100")
            .get()
            .build()
        val root = json.parseToJsonElement(execute(request)).jsonObject
        return root["models"]?.jsonArray.orEmpty()
            .mapNotNull { element ->
                val model = element.jsonObject
                val id = model["name"]?.jsonPrimitive?.content?.removePrefix("models/") ?: return@mapNotNull null
                val methods = model["supportedGenerationMethods"]?.jsonArray.orEmpty()
                    .map { it.jsonPrimitive.content }
                val lower = id.lowercase()
                if ("generateContent" !in methods) return@mapNotNull null
                if (listOf("embedding", "aqa", "retrieval").any(lower::contains)) return@mapNotNull null
                GeminiModelInfo(
                    id = id,
                    name = model["displayName"]?.jsonPrimitive?.content ?: id,
                    description = model["description"]?.jsonPrimitive?.content.orEmpty(),
                )
            }
            .sortedWith(compareByDescending<GeminiModelInfo> { "flash" in it.id.lowercase() }.thenBy { it.name })
    }

    suspend fun <T> generateJson(
        serializer: DeserializationStrategy<T>,
        prompt: String,
        systemInstruction: String? = null,
        modelName: String? = null,
    ): T {
        val raw = generateTextWithFallback(
            prompt = prompt,
            systemInstruction = systemInstruction,
            modelName = modelName,
            preferJson = true,
        )
        return json.decodeFromString(serializer, sanitizeJson(raw))
    }

    suspend fun generateText(
        modelName: String,
        prompt: String,
        systemInstruction: String? = null,
        preferJson: Boolean = false,
    ): String = generateTextWithFallback(
        prompt = prompt,
        systemInstruction = systemInstruction,
        modelName = modelName,
        preferJson = preferJson,
    )

    suspend fun generateTextWithFallback(
        prompt: String,
        systemInstruction: String? = null,
        modelName: String? = null,
        preferJson: Boolean = false,
    ): String {
        var lastError: Throwable? = null
        for (model in modelChain(modelName)) {
            runCatching {
                return requestGenerateContent(
                    modelName = model,
                    parts = listOf(textPart(prompt)),
                    systemInstruction = systemInstruction,
                    preferJson = preferJson,
                )
            }.onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("Gemini: all models failed")
    }

    suspend fun analyzeFoodText(
        text: String,
        modelName: String? = null,
    ): FoodAnalysisResult {
        val raw = generateTextWithFallback(
            prompt = "Описание пользователя: \"$text\". Оцени одну реалистичную порцию, калории и БЖУ. Верни только JSON.",
            systemInstruction = "Ты русскоязычный нутрициолог CalSnap. JSON-схема: {\"food\":\"string\",\"portion\":\"string\",\"calories\":0,\"protein\":0,\"fat\":0,\"carbs\":0,\"description\":\"string\",\"ingredients\":[\"string\"]}. Не добавляй markdown.",
            modelName = modelName,
            preferJson = true,
        )
        return decodeFoodAnalysis(raw)
    }

    suspend fun analyzeFoodPhoto(
        bitmap: Bitmap,
        userHint: String?,
        modelName: String? = null,
    ): FoodAnalysisResult {
        var lastError: Throwable? = null
        val imagePart = imagePart(bitmap)
        for (model in modelChain(modelName)) {
            runCatching {
                val raw = requestGenerateContent(
                    modelName = model,
                    parts = listOf(textPart(buildPhotoPrompt(userHint)), imagePart),
                    preferJson = true,
                )
                return decodeFoodAnalysis(raw)
            }.onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("Gemini photo: all models failed")
    }

    suspend fun analyzeBarcodePhoto(
        bitmap: Bitmap,
        modelName: String? = null,
    ): FoodAnalysisResult {
        var lastError: Throwable? = null
        val imagePart = imagePart(bitmap)
        for (model in modelChain(modelName)) {
            runCatching {
                val raw = requestGenerateContent(
                    modelName = model,
                    parts = listOf(textPart(buildBarcodePhotoPrompt()), imagePart),
                    preferJson = true,
                )
                return decodeFoodAnalysis(raw)
            }.onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("Gemini barcode photo: all models failed")
    }

    private suspend fun modelChain(modelName: String?): List<String> {
        val selected = modelName ?: preferences.geminiModel.first()
        return (listOf(selected) + defaultFallbackChain)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private suspend fun requestGenerateContent(
        modelName: String,
        parts: List<JsonObject>,
        systemInstruction: String? = null,
        preferJson: Boolean,
    ): String {
        val apiKey = keyStore.getGeminiApiKey() ?: throw NoApiKeyException()
        val body = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        parts.forEach { add(it) }
                    }
                })
            }
            if (!systemInstruction.isNullOrBlank()) {
                putJsonObject("system_instruction") {
                    putJsonArray("parts") {
                        add(textPart(systemInstruction))
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                put("maxOutputTokens", 2048)
                if (preferJson) put("responseMimeType", "application/json")
            }
        }
        val request = Request.Builder()
            .url("$BASE_URL/models/$modelName:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        return extractText(execute(request))
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw GeminiApiException(response.code, parseError(response.code, body))
            body
        }
    }

    private fun extractText(raw: String): String {
        val root = json.parseToJsonElement(raw).jsonObject
        val parts = root["candidates"]?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            .orEmpty()
        val text = parts.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }.trim()
        if (text.isBlank()) error("Gemini returned empty response")
        return text
    }

    private fun textPart(text: String): JsonObject = buildJsonObject {
        put("text", text)
    }

    private fun imagePart(bitmap: Bitmap): JsonObject {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        return buildJsonObject {
            putJsonObject("inline_data") {
                put("mime_type", "image/jpeg")
                put("data", base64)
            }
        }
    }

    private fun sanitizeJson(raw: String): String {
        var value = raw.trim()
            .replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```$"), "")
            .trim()
        val objectStart = value.indexOf('{')
        val arrayStart = value.indexOf('[')
        val start = listOf(objectStart, arrayStart).filter { it >= 0 }.minOrNull() ?: return value
        val end = if (start == objectStart) value.lastIndexOf('}') else value.lastIndexOf(']')
        if (end > start) value = value.substring(start, end + 1)
        return value
    }

    private fun decodeFoodAnalysis(raw: String): FoodAnalysisResult {
        val sanitized = sanitizeJson(raw)
        return runCatching {
            json.decodeFromString(FoodAnalysisResult.serializer(), sanitized)
        }.getOrElse {
            repairFoodAnalysis(sanitized)
        }
    }

    private fun repairFoodAnalysis(raw: String): FoodAnalysisResult {
        fun stringField(name: String): String? =
            Regex("\"$name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .find(raw)
                ?.groupValues
                ?.get(1)
                ?.replace("\\\"", "\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\\", "\\")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        fun floatField(name: String): Float? =
            Regex("\"$name\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
                .find(raw)
                ?.groupValues
                ?.get(1)
                ?.toFloatOrNull()

        fun intField(name: String): Int? = floatField(name)?.toInt()

        val ingredients = Regex("\"ingredients\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
            .find(raw)
            ?.groupValues
            ?.get(1)
            ?.let { body ->
                Regex("\"((?:\\\\.|[^\"\\\\])*)\"")
                    .findAll(body)
                    .map { it.groupValues[1].replace("\\\"", "\"").trim() }
                    .filter { it.isNotBlank() }
                    .toList()
            }
            .orEmpty()

        return FoodAnalysisResult(
            food = stringField("food") ?: stringField("name") ?: "Продукт",
            portion = stringField("portion").orEmpty(),
            calories = intField("calories") ?: intField("kcal") ?: 0,
            protein = floatField("protein") ?: floatField("prot") ?: 0f,
            fat = floatField("fat") ?: 0f,
            carbs = floatField("carbs") ?: floatField("carb") ?: 0f,
            description = stringField("description").orEmpty(),
            ingredients = ingredients,
        )
    }

    private fun parseError(statusCode: Int, body: String): String {
        val message = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        return when (statusCode) {
            400 -> "Неверный API ключ или запрос Gemini"
            403 -> "Доступ к Gemini запрещён — проверь API ключ"
            429 -> "Превышен лимит Gemini — попробуй позже или выбери другую модель"
            else -> message ?: "Ошибка Gemini API: $statusCode"
        }
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

    private fun buildBarcodePhotoPrompt(): String =
        "Ты — русскоязычный нутрициолог. На фото штрихкод или упаковка продукта. " +
            "Определи продукт и пищевую ценность на порцию или 100г. Верни СТРОГИЙ JSON по схеме: " +
            "{food: string, portion: string, calories: int, protein: float, fat: float, carbs: float, description: string, ingredients: [string]}. " +
            "Если виден только штрихкод, используй его как подсказку и оцени наиболее вероятный продукт."

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
