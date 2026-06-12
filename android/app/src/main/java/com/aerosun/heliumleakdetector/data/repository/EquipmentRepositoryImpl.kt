package com.aerosun.heliumleakdetector.data.repository

import com.aerosun.heliumleakdetector.data.local.dao.EquipmentDao
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.aerosun.heliumleakdetector.domain.repository.EquipmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentRepositoryImpl @Inject constructor(
    private val equipmentDao: EquipmentDao,
) : EquipmentRepository {

    override suspend fun insert(equipment: EquipmentEntity): Long = equipmentDao.insert(equipment)
    override suspend fun update(equipment: EquipmentEntity) = equipmentDao.update(equipment)
    override suspend fun delete(id: Long) = equipmentDao.delete(id)
    override suspend fun getById(id: Long): EquipmentEntity? = equipmentDao.getById(id)
    override fun getAllFlow(): Flow<List<EquipmentEntity>> = equipmentDao.getAllFlow()
    override fun getByStatusFlow(status: String): Flow<List<EquipmentEntity>> = equipmentDao.getByStatusFlow(status)

    override suspend fun getExpiringSoon(days: Int): List<EquipmentEntity> {
        val threshold = System.currentTimeMillis() + days.toLong() * 24 * 3600 * 1000
        return equipmentDao.getExpiringSoon(threshold)
    }
}
