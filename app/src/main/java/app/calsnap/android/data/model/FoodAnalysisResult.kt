package app.calsnap.android.data.model

import kotlinx.serialization.Serializable

/**
 * Result shape returned by Gemini for photo/text/barcode analysis. Mirrors
 * the JSON contract the PWA prompts for in assets/js/gemini.js → `pj()`.
 * Kotlinx-serialization lets us bind this directly to Gemini's structured
 * output (`responseMimeType = "application/json"` + schema).
 */
@Serializable
data class FoodAnalysisResult(
    val food: String = "Блюдо",
    val portion: String = "",
    val calories: Int = 0,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val carbs: Float = 0f,
    val description: String = "",
    val ingredients: List<String> = emptyList(),
)
