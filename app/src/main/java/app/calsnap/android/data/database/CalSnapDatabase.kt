package app.calsnap.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import app.calsnap.android.data.database.dao.FoodLogDao
import app.calsnap.android.data.database.dao.WaterDao
import app.calsnap.android.data.database.dao.WeightDao
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.database.entity.WaterEntity
import app.calsnap.android.data.database.entity.WeightEntity
import app.calsnap.android.data.model.MealType

@Database(
    entities = [FoodLogEntity::class, WeightEntity::class, WaterEntity::class],
    version  = 1,
    exportSchema = true,
)
@TypeConverters(CalSnapConverters::class)
abstract class CalSnapDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun weightDao(): WeightDao
    abstract fun waterDao(): WaterDao

    companion object {
        const val NAME = "calsnap.db"
    }
}

class CalSnapConverters {
    @TypeConverter fun fromMealType(m: MealType): String = m.name
    @TypeConverter fun toMealType(s: String): MealType = MealType.valueOf(s)

    @TypeConverter fun fromSource(s: FoodLogEntity.Source): String = s.name
    @TypeConverter fun toSource(s: String): FoodLogEntity.Source = FoodLogEntity.Source.valueOf(s)
}
