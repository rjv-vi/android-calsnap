package app.calsnap.android.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_log")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loggedAt: Long,
    val weightKg: Float,
)
