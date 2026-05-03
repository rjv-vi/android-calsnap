package app.calsnap.android.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favourite_food",
    indices = [Index(value = ["favouriteKey"], unique = true)],
)
data class FavouriteFoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val favouriteKey: String,
    val createdAt: Long,
    val updatedAt: Long,
    val foodName: String,
    val portion: String?,
    val calories: Int,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val ingredients: String? = null,
    val source: FoodLogEntity.Source = FoodLogEntity.Source.MANUAL,
    val barcode: String? = null,
    val isDrink: Boolean = false,
    val drinkMl: Int? = null,
)
