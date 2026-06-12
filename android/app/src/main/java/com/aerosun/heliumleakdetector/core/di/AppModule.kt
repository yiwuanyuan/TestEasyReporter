package com.aerosun.heliumleakdetector.core.di

import android.content.Context
import androidx.room.Room
import com.aerosun.heliumleakdetector.data.local.HeliumDatabase
import com.aerosun.heliumleakdetector.data.local.dao.EquipmentDao
import com.aerosun.heliumleakdetector.data.local.dao.RecordDao
import com.aerosun.heliumleakdetector.data.repository.EquipmentRepositoryImpl
import com.aerosun.heliumleakdetector.data.repository.RecordRepositoryImpl
import com.aerosun.heliumleakdetector.domain.repository.EquipmentRepository
import com.aerosun.heliumleakdetector.domain.repository.RecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块。
 *
 * 提供 Database、DAO、Repository 的单例实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HeliumDatabase =
        Room.databaseBuilder(
            context,
            HeliumDatabase::class.java,
            "helium_database",
        )
            .addMigrations(HeliumDatabase.MIGRATION_1_2, HeliumDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideRecordDao(db: HeliumDatabase): RecordDao = db.recordDao()

    @Provides
    @Singleton
    fun provideRecordRepository(repo: RecordRepositoryImpl): RecordRepository = repo

    @Provides
    fun provideEquipmentDao(db: HeliumDatabase): EquipmentDao = db.equipmentDao()

    @Provides
    @Singleton
    fun provideEquipmentRepository(repo: EquipmentRepositoryImpl): EquipmentRepository = repo
}
