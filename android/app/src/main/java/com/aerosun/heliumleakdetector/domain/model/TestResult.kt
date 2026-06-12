package com.aerosun.heliumleakdetector.domain.model

/**
 * 氦检漏试验计算结果。
 *
 * 对应 Python `TestResult` dataclass，包含所有中间计算量和最终判定。
 *
 * @property q0           标准漏孔标定漏率 (Pa·m³/s)
 * @property qt           试验温度下的标准漏率 (Pa·m³/s)
 * @property i0           仪器本底 (Pa·m³/s)
 * @property inValue      仪器本底噪声 In (Pa·m³/s) — 已应用 max(实测, I0/10)
 * @property inSource     噪声来源标识: "I0/10" 或 "实测值"
 * @property i            仪器校准读数 (Pa·m³/s)
 * @property qmin         仪器最小可检漏率 (Pa·m³/s)
 * @property m0           系统本底 (Pa·m³/s)
 * @property mnValue      系统本底噪声 Mn (Pa·m³/s) — 已应用 max(实测, M0/10)
 * @property mnSource     噪声来源标识
 * @property m1           系统校准读数 (Pa·m³/s)
 * @property qeim         系统最小可检漏率 (Pa·m³/s)
 * @property m2           喷氦后读数 (Pa·m³/s)
 * @property qMeasured    实测漏率 (Pa·m³/s)
 * @property isAcceptable 验收判定 (true=合格)
 * @property warnings     合理性验证警告列表
 * @property errors       计算错误列表
 */
data class TestResult(
    val q0: Double = 0.0,
    val qt: Double = 0.0,
    val i0: Double = 0.0,
    val inValue: Double = 0.0,
    val inSource: String = "I0/10",
    val i: Double = 0.0,
    val qmin: Double = 0.0,
    val m0: Double = 0.0,
    val mnValue: Double = 0.0,
    val mnSource: String = "M0/10",
    val m1: Double = 0.0,
    val qeim: Double = 0.0,
    val m2: Double = 0.0,
    val qMeasured: Double = 0.0,
    val isAcceptable: Boolean = false,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    /** 安全裕度（验收标准 / 实测漏率），>1 表示合格 */
    fun safetyMargin(acceptanceLimit: Double): Double =
        if (qMeasured > 0) acceptanceLimit / qMeasured else Double.POSITIVE_INFINITY

    /** 安全裕度 (dB) */
    fun safetyMarginDb(acceptanceLimit: Double): Double =
        if (qMeasured > 0) 10.0 * kotlin.math.log10(safetyMargin(acceptanceLimit)) else Double.POSITIVE_INFINITY
}
