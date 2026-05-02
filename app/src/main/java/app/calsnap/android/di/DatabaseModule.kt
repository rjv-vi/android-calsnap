package app.calsnap.android.di

import android.content.Context
import androidx.room.Room
import app.calsnap.android.data.database.CalSnapDatabase
import app.calsnap.android.data.database.CalSnapMigrations
import app.calsnap.android.data.database.dao.FoodLogDao
import app.calsnap.android.data.database.dao.WaterDao
import app.calsnap.android.data.database.dao.WeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): CalSnapDatabase =
        Room.databaseBuilder(ctx, CalSnapDatabase::class.java, CalSnapDatabase.NAME)
            .addMigrations(CalSnapMigrations.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFoodLogDao(db: CalSnapDatabase): FoodLogDao = db.foodLogDao()
    @Provides fun provideWeightDao(db: CalSnapDatabase): WeightDao   = db.weightDao()
    @Provides fun provideWaterDao(db: CalSnapDatabase): WaterDao     = db.waterDao()
}
