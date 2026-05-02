package app.calsnap.android.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.calsnap.android.data.model.MealType

/**
 * One diary entry. Photo/source data is optional — text- and barcode-derived
 * entries don't have an image path, and favourites store just the macros.
 */
@Entity(tableName = "food_log")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Unix ms — source of truth for both date and in-day sort order. */
    val loggedAt: Long,
    val foodName: String,
    val portion: String?,
    val calories: Int,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val mealType: MealType,
    val servings: Float = 1f,
    /** Local image file path (scoped storage) or null. */
    val imagePath: String? = null,
    val ingredients: String? = null,
    val source: Source = Source.MANUAL,
    val barcode: String? = null,
    val isDrink: Boolean = false,
    val drinkMl: Int? = null,
    val favourite: Boolean = false,
) {
    enum class Source { MANUAL, PHOTO_AI, TEXT_AI, BARCODE, DATABASE, FAVOURITE }
}
