package app.calsnap.android.data.repository

import app.calsnap.android.data.database.dao.WaterDao
import app.calsnap.android.data.database.entity.WaterEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val dao: WaterDao,
) {
    fun observeToday(): Flow<List<WaterEntity>> = observeDay(LocalDate.now())

    fun observeDay(day: LocalDate): Flow<List<WaterEntity>> {
        val (start, end) = dayBounds(day)
        return dao.observeDay(start, end)
    }

    suspend fun add(milliliters: Int): Long = dao.insert(
        WaterEntity(
            loggedAt = System.currentTimeMillis(),
            milliliters = milliliters.coerceAtLeast(0),
        ),
    )

    suspend fun totalForToday(): Int {
        val (start, end) = dayBounds(LocalDate.now())
        return dao.totalForDay(start, end)
    }

    suspend fun delete(entry: WaterEntity) = dao.delete(entry)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun wipe() = dao.wipe()

    private fun dayBounds(day: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}
