package com.aerosun.heliumleakdetector.domain.repository

import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow

interface EquipmentRepository {
    suspend fun insert(equipment: EquipmentEntity): Long
    suspend fun update(equipment: EquipmentEntity)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): EquipmentEntity?
    fun getAllFlow(): Flow<List<EquipmentEntity>>
    fun getByStatusFlow(status: String): Flow<List<EquipmentEntity>>
    suspend fun getExpiringSoon(days: Int = 7): List<EquipmentEntity>
}
