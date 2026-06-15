package com.aerosun.heliumleakdetector.domain.repository

import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 检测记录 Repository 接口（Domain 层定义，Data 层实现）。
 */
interface RecordRepository {
    suspend fun insert(record: DetectionRecordEntity): Long
    suspend fun update(record: DetectionRecordEntity)
    suspend fun softDelete(id: Long)
    suspend fun getById(id: Long): DetectionRecordEntity?
    fun getAllFlow(): Flow<List<DetectionRecordEntity>>
    fun searchFlow(query: String): Flow<List<DetectionRecordEntity>>
    fun getByAcceptableFlow(acceptable: Boolean): Flow<List<DetectionRecordEntity>>
    fun getFavoritesFlow(): Flow<List<DetectionRecordEntity>>
    suspend fun count(): Int
    fun getByDateFlow(date: String): Flow<List<DetectionRecordEntity>>
    fun getAllDatesFlow(): Flow<List<String>>
}
