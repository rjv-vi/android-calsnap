package app.calsnap.android.data.repository

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
        dao.observeFavourites().map { entries ->
            entries
                .distinctBy { FavouriteFood.key(it) }
                .take(30)
                .map { FavouriteFood.normalizedServing(it).copy(favourite = true) }
        }

    suspend fun add(entry: FoodLogEntity): Long = dao.insert(entry)
    suspend fun update(entry: FoodLogEntity)    = dao.update(entry)
    suspend fun delete(entry: FoodLogEntity)    = dao.delete(entry)
    suspend fun isFavouriteById(entry: FoodLogEntity): Boolean = dao.favouriteFlag(entry.id) == 1
    suspend fun hasFavouriteLike(entry: FoodLogEntity): Boolean =
        dao.listFavourites().any { FavouriteFood.same(it, entry) }

    suspend fun clearFavouritesLike(entry: FoodLogEntity) {
        dao.listFavourites()
            .filter { FavouriteFood.same(it, entry) }
            .forEach { dao.update(it.copy(favourite = false)) }
    }

    suspend fun caloriesForToday(): Int {
        val (start, end) = dayBounds(LocalDate.now())
        return dao.totalCaloriesForDay(start, end)
    }

    suspend fun wipe() = dao.wipe()

    private fun dayBounds(day: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}
