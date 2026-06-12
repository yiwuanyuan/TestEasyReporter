package com.aerosun.heliumleakdetector.data.local.dao

import androidx.room.*
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipment: EquipmentEntity): Long

    @Update
    suspend fun update(equipment: EquipmentEntity)

    @Query("DELETE FROM equipment WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getById(id: Long): EquipmentEntity?

    /** 全部设备，按校准到期日升序（即将到期的在前） */
    @Query("SELECT * FROM equipment ORDER BY calibration_due_date ASC")
    fun getAllFlow(): Flow<List<EquipmentEntity>>

    /** 按状态筛选 */
    @Query("SELECT * FROM equipment WHERE status = :status ORDER BY calibration_due_date ASC")
    fun getByStatusFlow(status: String): Flow<List<EquipmentEntity>>

    /** 即将到期（7天内）的设备 */
    @Query("""
        SELECT * FROM equipment
        WHERE status = 'active'
          AND calibration_due_date > 0
          AND calibration_due_date <= :threshold
        ORDER BY calibration_due_date ASC
    """)
    suspend fun getExpiringSoon(threshold: Long): List<EquipmentEntity>
}
