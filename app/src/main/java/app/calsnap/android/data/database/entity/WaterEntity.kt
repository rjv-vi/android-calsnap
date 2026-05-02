package app.calsnap.android.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_log")
data class WaterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loggedAt: Long,
    val milliliters: Int,
)
