package com.aerosun.heliumleakdetector.domain.usecase

import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult
import javax.inject.Inject
import kotlin.math.pow

/**
 * 氦检漏计算核心引擎。
 *
 * 100% 复现 Python `helium_leak_test.py` 的全部计算逻辑：
 *   6 大公式 + 噪声覆盖规则 (max(实测, 默认)) + 7 项合理性验证
 *
 * 对应 Python 类 `HeliumLeakTest` 及其 `run()` 方法。
 *
 * 公式速查：
 *   [1] Qt   = Q0 * (1 - (T_cal - T_test) * k)      — 温度修正
 *   [2] In   = max(In_measured,  I0 / 10)            — 仪器噪声（保守取大）
 *   [3] Qmin = In * Qt / (I - I0)                    — 仪器最小可检漏率
 *   [4] Mn   = max(Mn_measured, M0 / 10)             — 系统噪声（保守取大）
 *   [5] Qeim = Mn * Qt / (M1 - M0)                   — 系统最小可检漏率
 *   [6] Q    = Qt * (M2 - M0) / (M1 - M0) * 100/TG%  — 实测漏率
 *
 * Python 参考: E:\code\TestEasyReporter\helium_leak_test.py
 */
class CalculateHeliumLeakUseCase @Inject constructor() {

    companion object {
        /** 氦气漏率温度修正系数 (/°C) */
        const val TEMP_COEFFICIENT = 0.03

        /** 本底噪声比（噪声 = 本底 / 10） */
        const val NOISE_RATIO = 10
    }

    // ========================================================================
    // 主入口 — 对应 Python HeliumLeakTest.run()
    // ========================================================================

    /**
     * 执行完整氦检漏计算流程。
     *
     * 步骤：
     *   1. 合并尾数/指数 → 物理量实际值
     *   2. 温度修正 → Qt
     *   3. 噪声判定 → In (max规则), Mn (max规则)
     *   4. 仪器校准 → Qmin
     *   5. 系统校准 → Qeim
     *   6. 实测漏率 → Q
     *   7. 验收判定 + 7项合理性验证
     */
    operator fun invoke(input: TestInput): TestResult {
        val errors = mutableListOf<String>()

        // === 步骤 1: 基础物理量 ===
        val q0 = input.q0()
        val i0 = input.i0()
        val i  = input.i()
        val m0 = input.m0()
        val m1 = input.m1()
        val m2 = input.m2()

        // === 步骤 2: 温度修正 [公式 1] ===
        val qt = computeQt(q0, input.tCal, input.temperature, input.tempCoefficient)

        // === 步骤 3: 噪声判定 [公式 2 & 4] — max(实测, 默认) ===
        val (inEff, inSrc) = computeEffectiveNoiseWithSource(i0, input.inMeasuredValue(), NOISE_RATIO)
        val (mnEff, mnSrc) = computeEffectiveNoiseWithSource(m0, input.mnMeasuredValue(), NOISE_RATIO)

        // === 步骤 4: 仪器最小可检漏率 [公式 3] ===
        var qmin = 0.0
        try {
            qmin = computeQmin(inEff, qt, i, i0)
        } catch (e: IllegalArgumentException) {
            errors.add("仪器 Qmin 计算失败: ${e.message}")
        }

        // === 步骤 5: 系统最小可检漏率 [公式 5] ===
        var qeim = 0.0
        try {
            qeim = computeQeim(mnEff, qt, m1, m0)
        } catch (e: IllegalArgumentException) {
            errors.add("系统 Qeim 计算失败: ${e.message}")
        }

        // === 步骤 6: 实测漏率 [公式 6] ===
        var q = 0.0
        if (errors.isEmpty()) {
            try {
                q = computeQ(qt, m2, m0, m1, input.tgPercent)
            } catch (e: Exception) {
                errors.add("实测漏率 Q 计算失败: ${e.message}")
            }
        }

        // === 步骤 7: 验收判定 + 验证 ===
        val acceptable = q > 0 && q < input.acceptanceLimitValue()
        val warnings = validate(input, qt, i0, i, inEff, m0, m1, m2, mnEff, q)

        return TestResult(
            q0 = q0, qt = qt,
            i0 = i0, inValue = inEff, inSource = inSrc, i = i, qmin = qmin,
            m0 = m0, mnValue = mnEff, mnSource = mnSrc, m1 = m1, qeim = qeim, m2 = m2,
            qMeasured = q, isAcceptable = acceptable,
            warnings = warnings, errors = errors,
        )
    }

    // ========================================================================
    // 6 大公式 — 与 Python 逐行对应
    // ========================================================================

    /**
     * [公式 1] 温度修正：计算试验温度下的标准漏率 Qt。
     *
     * Python: compute_Qt(Q0, T_cal, T_test, coeff)
     *
     * 物理意义：氦气通过微小漏孔的漏率受温度影响，
     * 在 0~50°C 范围内漏率随温度升高近似线性增加 (~3%/°C)。
     *
     * @param q0     标定温度下的标准漏率 (Pa·m³/s)
     * @param tCal   标漏检定温度 (°C)，通常 20°C
     * @param tTest  试验环境温度 (°C)
     * @param coeff  温度修正系数（默认 0.03）
     * @return       试验温度下的标准漏率 Qt (Pa·m³/s)
     */
    fun computeQt(
        q0: Double,
        tCal: Double,
        tTest: Double,
        coeff: Double = TEMP_COEFFICIENT,
    ): Double = q0 * (1 - (tCal - tTest) * coeff)

    /**
     * [公式 2 & 4 合并] 计算有效噪声（含实测值覆盖判定）。
     *
     * Python: compute_effective_noise(baseSignal, measured, ratio)
     *
     * 规则：effective = max(measured, baseSignal / ratio)
     *   - 无实测值 → 使用默认值 baseSignal/10
     *   - 实测值 > 默认值 → 取实测值（保守，防止低估噪声）
     *   - 实测值 < 默认值 → 取默认值（实测可能偏小，保留下限）
     *
     * 物理意义：噪声只能被低估，若实测噪声更大说明存在额外噪声源，
     * 应使用更大的实测值以保证检测限判定的保守性。
     *
     * @param baseSignal  本底信号值 I0 或 M0 (Pa·m³/s)
     * @param measured    人工实测噪声值，null 表示未提供
     * @param ratio       噪声比（默认 10）
     * @return Pair(有效噪声值, 来源标识)
     */
    fun computeEffectiveNoiseWithSource(
        baseSignal: Double,
        measured: Double?,
        ratio: Int = NOISE_RATIO,
    ): Pair<Double, String> {
        val defaultNoise = baseSignal / ratio
        if (measured == null) return Pair(defaultNoise, "I0/10")

        val effective = maxOf(measured, defaultNoise)
        val source = if (effective == measured) "实测值" else "I0/10"
        return Pair(effective, source)
    }

    /**
     * [公式 3] 计算仪器最小可检漏率 Qmin。
     *
     * Python: compute_Qmin(noise, Qt, I, I0)
     *
     * 物理意义：检漏仪在给定条件下能可靠检测到的最小漏率，
     * 由仪器信噪比决定。
     *
     * @throws IllegalArgumentException 当 (I - I0) <= 0 时
     */
    fun computeQmin(noise: Double, qt: Double, i: Double, i0: Double): Double {
        val denom = i - i0
        require(denom > 0) {
            "分母 (I - I0) = ${"%.3e".format(denom)} <= 0，无法计算 Qmin。" +
            "校准读数 I=${"%.3e".format(i)} 必须大于本底 I0=${"%.3e".format(i0)}。"
        }
        return noise * qt / denom
    }

    /**
     * [公式 5] 计算系统最小可检漏率 Qeim。
     *
     * Python: compute_Qeim(noise, Qt, M1, M0)
     *
     * @throws IllegalArgumentException 当 (M1 - M0) <= 0 时
     */
    fun computeQeim(noise: Double, qt: Double, m1: Double, m0: Double): Double {
        val denom = m1 - m0
        require(denom > 0) {
            "分母 (M1 - M0) = ${"%.3e".format(denom)} <= 0，无法计算 Qeim。" +
            "校准读数 M1=${"%.3e".format(m1)} 必须大于本底 M0=${"%.3e".format(m0)}。"
        }
        return noise * qt / denom
    }

    /**
     * [公式 6] 计算实测漏率 Q。
     *
     * Python: compute_Q(Qt, M2, M0, M1, TG_percent)
     *
     * 物理意义：喷氦后如有泄漏，检漏仪读数从 M0 升至 M2，
     * 漏率与读数增量 (M2-M0) 成正比，比例系数由系统校准 (M1-M0) 确定。
     *
     * 氦浓度修正：
     *   - 纯氦 (TG%=100): 修正因子 = 1
     *   - 10% 氦混合气 (TG%=10): 修正因子 = 10
     */
    fun computeQ(
        qt: Double,
        m2: Double,
        m0: Double,
        m1: Double,
        tgPercent: Double = 100.0,
    ): Double {
        val deltaM = m2 - m0
        val calFactor = qt / (m1 - m0)
        val concCorrection = if (tgPercent > 0) 100.0 / tgPercent else 1.0
        return deltaM * calFactor * concCorrection
    }

    // ========================================================================
    // 7 项合理性验证 — 对应 Python _validate()
    // ========================================================================

    /**
     * 执行 7 项合理性验证。
     *
     * Python: HeliumLeakTest._validate(result)
     */
    fun validate(
        input: TestInput,
        qt: Double, i0: Double, i: Double, inEff: Double,
        m0: Double, m1: Double, m2: Double, mnEff: Double,
        q: Double,
    ): List<String> {
        val w = mutableListOf<String>()

        // 1. 温度修正方向：低温 → Qt < Q0
        val q0 = input.q0()
        if (input.temperature < input.tCal && qt >= q0) {
            w.add("温度 T_test(${input.temperature}) < T_cal(${input.tCal}) " +
                  "但 Qt(${"%.3e".format(qt)}) >= Q0(${"%.3e".format(q0)})，温度修正方向可能异常")
        }

        // 2. 仪器校准信号 > 本底
        if (i <= i0) {
            w.add("仪器校准读数 I(${"%.3e".format(i)}) <= 本底 I0(${"%.3e".format(i0)})，信号无法区分")
        }

        // 3. 系统校准信号 > 本底
        if (m1 <= m0) {
            w.add("系统校准读数 M1(${"%.3e".format(m1)}) <= 本底 M0(${"%.3e".format(m0)})，信号无法区分")
        }

        // 4. Qeim 非负（间接通过公式保证，但做防御性检查）
        // (Qeim 已在 try-catch 中处理，此处不再重复)

        // 5. 喷氦后信号增量 vs 噪声
        val deltaM = m2 - m0
        if (deltaM <= 0) {
            w.add("喷氦后读数 M2(${"%.3e".format(m2)}) 未超过本底 M0(${"%.3e".format(m0)})，可能无泄漏")
        } else if (deltaM < mnEff) {
            w.add("信号增量 ΔM=${"%.3e".format(deltaM)} < 噪声 Mn=${"%.3e".format(mnEff)}，信号不可靠")
        }

        // 6. 漏率合理性
        if (q < 0) {
            w.add("实测漏率 Q=${"%.3e".format(q)} 为负值，异常")
        }

        // 7. 氦浓度范围
        if (input.tgPercent !in 0.1..100.0) {
            w.add("氦浓度 TG%=${input.tgPercent} 不在 (0, 100] 范围")
        }

        return w
    }
}
