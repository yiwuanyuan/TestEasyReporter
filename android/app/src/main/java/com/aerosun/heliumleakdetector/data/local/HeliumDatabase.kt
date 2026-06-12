package com.aerosun.heliumleakdetector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aerosun.heliumleakdetector.data.local.dao.EquipmentDao
import com.aerosun.heliumleakdetector.data.local.dao.RecordDao
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity

/**
 * Room 数据库主类。
 *
 * 版本策略：
 *   - v1 (MVP): detection_records
 *   - v2 (V1.1): 新增 equipment 表
 *   - v3 (V2.0): 新增 user_accounts, cloud_sync_metadata
 */
@Database(
    entities = [DetectionRecordEntity::class, EquipmentEntity::class],
    version = 3,
    exportSchema = false,
)
@androidx.room.TypeConverters(Converters::class)
abstract class HeliumDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun equipmentDao(): EquipmentDao

    companion object {
        /** v1 → v2: 新增 equipment 表 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `equipment` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `type` TEXT NOT NULL DEFAULT '',
                        `model` TEXT NOT NULL DEFAULT '',
                        `serial_no` TEXT NOT NULL DEFAULT '',
                        `calibration_due_date` INTEGER NOT NULL DEFAULT 0,
                        `status` TEXT NOT NULL DEFAULT 'active',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_status` ON `equipment` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_calibration_due_date` ON `equipment` (`calibration_due_date`)")
            }
        }

        /** v2 → v3: In/Mn/验收限值 改为尾数+指数形式 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `detection_records` ADD COLUMN `in_meas_mantissa` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `detection_records` ADD COLUMN `in_meas_exponent` INTEGER NOT NULL DEFAULT -14")
                db.execSQL("ALTER TABLE `detection_records` ADD COLUMN `mn_meas_mantissa` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `detection_records` ADD COLUMN `mn_meas_exponent` INTEGER NOT NULL DEFAULT -14")
                db.execSQL("ALTER TABLE `detection_records` ADD COLUMN `acc_limit_mantissa` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `detection_records` ADD COLUMN `acc_limit_exponent` INTEGER NOT NULL DEFAULT -10")
            }
        }
    }
}
