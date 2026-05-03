package app.calsnap.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.calsnap.android.data.database.entity.FavouriteFoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteFoodDao {

    @Query("SELECT * FROM favourite_food ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<FavouriteFoodEntity>>

    @Query("SELECT * FROM favourite_food ORDER BY updatedAt DESC")
    suspend fun listAll(): List<FavouriteFoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FavouriteFoodEntity)

    @Query("DELETE FROM favourite_food WHERE favouriteKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM favourite_food")
    suspend fun wipe()
}
