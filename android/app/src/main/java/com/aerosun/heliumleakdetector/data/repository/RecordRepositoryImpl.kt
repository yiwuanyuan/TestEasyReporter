package com.aerosun.heliumleakdetector.data.repository

import com.aerosun.heliumleakdetector.data.local.dao.RecordDao
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.domain.repository.RecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
) : RecordRepository {

    override suspend fun insert(record: DetectionRecordEntity): Long =
        recordDao.insert(record)

    override suspend fun update(record: DetectionRecordEntity) =
        recordDao.update(record)

    override suspend fun softDelete(id: Long) =
        recordDao.softDelete(id)

    override suspend fun getById(id: Long): DetectionRecordEntity? =
        recordDao.getById(id)

    override fun getAllFlow(): Flow<List<DetectionRecordEntity>> =
        recordDao.getAllFlow()

    override fun searchFlow(query: String): Flow<List<DetectionRecordEntity>> =
        recordDao.searchFlow(query)

    override fun getByAcceptableFlow(acceptable: Boolean): Flow<List<DetectionRecordEntity>> =
        recordDao.getByAcceptableFlow(acceptable)

    override fun getFavoritesFlow(): Flow<List<DetectionRecordEntity>> =
        recordDao.getFavoritesFlow()

    override suspend fun count(): Int =
        recordDao.count()

    override fun getByDateFlow(date: String): Flow<List<DetectionRecordEntity>> =
        recordDao.getByDateFlow(date)

    override fun getAllDatesFlow(): Flow<List<String>> =
        recordDao.getAllDatesFlow()
}
