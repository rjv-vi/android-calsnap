package app.calsnap.android.domain

import app.calsnap.android.data.database.entity.FoodLogEntity
import java.util.Locale
import kotlin.math.roundToInt

object FavouriteFood {

    data class Key(val name: String, val caloriesPerServing: Int)

    fun key(entry: FoodLogEntity): Key =
        Key(
            name = entry.foodName.trim().lowercase(Locale.ROOT),
            caloriesPerServing = perServing(entry.calories.toFloat(), entry.servings)
                .roundToInt()
                .coerceAtLeast(0),
        )

    fun normalizedServing(entry: FoodLogEntity): FoodLogEntity {
        val servings = entry.servings.takeIf { it > 0f } ?: 1f
        if (servings == 1f) return entry
        return entry.copy(
            servings = 1f,
            calories = perServing(entry.calories.toFloat(), servings).roundToInt().coerceAtLeast(0),
            protein = perServing(entry.protein, servings).coerceAtLeast(0f),
            carbs = perServing(entry.carbs, servings).coerceAtLeast(0f),
            fat = perServing(entry.fat, servings).coerceAtLeast(0f),
        )
    }

    fun withServingMultiplier(entry: FoodLogEntity, multiplier: Float): FoodLogEntity {
        val base = normalizedServing(entry)
        val safeMultiplier = multiplier.coerceIn(0.25f, 5f)
        return base.copy(
            servings = safeMultiplier,
            calories = (base.calories * safeMultiplier).roundToInt().coerceAtLeast(0),
            protein = (base.protein * safeMultiplier).coerceAtLeast(0f),
            carbs = (base.carbs * safeMultiplier).coerceAtLeast(0f),
            fat = (base.fat * safeMultiplier).coerceAtLeast(0f),
        )
    }

    fun same(a: FoodLogEntity, b: FoodLogEntity): Boolean = key(a) == key(b)

    private fun perServing(value: Float, servings: Float): Float =
        value / (servings.takeIf { it > 0f } ?: 1f)
}
