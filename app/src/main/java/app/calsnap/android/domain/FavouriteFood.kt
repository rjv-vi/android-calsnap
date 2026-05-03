package app.calsnap.android.domain

import app.calsnap.android.data.database.entity.FavouriteFoodEntity
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.model.MealType
import java.util.Locale
import kotlin.math.roundToInt

object FavouriteFood {

    data class Key(
        val name: String,
        val caloriesPerServing: Int,
        val proteinPerServing: Int,
        val carbsPerServing: Int,
        val fatPerServing: Int,
    )

    fun key(entry: FoodLogEntity): Key =
        key(
            name = entry.foodName,
            calories = entry.calories,
            protein = entry.protein,
            carbs = entry.carbs,
            fat = entry.fat,
            servings = entry.servings,
        )

    fun key(entry: FavouriteFoodEntity): Key =
        key(
            name = entry.foodName,
            calories = entry.calories,
            protein = entry.protein,
            carbs = entry.carbs,
            fat = entry.fat,
            servings = 1f,
        )

    fun stableKey(entry: FoodLogEntity): String = key(entry).stableKey()

    fun stableKey(entry: FavouriteFoodEntity): String = key(entry).stableKey()

    fun toFavouriteEntity(entry: FoodLogEntity, now: Long = System.currentTimeMillis()): FavouriteFoodEntity {
        val base = normalizedServing(entry)
        return FavouriteFoodEntity(
            favouriteKey = stableKey(base),
            createdAt = now,
            updatedAt = now,
            foodName = base.foodName,
            portion = base.portion,
            calories = base.calories,
            protein = base.protein,
            fat = base.fat,
            carbs = base.carbs,
            ingredients = base.ingredients,
            source = base.source,
            barcode = base.barcode,
            isDrink = base.isDrink,
            drinkMl = base.drinkMl,
        )
    }

    fun toFoodLogEntity(entry: FavouriteFoodEntity): FoodLogEntity =
        FoodLogEntity(
            id = entry.id,
            loggedAt = entry.updatedAt,
            foodName = entry.foodName,
            portion = entry.portion,
            calories = entry.calories,
            protein = entry.protein,
            fat = entry.fat,
            carbs = entry.carbs,
            mealType = MealType.SNACK,
            servings = 1f,
            ingredients = entry.ingredients,
            source = entry.source,
            barcode = entry.barcode,
            isDrink = entry.isDrink,
            drinkMl = entry.drinkMl,
            favourite = true,
        )

    private fun key(
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        servings: Float,
    ): Key {
        val safeServings = servings.takeIf { it > 0f } ?: 1f
        return Key(
            name = name.trim().lowercase(Locale.ROOT),
            caloriesPerServing = perServing(calories.toFloat(), safeServings)
                .roundToInt()
                .coerceAtLeast(0),
            proteinPerServing = macroKey(protein, safeServings),
            carbsPerServing = macroKey(carbs, safeServings),
            fatPerServing = macroKey(fat, safeServings),
        )
    }

    private fun Key.stableKey(): String =
        listOf(name, caloriesPerServing, proteinPerServing, carbsPerServing, fatPerServing).joinToString("|")

    private fun macroKey(value: Float, servings: Float): Int =
        (perServing(value, servings) * 10f).roundToInt().coerceAtLeast(0)

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

    fun same(a: FavouriteFoodEntity, b: FoodLogEntity): Boolean =
        stableKey(a) == stableKey(b)

    private fun perServing(value: Float, servings: Float): Float =
        value / (servings.takeIf { it > 0f } ?: 1f)
}
