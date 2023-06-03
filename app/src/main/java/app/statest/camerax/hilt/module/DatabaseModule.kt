package app.statest.camerax.hilt.module

import app.statest.camerax.SQLHelper
import app.statest.camerax.database.CameraDataSource
import app.statest.camerax.database.CameraRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideCameraDataSource(sqlHelper: SQLHelper):CameraRepository = CameraDataSource(sqlHelper)
}