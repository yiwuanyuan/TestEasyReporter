package com.aerosun.heliumleakdetector.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.pow

/**
 * 检测记录 Room Entity。
 *
 * 对应策划方案数据库 DDL 中的 detection_records 表。
 *
 * 字段分为 3 组：
 *   1. 人工输入参数（绿色底）— 用户在表单中填写
 *   2. 计算结果（白色底）  — 由 CalculateHeliumLeakUseCase 生成并冗余存储
 *   3. 元数据            — 创建时间、收藏、软删除标记等
 *
 * 索引覆盖：created_at(时间排序) | report_no(编号搜索) | is_deleted(软删除隔离)
 */
@Entity(
    tableName = "detection_records",
    indices = [
        Index("created_at"),
        Index("report_no"),
        Index("is_deleted"),
        Index("is_favorite"),
    ],
)
data class DetectionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ====== 项目信息 ======
    @ColumnInfo(name = "report_no")        val reportNo: String = "",
    @ColumnInfo(name = "contract_no")      val contractNo: String = "",
    @ColumnInfo(name = "test_date")        val testDate: String = "",
    @ColumnInfo(name = "product_code")     val productCode: String = "",
    @ColumnInfo(name = "product_name")     val productName: String = "",
    @ColumnInfo(name = "weld_name")        val weldName: String = "",
    @ColumnInfo(name = "inspection_area")  val inspectionArea: String = "",
    @ColumnInfo(name = "product_serial_no") val productSerialNo: String = "",   // 出厂编号
    @ColumnInfo(name = "test_method")      val testMethod: String = "Helium Spray Test",
    @ColumnInfo(name = "test_procedure_no") val testProcedureNo: String = "",

    // ====== 环境条件 ======
    @ColumnInfo(name = "temperature") val temperature: Double = 20.0,
    @ColumnInfo(name = "humidity")    val humidity: Double = 40.0,

    // ====== 标准漏孔 (人工输入, 绿色底) ======
    @ColumnInfo(name = "Q0_mantissa") val q0Mantissa: Double = 0.0,   // 不预填
    @ColumnInfo(name = "Q0_exponent") val q0Exponent: Int = -9,
    @ColumnInfo(name = "T_cal")       val tCal: Double = 20.0,

    // ====== 检漏仪 (人工输入, 绿色底) ======
    @ColumnInfo(name = "I0_mantissa") val i0Mantissa: Double = 0.0,
    @ColumnInfo(name = "I0_exponent") val i0Exponent: Int = -13,
    @ColumnInfo(name = "I_mantissa")  val iMantissa: Double = 0.0,
    @ColumnInfo(name = "I_exponent")  val iExponent: Int = -9,

    // ====== 系统 (人工输入, 绿色底) ======
    @ColumnInfo(name = "M0_mantissa") val m0Mantissa: Double = 0.0,
    @ColumnInfo(name = "M0_exponent") val m0Exponent: Int = -13,
    @ColumnInfo(name = "M1_mantissa") val m1Mantissa: Double = 0.0,
    @ColumnInfo(name = "M1_exponent") val m1Exponent: Int = -9,
    @ColumnInfo(name = "M2_mantissa") val m2Mantissa: Double = 0.0,
    @ColumnInfo(name = "M2_exponent") val m2Exponent: Int = -13,
    @ColumnInfo(name = "T_response")  val tResponse: Double = 50.0,
    @ColumnInfo(name = "TG_percent")  val tgPercent: Double = 100.0,

    // ====== 噪声实测值 (可覆盖, 深绿色底) — 尾数+指数 ======
    @ColumnInfo(name = "in_meas_mantissa") val inMeasuredMantissa: Double = 0.0,
    @ColumnInfo(name = "in_meas_exponent") val inMeasuredExponent: Int = -14,
    @ColumnInfo(name = "mn_meas_mantissa") val mnMeasuredMantissa: Double = 0.0,
    @ColumnInfo(name = "mn_meas_exponent") val mnMeasuredExponent: Int = -14,

    // ====== 验收标准 & 系数 — 尾数+指数 ======
    @ColumnInfo(name = "acc_limit_mantissa") val acceptanceLimitMantissa: Double = 1.0,
    @ColumnInfo(name = "acc_limit_exponent") val acceptanceLimitExponent: Int = -10,
    @ColumnInfo(name = "temp_coefficient") val tempCoefficient: Double = 0.03,

    // ====== 设备关联 (JSON 数组: [1,2,3]) ======
    @ColumnInfo(name = "equipment_ids") val equipmentIds: String = "",

    // ====== 计算结果 (冗余存储, 白色底) ======
    @ColumnInfo(name = "Q0_value")   val q0Value: Double = 0.0,
    @ColumnInfo(name = "Qt_value")   val qtValue: Double = 0.0,
    @ColumnInfo(name = "I0_value")   val i0Value: Double = 0.0,
    @ColumnInfo(name = "In_value")   val inValue: Double = 0.0,
    @ColumnInfo(name = "In_source")  val inSource: String = "I0/10",
    @ColumnInfo(name = "I_value")    val iValue: Double = 0.0,
    @ColumnInfo(name = "Qmin_value") val qminValue: Double = 0.0,
    @ColumnInfo(name = "M0_value")   val m0Value: Double = 0.0,
    @ColumnInfo(name = "Mn_value")   val mnValue: Double = 0.0,
    @ColumnInfo(name = "Mn_source")  val mnSource: String = "M0/10",
    @ColumnInfo(name = "M1_value")   val m1Value: Double = 0.0,
    @ColumnInfo(name = "Qeim_value") val qeimValue: Double = 0.0,
    @ColumnInfo(name = "M2_value")   val m2Value: Double = 0.0,
    @ColumnInfo(name = "Q_measured") val qMeasured: Double = 0.0,
    @ColumnInfo(name = "is_acceptable") val isAcceptable: Boolean = false,
    val warnings: List<String> = emptyList(),  // JSON 数组 (Room TypeConverter)

    // ====== 元数据 ======
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_deleted")  val isDeleted: Boolean = false,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
) {
    /** 验收限值 (Pa·m³/s) — computed from mantissa+exponent */
    fun acceptanceLimitValue(): Double =
        acceptanceLimitMantissa * 10.0.pow(acceptanceLimitExponent)
}
