package com.aerosun.heliumleakdetector.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 检测设备台账 Entity。
 *
 * 记录试验所用的检漏仪、标准漏孔、温度计、氦浓度计等设备信息及校准有效期。
 */
@Entity(
    tableName = "equipment",
    indices = [
        Index("status"),
        Index("calibration_due_date"),
    ]
)
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "type") val type: String = "",
    @ColumnInfo(name = "model") val model: String = "",
    @ColumnInfo(name = "serial_no") val serialNo: String = "",

    /** 校准有效期截止时间 (epoch millis) */
    @ColumnInfo(name = "calibration_due_date") val calibrationDueDate: Long = 0,

    /** active | expired | retired */
    @ColumnInfo(name = "status") val status: String = "active",

    val notes: String = "",

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
