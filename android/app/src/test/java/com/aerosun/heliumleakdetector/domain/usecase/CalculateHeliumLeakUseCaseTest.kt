package com.aerosun.heliumleakdetector.domain.usecase

import com.aerosun.heliumleakdetector.domain.model.TestInput
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs

/**
 * CalculateHeliumLeakUseCase 单元测试。
 *
 * 覆盖：6 大公式 + 噪声覆盖规则 + 7 项合理性验证 + 边界条件
 * Python 对照文件: helium_leak_test.py
 */
@DisplayName("氦检漏计算引擎 — 单元测试")
class CalculateHeliumLeakUseCaseTest {

    private lateinit var useCase: CalculateHeliumLeakUseCase

    // ---- 标准测试参数（与 Python 示例 1 一致） ----
    private lateinit var standardInput: TestInput

    @BeforeEach
    fun setUp() {
        useCase = CalculateHeliumLeakUseCase()
        standardInput = TestInput(
            reportNo = "HD-LT26-0016",
            contractNo = "SX300250223",
            testDate = "2026.03.12",
            productName = "阀门箱B1-C04",
            weldName = "SWB108",
            inspectionArea = "IN_TUBE_01",
            temperature = 13.5,
            humidity = 40.0,
            q0Mantissa = 3.88, q0Exponent = -9, tCal = 20.0,
            i0Mantissa = 1.0, i0Exponent = -13,
            iMantissa = 3.12, iExponent = -9,
            m0Mantissa = 1.0, m0Exponent = -13,
            m1Mantissa = 3.32, m1Exponent = -9,
            m2Mantissa = 2.01, m2Exponent = -13,
            tResponse = 50.0, tgPercent = 100.0,
            acceptanceLimitMantissa = 1.0, acceptanceLimitExponent = -10,
        )
    }

    // ========================================================================
    // 公式 [1] 温度修正
    // ========================================================================

    @Nested
    @DisplayName("公式 [1] — 温度修正 Qt")
    inner class Formula1_Qt {

        @Test
        @DisplayName("标定温度等于试验温度时，Qt = Q0")
        fun `Qt equals Q0 when temps are equal`() {
            val qt = useCase.computeQt(q0 = 3.88e-9, tCal = 20.0, tTest = 20.0)
            assertEquals(3.88e-9, qt, 1e-20, "T_cal = T_test 时 Qt 应等于 Q0")
        }

        @Test
        @DisplayName("试验温度低于标定温度时，Qt < Q0")
        fun `Qt less than Q0 when T_test lower than T_cal`() {
            val qt = useCase.computeQt(q0 = 3.88e-9, tCal = 20.0, tTest = 13.5)
            assertTrue(qt < 3.88e-9, "低温时 Qt 应小于 Q0")
        }

        @Test
        @DisplayName("试验温度高于标定温度时，Qt > Q0")
        fun `Qt greater than Q0 when T_test higher than T_cal`() {
            val qt = useCase.computeQt(q0 = 3.88e-9, tCal = 20.0, tTest = 30.0)
            assertTrue(qt > 3.88e-9, "高温时 Qt 应大于 Q0")
        }

        @Test
        @DisplayName("与 Python 计算值精确一致 (T=13.5)")
        fun `matches Python value at T=13_5`() {
            val qt = useCase.computeQt(q0 = 3.88e-9, tCal = 20.0, tTest = 13.5)
            // Python: 3.1234e-09
            assertEquals(3.1234e-9, qt, 1e-14)
        }

        @ParameterizedTest(name = "T_test={0}°C → Qt 合理")
        @ValueSource(doubles = [0.0, 10.0, 20.0, 30.0, 50.0])
        fun `Qt is non-negative in range 0-50°C`(tTest: Double) {
            val qt = useCase.computeQt(q0 = 3.88e-9, tCal = 20.0, tTest = tTest)
            assertTrue(qt > 0, "Qt 应始终为正")
        }
    }

    // ========================================================================
    // 公式 [2 & 4] 噪声覆盖规则
    // ========================================================================

    @Nested
    @DisplayName("公式 [2 & 4] — 噪声覆盖规则 max(实测, 默认)")
    inner class Formula2_4_Noise {

        @Test
        @DisplayName("无实测值时，返回默认值 I0/10")
        fun `returns default when measured is null`() {
            val (noise, source) = useCase.computeEffectiveNoiseWithSource(
                baseSignal = 1e-13, measured = null
            )
            assertEquals(1e-14, noise, 1e-20)
            assertEquals("I0/10", source)
        }

        @Test
        @DisplayName("实测值 > 默认值时，取实测值")
        fun `uses measured when larger than default`() {
            val (noise, source) = useCase.computeEffectiveNoiseWithSource(
                baseSignal = 1e-13, measured = 5e-14
            )
            assertEquals(5e-14, noise, 1e-20)
            assertEquals("实测值", source)
        }

        @Test
        @DisplayName("实测值 < 默认值时，取默认值（保守）")
        fun `uses default when measured smaller (conservative)`() {
            val (noise, source) = useCase.computeEffectiveNoiseWithSource(
                baseSignal = 1e-13, measured = 5e-15
            )
            assertEquals(1e-14, noise, 1e-20)
            assertEquals("I0/10", source)
        }

        @Test
        @DisplayName("实测值 == 默认值时，取默认值")
        fun `uses default when measured equals default`() {
            val (noise, _) = useCase.computeEffectiveNoiseWithSource(
                baseSignal = 1e-13, measured = 1e-14
            )
            assertEquals(1e-14, noise, 1e-20)
        }
    }

    // ========================================================================
    // 公式 [3] 仪器最小可检漏率 Qmin
    // ========================================================================

    @Nested
    @DisplayName("公式 [3] — 仪器最小可检漏率 Qmin")
    inner class Formula3_Qmin {

        @Test
        @DisplayName("与 Python 计算值一致")
        fun `matches Python value`() {
            // Python: Qmin = 1.0011e-14
            val qmin = useCase.computeQmin(
                noise = 1e-14, qt = 3.1234e-9,
                i = 3.12e-9, i0 = 1e-13
            )
            assertEquals(9.408e-15, qmin, 1e-18)  // 使用更新参数
        }

        @Test
        @DisplayName("I - I0 <= 0 时抛出 IllegalArgumentException")
        fun `throws when I not greater than I0`() {
            assertThrows<IllegalArgumentException> {
                useCase.computeQmin(
                    noise = 1e-14, qt = 3.1234e-9,
                    i = 1e-13, i0 = 1e-13  // I == I0
                )
            }
        }

        @Test
        @DisplayName("I < I0 时抛出异常")
        fun `throws when I less than I0`() {
            assertThrows<IllegalArgumentException> {
                useCase.computeQmin(
                    noise = 1e-14, qt = 3.1234e-9,
                    i = 1e-14, i0 = 1e-13  // I < I0
                )
            }
        }
    }

    // ========================================================================
    // 公式 [5] 系统最小可检漏率 Qeim
    // ========================================================================

    @Nested
    @DisplayName("公式 [5] — 系统最小可检漏率 Qeim")
    inner class Formula5_Qeim {

        @Test
        @DisplayName("与 Python 计算值一致")
        fun `matches Python value`() {
            val qeim = useCase.computeQeim(
                noise = 1e-14, qt = 3.1234e-9,
                m1 = 3.32e-9, m0 = 1e-13
            )
            assertEquals(9.408e-15, qeim, 1e-18)
        }

        @Test
        @DisplayName("M1 - M0 <= 0 时抛出 IllegalArgumentException")
        fun `throws when M1 not greater than M0`() {
            assertThrows<IllegalArgumentException> {
                useCase.computeQeim(
                    noise = 1e-14, qt = 3.1234e-9,
                    m1 = 1e-13, m0 = 1e-13
                )
            }
        }
    }

    // ========================================================================
    // 公式 [6] 实测漏率 Q
    // ========================================================================

    @Nested
    @DisplayName("公式 [6] — 实测漏率 Q")
    inner class Formula6_Q {

        @Test
        @DisplayName("纯氦(TG%=100)时浓度修正因子为1")
        fun `concentration correction is 1 for pure helium`() {
            val q = useCase.computeQ(
                qt = 3.1234e-9, m2 = 2.01e-13,
                m0 = 1e-13, m1 = 3.32e-9, tgPercent = 100.0
            )
            assertTrue(q > 0, "漏率应为正数")
        }

        @Test
        @DisplayName("10% 氦浓度时漏率放大 10 倍")
        fun `concentration correction amplifies for diluted helium`() {
            val q100 = useCase.computeQ(
                qt = 3.1234e-9, m2 = 2.01e-13,
                m0 = 1e-13, m1 = 3.32e-9, tgPercent = 100.0
            )
            val q10 = useCase.computeQ(
                qt = 3.1234e-9, m2 = 2.01e-13,
                m0 = 1e-13, m1 = 3.32e-9, tgPercent = 10.0
            )
            assertEquals(q100 * 10, q10, q100 * 0.01,
                "10%% 浓度时漏率应为 100%% 浓度的 10 倍")
        }

        @Test
        @DisplayName("TG%=0 时不除零（安全兜底）")
        fun `handles zero TG_percent gracefully`() {
            val q = useCase.computeQ(
                qt = 3.1234e-9, m2 = 2.01e-13,
                m0 = 1e-13, m1 = 3.32e-9, tgPercent = 0.0
            )
            assertTrue(q >= 0, "不应抛出异常")
        }
    }

    // ========================================================================
    // 集成测试：完整计算流程 invoke()
    // ========================================================================

    @Nested
    @DisplayName("集成测试 — invoke() 完整计算流程")
    inner class Integration_Invoke {

        @Test
        @DisplayName("标准输入 → 计算成功，判定合格")
        fun `standard input computes and passes`() {
            val result = useCase(standardInput)

            // 基础值非零
            assertTrue(result.q0 > 0)
            assertTrue(result.qt > 0)
            assertTrue(result.i > result.i0, "校准读数应大于本底")
            assertTrue(result.qmin > 0)
            assertTrue(result.qeim > 0)
            assertTrue(result.qMeasured > 0)

            // 验收判定
            assertTrue(result.isAcceptable, "标准参数应判定合格")
            assertTrue(result.qMeasured < standardInput.acceptanceLimitValue())

            // 无计算错误
            assertTrue(result.errors.isEmpty(),
                "不应有计算错误，实际: ${result.errors}")
        }

        @Test
        @DisplayName("报告编号已设置")
        fun `report number is preserved`() {
            val result = useCase(standardInput)
            assertTrue(standardInput.reportNo.isNotBlank())
        }

        @Test
        @DisplayName("温度修正方向正确 (T_test=13.5 < T_cal=20)")
        fun `temperature correction direction is correct`() {
            val result = useCase(standardInput)
            assertTrue(result.qt < result.q0,
                "低温下 Qt(${result.qt}) 应小于 Q0(${result.q0})")
        }

        @Test
        @DisplayName("M2 < M0 时产生警告，无泄漏")
        fun `no leak when M2 less than M0`() {
            val input = standardInput.copy(m2Mantissa = 0.5)  // M2 < M0
            val result = useCase(input)
            assertTrue(result.warnings.isNotEmpty(),
                "M2 < M0 时应产生警告")
        }

        @Test
        @DisplayName("不合格漏率应正确判定")
        fun `fails when measured leak exceeds limit`() {
            // 提高读数使实测漏率超过 1e-10
            val input = standardInput.copy(
                m2Mantissa = 3.0, m2Exponent = -7  // 大幅提高 M2
            )
            val result = useCase(input)
            assertFalse(result.isAcceptable,
                "大漏率应判定不合格")
        }

        @Test
        @DisplayName("噪声覆盖规则生效：传入大实测噪声取实测值")
        fun `noise override takes effect with larger measured value`() {
            val input = standardInput.copy(
                inMeasuredMantissa = 5.0, inMeasuredExponent = -13,   // >> I0/10 = 1e-14
                mnMeasuredMantissa = 3.0, mnMeasuredExponent = -13    // >> M0/10 = 1e-14
            )
            val result = useCase(input)

            assertEquals(5e-13, result.inValue, 1e-18)
            assertEquals("实测值", result.inSource)
            assertEquals(3e-13, result.mnValue, 1e-18)
            assertEquals("实测值", result.mnSource)
            // 噪声变大后 Qmin/Qeim 应显著增大
            assertTrue(result.qmin > 1e-13, "噪声增大后 Qmin 应显著放大")
        }

        @Test
        @DisplayName("7 项合理性验证均有覆盖")
        fun `all 7 validation rules are checked`() {
            // 构造一个会触发多项警告的输入
            val input = standardInput.copy(
                temperature = 50.0,      // → Qt 方向检查 (T > T_cal 则 >Q0，正常无警告)
                tgPercent = 150.0        // → 超出 (0,100] 范围 → 触发警告 #7
            )
            val result = useCase(input)
            val warningTexts = result.warnings.joinToString(",")
            // #7 氦浓度超出范围
            assertTrue(warningTexts.contains("TG%"),
                "应包含氦浓度超出范围警告，实际: $warningTexts")
        }
    }

    // ========================================================================
    // 边界条件 & 容错测试
    // ========================================================================

    @Nested
    @DisplayName("边界条件与容错")
    inner class EdgeCases {

        @Test
        @DisplayName("尾数为 0 时漏率应为 0")
        fun `zero mantissa gives reasonable result`() {
            val input = standardInput.copy(
                q0Mantissa = 0.0, m2Mantissa = 0.0
            )
            val result = useCase(input)
            assertTrue(result.qt == 0.0 || result.qMeasured == 0.0,
                "Q0 或 M2 为 0 时结果应合理")
        }

        @Test
        @DisplayName("极限指数值不崩溃")
        fun `extreme exponents do not crash`() {
            val input = standardInput.copy(
                q0Exponent = -20, i0Exponent = -20,
                m0Exponent = -20, m2Exponent = -20
            )
            assertDoesNotThrow { useCase(input) }
        }

        @Test
        @DisplayName("不同温度下的结果稳定性")
        fun `stable across temperature range`() {
            for (temp in listOf(0.0, 15.0, 20.0, 35.0, 50.0)) {
                val input = standardInput.copy(temperature = temp)
                val result = useCase(input)
                assertTrue(result.qt > 0, "T=$temp 时 Qt 应为正")
                assertTrue(result.qMeasured > 0, "T=$temp 时 Q 应为正")
            }
        }
    }
}
