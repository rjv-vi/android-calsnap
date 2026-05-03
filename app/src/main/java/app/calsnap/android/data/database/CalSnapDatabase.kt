package app.calsnap.android.data.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import app.calsnap.android.data.database.dao.FavouriteFoodDao
import app.calsnap.android.data.database.dao.FoodLogDao
import app.calsnap.android.data.database.dao.WaterDao
import app.calsnap.android.data.database.dao.WeightDao
import app.calsnap.android.data.database.entity.FavouriteFoodEntity
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.database.entity.WaterEntity
import app.calsnap.android.data.database.entity.WeightEntity
import app.calsnap.android.data.model.MealType

@Database(
    entities = [FoodLogEntity::class, FavouriteFoodEntity::class, WeightEntity::class, WaterEntity::class],
    version  = 3,
    exportSchema = true,
)
@TypeConverters(CalSnapConverters::class)
abstract class CalSnapDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun favouriteFoodDao(): FavouriteFoodDao
    abstract fun weightDao(): WeightDao
    abstract fun waterDao(): WaterDao

    companion object {
        const val NAME = "calsnap.db"
    }
}

class CalSnapConverters {
    @TypeConverter fun fromMealType(m: MealType?): String = (m ?: MealType.SNACK).name
    @TypeConverter fun toMealType(s: String?): MealType =
        runCatching { MealType.valueOf(s.orEmpty().uppercase()) }.getOrDefault(MealType.SNACK)

    @TypeConverter fun fromSource(s: FoodLogEntity.Source?): String = (s ?: FoodLogEntity.Source.MANUAL).name
    @TypeConverter fun toSource(s: String?): FoodLogEntity.Source =
        runCatching { FoodLogEntity.Source.valueOf(s.orEmpty().uppercase()) }.getOrDefault(FoodLogEntity.Source.MANUAL)
}

object CalSnapMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `food_log` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `loggedAt` INTEGER NOT NULL,
                    `foodName` TEXT NOT NULL,
                    `portion` TEXT,
                    `calories` INTEGER NOT NULL,
                    `protein` REAL NOT NULL,
                    `fat` REAL NOT NULL,
                    `carbs` REAL NOT NULL,
                    `mealType` TEXT NOT NULL,
                    `servings` REAL NOT NULL DEFAULT 1.0,
                    `imagePath` TEXT,
                    `ingredients` TEXT,
                    `source` TEXT NOT NULL DEFAULT 'MANUAL',
                    `barcode` TEXT,
                    `isDrink` INTEGER NOT NULL DEFAULT 0,
                    `drinkMl` INTEGER,
                    `favourite` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `weight_log` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `loggedAt` INTEGER NOT NULL,
                    `weightKg` REAL NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `water_log` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `loggedAt` INTEGER NOT NULL,
                    `milliliters` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.addColumnIfMissing("food_log", "servings", "REAL NOT NULL DEFAULT 1.0")
            db.addColumnIfMissing("food_log", "imagePath", "TEXT")
            db.addColumnIfMissing("food_log", "ingredients", "TEXT")
            db.addColumnIfMissing("food_log", "source", "TEXT NOT NULL DEFAULT 'MANUAL'")
            db.addColumnIfMissing("food_log", "barcode", "TEXT")
            db.addColumnIfMissing("food_log", "isDrink", "INTEGER NOT NULL DEFAULT 0")
            db.addColumnIfMissing("food_log", "drinkMl", "INTEGER")
            db.addColumnIfMissing("food_log", "favourite", "INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.createFavouriteFoodTable()
            db.execSQL(
                """
                INSERT OR REPLACE INTO `favourite_food` (
                    `favouriteKey`,
                    `createdAt`,
                    `updatedAt`,
                    `foodName`,
                    `portion`,
                    `calories`,
                    `protein`,
                    `fat`,
                    `carbs`,
                    `ingredients`,
                    `source`,
                    `barcode`,
                    `isDrink`,
                    `drinkMl`
                )
                SELECT
                    lower(trim(`foodName`)) || '|' ||
                        CAST(ROUND(`calories` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END) AS INTEGER) || '|' ||
                        CAST(ROUND((`protein` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END) * 10.0) AS INTEGER) || '|' ||
                        CAST(ROUND((`carbs` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END) * 10.0) AS INTEGER) || '|' ||
                        CAST(ROUND((`fat` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END) * 10.0) AS INTEGER) AS `favouriteKey`,
                    MIN(`loggedAt`),
                    MAX(`loggedAt`),
                    `foodName`,
                    `portion`,
                    CAST(ROUND(`calories` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END) AS INTEGER),
                    `protein` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END,
                    `fat` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END,
                    `carbs` / CASE WHEN `servings` > 0 THEN `servings` ELSE 1.0 END,
                    `ingredients`,
                    `source`,
                    `barcode`,
                    `isDrink`,
                    `drinkMl`
                FROM `food_log`
                WHERE `favourite` = 1
                GROUP BY `favouriteKey`
                """.trimIndent(),
            )
        }
    }

    private fun SupportSQLiteDatabase.addColumnIfMissing(table: String, column: String, definition: String) {
        val hasColumn = query("PRAGMA table_info($table)").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .any { it.getString(it.getColumnIndexOrThrow("name")) == column }
        }
        if (!hasColumn) execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
    }

    private fun SupportSQLiteDatabase.createFavouriteFoodTable() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `favourite_food` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `favouriteKey` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `foodName` TEXT NOT NULL,
                `portion` TEXT,
                `calories` INTEGER NOT NULL,
                `protein` REAL NOT NULL,
                `fat` REAL NOT NULL,
                `carbs` REAL NOT NULL,
                `ingredients` TEXT,
                `source` TEXT NOT NULL DEFAULT 'MANUAL',
                `barcode` TEXT,
                `isDrink` INTEGER NOT NULL DEFAULT 0,
                `drinkMl` INTEGER
            )
            """.trimIndent(),
        )
        execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favourite_food_favouriteKey` ON `favourite_food` (`favouriteKey`)")
    }
}
