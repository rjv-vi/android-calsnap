package app.calsnap.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.calsnap.android.data.database.entity.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_log ORDER BY loggedAt DESC")
    fun observeAll(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weight_log ORDER BY loggedAt DESC")
    suspend fun listAll(): List<WeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weight: WeightEntity): Long

    @Query("DELETE FROM weight_log")
    suspend fun wipe()
}
