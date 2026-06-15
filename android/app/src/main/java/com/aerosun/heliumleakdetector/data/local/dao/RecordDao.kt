package com.aerosun.heliumleakdetector.data.local.dao

import androidx.room.*
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 检测记录 DAO — Room 数据访问对象。
 *
 * 查询规则：
 *   - 所有列表查询自动过滤 is_deleted = 1 的记录（软删除隔离）
 *   - 返回 Flow 用于响应式 UI 更新
 */
@Dao
interface RecordDao {

    // ====== 基础 CRUD ======

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DetectionRecordEntity): Long

    @Update
    suspend fun update(record: DetectionRecordEntity)

    @Query("UPDATE detection_records SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM detection_records WHERE id = :id")
    suspend fun hardDelete(id: Long)

    // ====== 单条查询 ======

    @Query("SELECT * FROM detection_records WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: Long): DetectionRecordEntity?

    @Query("SELECT * FROM detection_records WHERE report_no = :reportNo AND is_deleted = 0 LIMIT 1")
    suspend fun getByReportNo(reportNo: String): DetectionRecordEntity?

    // ====== 列表查询 (Flow — 响应式) ======

    @Query("SELECT * FROM detection_records WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<DetectionRecordEntity>>

    @Query("""
        SELECT * FROM detection_records
        WHERE is_deleted = 0 AND is_acceptable = :acceptable
        ORDER BY created_at DESC
    """)
    fun getByAcceptableFlow(acceptable: Boolean): Flow<List<DetectionRecordEntity>>

    @Query("SELECT * FROM detection_records WHERE is_deleted = 0 AND is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoritesFlow(): Flow<List<DetectionRecordEntity>>

    // ====== 搜索 ======

    @Query("""
        SELECT * FROM detection_records
        WHERE is_deleted = 0
          AND (report_no LIKE '%' || :query || '%'
            OR contract_no LIKE '%' || :query || '%'
            OR product_name LIKE '%' || :query || '%'
            OR product_code LIKE '%' || :query || '%'
            OR product_serial_no LIKE '%' || :query || '%'
            OR weld_name LIKE '%' || :query || '%'
            OR inspection_area LIKE '%' || :query || '%')
        ORDER BY created_at DESC
    """)
    fun searchFlow(query: String): Flow<List<DetectionRecordEntity>>

    // ====== 按试验日期筛选 ======

    @Query("""
        SELECT * FROM detection_records
        WHERE is_deleted = 0 AND test_date = :date
        ORDER BY created_at DESC
    """)
    fun getByDateFlow(date: String): Flow<List<DetectionRecordEntity>>

    /** 获取所有有记录的日期（用于日历展示） */
    @Query("SELECT DISTINCT test_date FROM detection_records WHERE is_deleted = 0 ORDER BY test_date DESC")
    fun getAllDatesFlow(): Flow<List<String>>

    // ====== 计数 ======

    @Query("SELECT COUNT(*) FROM detection_records WHERE is_deleted = 0")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM detection_records WHERE is_deleted = 0 AND is_acceptable = 1")
    suspend fun countPass(): Int

    @Query("SELECT COUNT(*) FROM detection_records WHERE is_deleted = 0 AND is_acceptable = 0")
    suspend fun countFail(): Int
}
