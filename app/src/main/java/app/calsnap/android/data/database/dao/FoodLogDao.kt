package app.calsnap.android.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.calsnap.android.data.database.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    /** Today's entries ordered by time. Home screen subscribes. */
    @Query(
        """
        SELECT * FROM food_log
        WHERE loggedAt BETWEEN :startOfDayMs AND :endOfDayMs
        ORDER BY loggedAt ASC
        """
    )
    fun observeDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<FoodLogEntity>>

    @Query(
        """
        SELECT * FROM food_log
        WHERE loggedAt BETWEEN :startMs AND :endMs
        ORDER BY loggedAt DESC
        """
    )
    fun observeRange(startMs: Long, endMs: Long): Flow<List<FoodLogEntity>>

    /** Favourites chip row on the add sheet. */
    @Query("SELECT * FROM food_log WHERE favourite = 1 ORDER BY loggedAt DESC LIMIT 30")
    fun observeFavourites(): Flow<List<FoodLogEntity>>

    @Query("SELECT COUNT(*) FROM food_log WHERE favourite = 1 AND lower(foodName) = lower(:foodName) AND calories = :calories")
    suspend fun countFavouritesLike(foodName: String, calories: Int): Int

    @Query("UPDATE food_log SET favourite = 0 WHERE favourite = 1 AND lower(foodName) = lower(:foodName) AND calories = :calories")
    suspend fun clearFavouritesLike(foodName: String, calories: Int)

    /** Aggregate totals for the streak / progress views. */
    @Query(
        """
        SELECT COALESCE(SUM(calories), 0) FROM food_log
        WHERE loggedAt BETWEEN :startOfDayMs AND :endOfDayMs
        """
    )
    suspend fun totalCaloriesForDay(startOfDayMs: Long, endOfDayMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: FoodLogEntity): Long

    @Update
    suspend fun update(entry: FoodLogEntity)

    @Delete
    suspend fun delete(entry: FoodLogEntity)

    @Query("DELETE FROM food_log")
    suspend fun wipe()
}
