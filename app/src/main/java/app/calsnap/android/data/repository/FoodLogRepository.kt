package app.calsnap.android.data.repository

import app.calsnap.android.data.database.dao.FavouriteFoodDao
import app.calsnap.android.data.database.dao.FoodLogDao
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.domain.FavouriteFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodLogRepository @Inject constructor(
    private val dao: FoodLogDao,
    private val favouriteDao: FavouriteFoodDao,
) {
    fun observeToday(): Flow<List<FoodLogEntity>> = observeDay(LocalDate.now())

    fun observeDay(day: LocalDate): Flow<List<FoodLogEntity>> {
        val (start, end) = dayBounds(day)
        return dao.observeDay(start, end)
    }

    fun observeLastDays(days: Long): Flow<List<FoodLogEntity>> {
        val today = LocalDate.now()
        val (start, _) = dayBounds(today.minusDays(days - 1))
        val (_, end) = dayBounds(today)
        return dao.observeRange(start, end)
    }

    fun observeFavourites(): Flow<List<FoodLogEntity>> =
        favouriteDao.observeAll().map { entries ->
            entries
                .distinctBy { FavouriteFood.stableKey(it) }
                .take(30)
                .map { FavouriteFood.toFoodLogEntity(it) }
        }

    suspend fun add(entry: FoodLogEntity): Long = dao.insert(entry)
    suspend fun update(entry: FoodLogEntity)    = dao.update(entry)
    suspend fun delete(entry: FoodLogEntity)    = dao.delete(entry)
    suspend fun addFavourite(entry: FoodLogEntity) = favouriteDao.upsert(FavouriteFood.toFavouriteEntity(entry))
    suspend fun hasFavouriteLike(entry: FoodLogEntity): Boolean =
        favouriteDao.listAll().any { FavouriteFood.same(it, entry) }

    suspend fun clearFavouritesLike(entry: FoodLogEntity) {
        favouriteDao.listAll()
            .filter { FavouriteFood.same(it, entry) }
            .forEach { favouriteDao.deleteByKey(it.favouriteKey) }
        dao.listFavourites()
            .filter { FavouriteFood.stableKey(it) == FavouriteFood.stableKey(entry) }
            .forEach { dao.update(it.copy(favourite = false)) }
    }

    suspend fun caloriesForToday(): Int {
        val (start, end) = dayBounds(LocalDate.now())
        return dao.totalCaloriesForDay(start, end)
    }

    suspend fun wipe() {
        dao.wipe()
        favouriteDao.wipe()
    }

    private fun dayBounds(day: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}
