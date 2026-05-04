package app.calsnap.android.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.calsnap.android.data.database.entity.WaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query(
        """
        SELECT * FROM water_log
        WHERE loggedAt BETWEEN :startOfDayMs AND :endOfDayMs
        ORDER BY loggedAt ASC
        """
    )
    fun observeDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water_log ORDER BY loggedAt DESC")
    suspend fun listAll(): List<WaterEntity>

    @Query(
        """
        SELECT COALESCE(SUM(milliliters), 0) FROM water_log
        WHERE loggedAt BETWEEN :startOfDayMs AND :endOfDayMs
        """
    )
    suspend fun totalForDay(startOfDayMs: Long, endOfDayMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: WaterEntity): Long

    @Delete
    suspend fun delete(entry: WaterEntity)

    @Query("DELETE FROM water_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM water_log")
    suspend fun wipe()
}
