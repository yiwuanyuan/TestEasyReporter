package com.aerosun.heliumleakdetector.domain.model

import kotlin.math.pow
import java.text.SimpleDateFormat
import java.util.*

private val today: String get() = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

/**
 * 氦检漏试验输入参数。
 *
 * 科学计数法：所有物理量由"尾数"+"指数"组成 → 实际值 = mantissa * 10^exponent
 *
 * V1.1 改进：
 *   - In/Mn/验收限值 统一使用尾数+指数输入，与 I0/M0 等一致
 *   - 试验日期默认取系统当天
 */
data class TestInput(
    // ---- 项目信息 ----
    val reportNo: String = "",
    val contractNo: String = "",
    val testDate: String = today,          // 默认当天
    val productCode: String = "",
    val productName: String = "",
    val weldName: String = "",
    val inspectionArea: String = "",
    val testMethod: String = "Helium Spray Test",
    val testProcedureNo: String = "",

    // ---- 环境条件 ----
    val temperature: Double = 20.0,
    val humidity: Double = 40.0,

    // ---- 标准漏孔 (人工输入) ----
    val q0Mantissa: Double = 0.0,
    val q0Exponent: Int = -9,
    val tCal: Double = 20.0,

    // ---- 检漏仪 (人工输入) ----
    val i0Mantissa: Double = 0.0,
    val i0Exponent: Int = -13,
    val iMantissa: Double = 0.0,
    val iExponent: Int = -9,

    // ---- 系统 (人工输入) ----
    val m0Mantissa: Double = 0.0,
    val m0Exponent: Int = -13,
    val m1Mantissa: Double = 0.0,
    val m1Exponent: Int = -9,
    val m2Mantissa: Double = 0.0,
    val m2Exponent: Int = -13,
    val tResponse: Double = 50.0,
    val tgPercent: Double = 100.0,

    // ---- 噪声实测值 (可覆盖, 深绿色底) — 尾数+指数 ----
    val inMeasuredMantissa: Double = 0.0,      // 0 = 使用默认 I0/10
    val inMeasuredExponent: Int = -14,
    val mnMeasuredMantissa: Double = 0.0,      // 0 = 使用默认 M0/10
    val mnMeasuredExponent: Int = -14,

    // ---- 验收标准 & 温度系数 — 尾数+指数 ----
    val acceptanceLimitMantissa: Double = 1.0,
    val acceptanceLimitExponent: Int = -10,
    val tempCoefficient: Double = 0.03,
) {
    fun q0() = q0Mantissa * 10.0.pow(q0Exponent)
    fun i0() = i0Mantissa * 10.0.pow(i0Exponent)
    fun i()  = iMantissa  * 10.0.pow(iExponent)
    fun m0() = m0Mantissa * 10.0.pow(m0Exponent)
    fun m1() = m1Mantissa * 10.0.pow(m1Exponent)
    fun m2() = m2Mantissa * 10.0.pow(m2Exponent)

    /** 仪器本底噪声，mantissa==0 → null (使用默认 I0/10) */
    fun inMeasuredValue(): Double? =
        if (inMeasuredMantissa > 0) inMeasuredMantissa * 10.0.pow(inMeasuredExponent) else null

    /** 系统本底噪声，mantissa==0 → null (使用默认 M0/10) */
    fun mnMeasuredValue(): Double? =
        if (mnMeasuredMantissa > 0) mnMeasuredMantissa * 10.0.pow(mnMeasuredExponent) else null

    /** 验收限值 */
    fun acceptanceLimitValue(): Double =
        acceptanceLimitMantissa * 10.0.pow(acceptanceLimitExponent)
}
