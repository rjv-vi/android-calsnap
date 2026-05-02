package app.calsnap.android.data.repository

import app.calsnap.android.data.database.dao.WeightDao
import app.calsnap.android.data.database.entity.WeightEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightDao,
) {
    fun observeAll(): Flow<List<WeightEntity>> = dao.observeAll()

    suspend fun add(weightKg: Float): Long = dao.insert(
        WeightEntity(
            loggedAt = System.currentTimeMillis(),
            weightKg = weightKg,
        ),
    )

    suspend fun wipe() = dao.wipe()
}
