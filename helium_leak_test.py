#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
================================================================================
 氦检漏试验数据处理模块 (纯 Python 实现)
 Helium Leak Test Data Processing — Pure Python
================================================================================

 功能：
   1. 所有参数通过 Python 代码传入，不依赖任何 Excel 文件
   2. 实现氦检漏试验的 6 个核心计算公式
   3. 数据合理性自动验证
   4. 生成 Markdown 格式试验报告

 核心公式（从 Excel 原始公式中提取并验证）：

   【人工输入参数（Excel 绿色底）】
     Q0, T_cal  — 标准漏孔标定漏率与检定温度
     I0, I      — 仪器本底读数与校准读数
     M0, M1, M2 — 系统本底、校准读数、喷氦后读数
     T_response — 反应时间
     TG%        — 氦浓度百分比
     温度、湿度   — 环境条件

   【可覆盖公式参数（Excel 深绿色底，默认公式，允许手动覆盖）】
     In  — 仪器本底噪声：默认 In = I0/10，若提供实测值则取 max(实测值, I0/10)
     Mn  — 系统本底噪声：默认 Mn = M0/10，若提供实测值则取 max(实测值, M0/10)

   【自动计算参数（Excel 白色底）】
     [1] 温度修正：     Qt   = Q0 * (1 - (T_cal - T_test) * k)
     [3] 仪器最小可检漏率：Qmin = In * Qt / (I - I0)
     [5] 系统最小可检漏率：Qeim = Mn * Qt / (M1 - M0)
     [6] 实测漏率：     Q    = Qt * (M2 - M0) / (M1 - M0) * (100 / TG%)

 其中：
   k   = 0.03 /°C  — 氦气漏率温度修正系数
   TG% = 氦浓度百分比（100 为纯氦，<100 需按比例修正）

 噪声取值规则（In / Mn 共同逻辑）：
   noise_effective = max(measured_noise, base_signal / 10)
   即：若人工实测的噪声值大于公式计算值，取实测值；
       若实测值更小或未提供，取 I0/10 或 M0/10。
       此规则确保不会低估噪声，保证检测限判定的保守性。

 使用示例：
   >>> from helium_leak_test import HeliumLeakTest, TestInput
   >>> params = TestInput(
   ...     report_no="HD-LT26-0016",
   ...     Q0_mantissa=3.88, Q0_exponent=-9, T_cal=20.0, T_test=13.5,
   ...     I0_mantissa=1,   I0_exponent=-13,
   ...     I_mantissa=3.12, I_exponent=-9,
   ...     M0_mantissa=1,   M0_exponent=-13,
   ...     M1_mantissa=3.32, M1_exponent=-9,
   ...     M2_mantissa=2.01, M2_exponent=-13,
   ...     T_response=50, TG_percent=100,
   ... )
   >>> test = HeliumLeakTest(params)
   >>> test.run()
   >>> print(test.report())

 作者：AI 自动生成
 日期：2026-06-10
================================================================================
"""

import math
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional, List, Tuple


# ============================================================================
# 常量定义
# ============================================================================

# 氦气漏率温度修正系数 (/°C)
# 在 0~50°C 范围内，温度每变化 1°C，氦气漏率变化约 3%
TEMP_COEFFICIENT = 0.03

# 验收标准 (Pa*m3/s)
ACCEPTANCE_LIMIT_DEFAULT = 1.0e-10

# 本底噪声比（噪声 = 本底 / NOISE_RATIO）
NOISE_RATIO = 10


# ============================================================================
# 输入参数数据类
# ============================================================================

@dataclass
class TestInput:
    """
    氦检漏试验输入参数（一次完整试验的全部原始数据）。

    参数分为 4 组：项目信息、标准漏孔参数、检漏仪参数、系统参数。

    科学计数法表示：
        每个物理量由"尾数"和"指数"两部分给定，最终值 = mantissa * 10^exponent。
        例如：Q0_mantissa=3.88, Q0_exponent=-9 表示 Q0 = 3.88e-9 Pa*m3/s。

    Attributes:
        # ---- 项目信息 ----
        report_no:       报告编号
        contract_no:     合同号
        test_date:       试验日期
        product_code:    产品代码
        product_name:    产品名称
        weld_name:       焊缝名称
        inspection_area: 检测区域
        test_method:     试验方法（默认 Helium Spray Test）
        test_procedure_no: 试验规程编号

        # ---- 环境条件 ----
        temperature:     试验温度 (°C)
        humidity:        试验湿度 (%)

        # ---- 标准漏孔参数 ----
        Q0_mantissa:    标准漏孔标定漏率 尾数
        Q0_exponent:    标准漏孔标定漏率 指数
        T_cal:          标漏检定温度 (°C)

        # ---- 检漏仪（仪器）参数 ----
        I0_mantissa:    仪器本底读数 尾数
        I0_exponent:    仪器本底读数 指数
        I_mantissa:     仪器校准标漏开启后读数 尾数
        I_exponent:     仪器校准标漏开启后读数 指数

        # ---- 测试系统参数 ----
        M0_mantissa:    系统本底读数 尾数
        M0_exponent:    系统本底读数 指数
        M1_mantissa:    系统校准标漏开启后读数 尾数
        M1_exponent:    系统校准标漏开启后读数 指数
        M2_mantissa:    喷氦后读数 尾数
        M2_exponent:    喷氦后读数 指数
        T_response:     反应时间 (s)
        TG_percent:     氦浓度百分比 (%), 100 = 纯氦

        # ---- 可选噪声实测值（覆盖默认公式） ----
        In_measured:    仪器本底噪声实测值 (Pa*m3/s)，None=使用 I0/10
                        规则：effective_In = max(In_measured, I0/10)
        Mn_measured:    系统本底噪声实测值 (Pa*m3/s)，None=使用 M0/10
                        规则：effective_Mn = max(Mn_measured, M0/10)

        # ---- 验收标准 ----
        acceptance_limit: 验收限值 (Pa*m3/s), 默认 1.0e-10

        # ---- 设备信息（供报告使用） ----
        equipment:       设备信息字典
    """

    # ---- 项目信息 ----
    report_no: str = ""
    contract_no: str = ""
    test_date: str = ""
    product_code: str = ""
    product_name: str = ""
    weld_name: str = ""
    inspection_area: str = ""
    test_method: str = "Helium Spray Test"
    test_procedure_no: str = ""

    # ---- 环境条件 ----
    temperature: float = 13.5
    humidity: float = 40.0

    # ---- 标准漏孔参数 ----
    Q0_mantissa: float = 3.88
    Q0_exponent: int = -9
    T_cal: float = 20.0

    # ---- 检漏仪参数 ----
    I0_mantissa: float = 1.0
    I0_exponent: int = -13
    I_mantissa: float = 3.12
    I_exponent: int = -9

    # ---- 系统参数 ----
    M0_mantissa: float = 1.0
    M0_exponent: int = -13
    M1_mantissa: float = 3.32
    M1_exponent: int = -9
    M2_mantissa: float = 2.01
    M2_exponent: int = -13
    T_response: float = 50.0
    TG_percent: float = 100.0

    # ---- 可选噪声实测值（None = 使用默认公式 I0/10 或 M0/10） ----
    In_measured: Optional[float] = None
    Mn_measured: Optional[float] = None

    # ---- 验收标准 ----
    acceptance_limit: float = ACCEPTANCE_LIMIT_DEFAULT

    # ---- 设备信息 ----
    equipment: dict = field(default_factory=dict)

    # ---- 温度修正系数 (可覆盖) ----
    temp_coefficient: float = TEMP_COEFFICIENT

    def get_Q0(self) -> float:
        """返回标准漏孔标定漏率 Q0 (Pa*m3/s)。"""
        return self.Q0_mantissa * (10 ** self.Q0_exponent)

    def get_I0(self) -> float:
        """返回仪器本底 I0 (Pa*m3/s)。"""
        return self.I0_mantissa * (10 ** self.I0_exponent)

    def get_I(self) -> float:
        """返回仪器校准读数 I (Pa*m3/s)。"""
        return self.I_mantissa * (10 ** self.I_exponent)

    def get_M0(self) -> float:
        """返回系统本底 M0 (Pa*m3/s)。"""
        return self.M0_mantissa * (10 ** self.M0_exponent)

    def get_M1(self) -> float:
        """返回系统校准读数 M1 (Pa*m3/s)。"""
        return self.M1_mantissa * (10 ** self.M1_exponent)

    def get_M2(self) -> float:
        """返回喷氦后读数 M2 (Pa*m3/s)。"""
        return self.M2_mantissa * (10 ** self.M2_exponent)


# ============================================================================
# 计算结果数据类
# ============================================================================

@dataclass
class TestResult:
    """
    氦检漏试验计算结果。

    包含所有中间计算量和最终判定结果。

    Attributes:
        Q0:            标准漏孔标定漏率 (Pa*m3/s)
        Qt:            试验温度下的标准漏率 (Pa*m3/s)
        I0:            仪器本底 (Pa*m3/s)
        In:            仪器本底噪声 (Pa*m3/s)
        I:             仪器校准读数 (Pa*m3/s)
        Qmin:          仪器最小可检漏率 (Pa*m3/s)
        M0:            系统本底 (Pa*m3/s)
        Mn:            系统本底噪声 (Pa*m3/s)
        M1:            系统校准读数 (Pa*m3/s)
        Qeim:          系统最小可检漏率 (Pa*m3/s)
        M2:            喷氦后读数 (Pa*m3/s)
        Q_measured:    实测漏率 (Pa*m3/s)
        is_acceptable: 是否合格
        warnings:      计算过程中的警告信息
        errors:        计算过程中的错误信息
    """
    Q0: float = 0.0
    Qt: float = 0.0
    I0: float = 0.0
    In: float = 0.0
    I: float = 0.0
    Qmin: float = 0.0
    M0: float = 0.0
    Mn: float = 0.0
    M1: float = 0.0
    Qeim: float = 0.0
    M2: float = 0.0
    Q_measured: float = 0.0
    is_acceptable: bool = False
    warnings: List[str] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)


# ============================================================================
# 核心计算函数
# ============================================================================

def compute_Qt(Q0: float, T_cal: float, T_test: float,
               coeff: float = TEMP_COEFFICIENT) -> float:
    """
    【公式 1】温度修正：计算试验温度下的标准漏率 Qt。

    物理原理：
        氦气通过微小漏孔的漏率受温度影响。在 0~50°C 范围内，
        漏率随温度升高近似线性增加，系数约为 3%/°C。

    公式：
        Qt = Q0 * (1 - (T_cal - T_test) * k)

    其中 k = 0.03 /°C —— 当试验温度低于标定温度时，Qt < Q0（漏率减小）。

    Args:
        Q0:     标定温度下的标准漏率 (Pa*m3/s)
        T_cal:  标漏检定温度 (°C)，通常为 20°C
        T_test: 试验环境温度 (°C)
        coeff:  温度修正系数（默认 0.03）

    Returns:
        试验温度下的标准漏率 Qt (Pa*m3/s)

    Example:
        >>> compute_Qt(3.88e-9, 20.0, 13.5)
        3.1234e-09
    """
    return Q0 * (1 - (T_cal - T_test) * coeff)


def compute_noise(signal: float, ratio: float = NOISE_RATIO) -> float:
    """
    【公式 2 & 4】计算默认本底噪声（无实测值覆盖时使用）。

    噪声默认定义为本底信号的 1/10。

    Args:
        signal: 本底信号值 (Pa*m3/s)
        ratio:  噪声比（默认 10）

    Returns:
        默认噪声值 (Pa*m3/s)
    """
    return signal / ratio


def compute_effective_noise(base_signal: float,
                            measured: Optional[float] = None,
                            ratio: float = NOISE_RATIO) -> float:
    """
    计算有效本底噪声（含实测值覆盖判定）。

    规则（对应 Excel 中 In/Mn 单元格的深绿色底逻辑）：
        默认值 = base_signal / ratio
        若提供了实测噪声值，则取 max(实测值, 默认值)。
        若未提供实测值，直接使用默认值。

    此规则的物理意义：
        噪声只能被低估而不能被高估——若实测噪声比公式计算的大，
        说明存在额外噪声源，应使用更大的实测值以保证检测限判定的保守性。
        若实测噪声比公式计算的还小（可能是测量误差），保留默认值作为下限。

    Args:
        base_signal: 本底信号值（I0 或 M0）(Pa*m3/s)
        measured:    人工实测的噪声值 (Pa*m3/s)，None 表示未提供
        ratio:       噪声比（默认 10）

    Returns:
        有效噪声值 (Pa*m3/s)

    Example:
        >>> compute_effective_noise(1e-13)              # 无实测值
        1e-14
        >>> compute_effective_noise(1e-13, 5e-14)      # 实测值 > 默认值
        5e-14
        >>> compute_effective_noise(1e-13, 5e-15)      # 实测值 < 默认值，取默认值
        1e-14
    """
    default_noise = base_signal / ratio

    if measured is None:
        return default_noise

    # 取大不取小：实测 vs 默认值，取较大的作为有效噪声
    effective = max(measured, default_noise)
    return effective


def compute_Qmin(noise: float, Qt: float, I: float, I0: float) -> float:
    """
    【公式 3】计算仪器最小可检漏率 Qmin。

    物理意义：
        检漏仪在给定条件下能可靠检测到的最小漏率，
        由仪器信噪比决定。

    公式：
        Qmin = In * Qt / (I - I0)

    Args:
        noise: 仪器本底噪声 In (Pa*m3/s)
        Qt:    试验温度下标准漏率 (Pa*m3/s)
        I:     仪器校准读数 (Pa*m3/s)
        I0:    仪器本底 (Pa*m3/s)

    Returns:
        仪器最小可检漏率 (Pa*m3/s)

    Raises:
        ValueError: 当 (I - I0) <= 0 时（校准读数必须大于本底）
    """
    denom = I - I0
    if denom <= 0:
        raise ValueError(
            f"分母 (I - I0) = {denom:.3e} <= 0，无法计算 Qmin。"
            f"校准读数 I={I:.3e} 必须大于本底 I0={I0:.3e}。"
        )
    return noise * Qt / denom


def compute_Qeim(noise: float, Qt: float, M1: float, M0: float) -> float:
    """
    【公式 5】计算系统最小可检漏率 Qeim。

    物理意义：
        整个测试系统（仪器 + 管路 + 夹具）能可靠检测到的最小漏率。
        由于系统本底通常低于仪器本底，Qeim 通常优于 Qmin。

    公式：
        Qeim = Mn * Qt / (M1 - M0)

    Args:
        noise: 系统本底噪声 Mn (Pa*m3/s)
        Qt:    试验温度下标准漏率 (Pa*m3/s)
        M1:    系统校准读数 (Pa*m3/s)
        M0:    系统本底 (Pa*m3/s)

    Returns:
        系统最小可检漏率 (Pa*m3/s)

    Raises:
        ValueError: 当 (M1 - M0) <= 0 时
    """
    denom = M1 - M0
    if denom <= 0:
        raise ValueError(
            f"分母 (M1 - M0) = {denom:.3e} <= 0，无法计算 Qeim。"
            f"校准读数 M1={M1:.3e} 必须大于本底 M0={M0:.3e}。"
        )
    return noise * Qt / denom


def compute_Q(Qt: float, M2: float, M0: float, M1: float,
              TG_percent: float = 100.0) -> float:
    """
    【公式 6】计算实测漏率 Q。

    物理原理：
        喷氦后如果有泄漏，检漏仪读数从 M0 升至 M2，
        漏率与读数增量 (M2 - M0) 成正比，比例系数由系统校准确定。

    公式：
        Q = Qt * (M2 - M0) / (M1 - M0) * (100 / TG%)

    浓度修正说明：
        - 纯氦 (TG%=100): 修正因子 = 1
        - 10% 氦混合气 (TG%=10): 修正因子 = 10
          （只有 10% 的分子参与检测，实际漏率需放大 10 倍）

    Args:
        Qt:         试验温度下标准漏率 (Pa*m3/s)
        M2:         喷氦后读数 (Pa*m3/s)
        M0:         系统本底 (Pa*m3/s)
        M1:         系统校准读数 (Pa*m3/s)
        TG_percent: 氦浓度百分比 (%), 默认 100

    Returns:
        实测漏率 (Pa*m3/s)
    """
    delta_M = M2 - M0
    cal_factor = Qt / (M1 - M0)
    conc_correction = 100.0 / TG_percent if TG_percent > 0 else 1.0
    return delta_M * cal_factor * conc_correction


# ============================================================================
# 主计算类
# ============================================================================

class HeliumLeakTest:
    """
    氦检漏试验计算引擎。

    接收 TestInput，执行全部计算，存储结果，提供报告接口。

    使用方式：

        # 1. 构造输入参数
        params = TestInput(
            report_no="HD-LT26-0016",
            Q0_mantissa=3.88, Q0_exponent=-9,
            I0_mantissa=1,   I0_exponent=-13,
            ...
        )

        # 2. 创建计算实例并执行
        test = HeliumLeakTest(params)
        test.run()

        # 3. 获取结果
        result = test.result
        print(f"实测漏率: {result.Q_measured:.4e} Pa*m3/s")
        print(f"判定: {'合格' if result.is_acceptable else '不合格'}")

        # 4. 生成报告
        print(test.report())
        test.save_report("output.md")
    """

    def __init__(self, input_params: TestInput):
        """
        初始化计算引擎。

        Args:
            input_params: 试验输入参数
        """
        self.input = input_params
        self.result: Optional[TestResult] = None

    def run(self) -> TestResult:
        """
        执行完整计算流程：
          1. 从 mantissa/exponent 计算各物理量实际值
          2. 温度修正 → Qt
          3. 仪器校准 → In, Qmin
          4. 系统校准 → Mn, Qeim
          5. 实测漏率 → Q
          6. 验收判定
          7. 合理性验证

        Returns:
            TestResult 对象，包含所有中间值和最终判定
        """
        result = TestResult()
        inp = self.input

        # ---- 步骤 1：基础物理量 ----
        result.Q0 = inp.get_Q0()
        result.I0 = inp.get_I0()
        result.I  = inp.get_I()
        result.M0 = inp.get_M0()
        result.M1 = inp.get_M1()
        result.M2 = inp.get_M2()

        # ---- 步骤 2：温度修正 → Qt ----
        result.Qt = compute_Qt(result.Q0, inp.T_cal, inp.temperature,
                               inp.temp_coefficient)

        # ---- 步骤 3：仪器校准 ----
        # In = max(In_measured, I0/10) —— 若提供了实测噪声值则取大者
        result.In = compute_effective_noise(result.I0, inp.In_measured)
        try:
            result.Qmin = compute_Qmin(result.In, result.Qt, result.I, result.I0)
        except ValueError as e:
            result.errors.append(f"仪器 Qmin 计算失败: {e}")

        # ---- 步骤 4：系统校准 ----
        # Mn = max(Mn_measured, M0/10) —— 若提供了实测噪声值则取大者
        result.Mn = compute_effective_noise(result.M0, inp.Mn_measured)
        try:
            result.Qeim = compute_Qeim(result.Mn, result.Qt, result.M1, result.M0)
        except ValueError as e:
            result.errors.append(f"系统 Qeim 计算失败: {e}")

        # ---- 步骤 5：实测漏率 ----
        if not result.errors:
            try:
                result.Q_measured = compute_Q(
                    result.Qt, result.M2, result.M0, result.M1,
                    inp.TG_percent
                )
            except Exception as e:
                result.errors.append(f"实测漏率 Q 计算失败: {e}")

        # ---- 步骤 6：验收判定 ----
        result.is_acceptable = (
            result.Q_measured < inp.acceptance_limit
            and result.Q_measured > 0
        )

        # ---- 步骤 7：合理性验证 ----
        result.warnings = self._validate(result)

        self.result = result
        return result

    def _validate(self, r: TestResult) -> List[str]:
        """
        结果合理性验证（7 项检查）。

        Args:
            r: 计算结果

        Returns:
            警告信息列表
        """
        w = []
        inp = self.input

        # 1. 温度修正方向：低温 -> Qt < Q0
        if inp.temperature < inp.T_cal and r.Qt >= r.Q0:
            w.append(
                f"温度 T_test({inp.temperature}) < T_cal({inp.T_cal}) "
                f"但 Qt({r.Qt:.3e}) >= Q0({r.Q0:.3e})，温度修正方向可能异常"
            )

        # 2. 仪器校准信号 > 本底
        if r.I <= r.I0:
            w.append(
                f"仪器校准读数 I({r.I:.3e}) <= 本底 I0({r.I0:.3e})，"
                f"信号无法区分"
            )

        # 3. 系统校准信号 > 本底
        if r.M1 <= r.M0:
            w.append(
                f"系统校准读数 M1({r.M1:.3e}) <= 本底 M0({r.M0:.3e})，"
                f"信号无法区分"
            )

        # 4. Qeim 非负
        if r.Qeim <= 0:
            w.append(f"系统最小可检漏率 Qeim={r.Qeim:.3e} <= 0，异常")

        # 5. 喷氦后信号增量
        delta_M = r.M2 - r.M0
        if delta_M <= 0:
            w.append(
                f"喷氦后读数 M2({r.M2:.3e}) 未超过本底 M0({r.M0:.3e})，"
                f"可能无泄漏"
            )
        elif delta_M < r.Mn:
            w.append(
                f"信号增量 deltaM={delta_M:.3e} < 噪声 Mn={r.Mn:.3e}，"
                f"信号不可靠"
            )

        # 6. 漏率合理性
        if r.Q_measured < 0:
            w.append(f"实测漏率 Q={r.Q_measured:.3e} 为负值，异常")

        # 7. 氦浓度范围
        if not (0 < inp.TG_percent <= 100):
            w.append(f"氦浓度 TG%={inp.TG_percent} 不在 (0, 100] 范围")

        return w

    # ---- 报告输出 ----

    def summary(self) -> str:
        """单行结果摘要。"""
        if self.result is None:
            return "[未运行]"
        r = self.result
        status = "PASS" if r.is_acceptable else "FAIL"
        return (
            f"[{status}] {self.input.report_no} | "
            f"Q={r.Q_measured:.4e} Pa*m3/s | "
            f"验收标准: <{self.input.acceptance_limit:.1e}"
        )

    def report(self) -> str:
        """
        生成纯文本格式试验报告。

        Returns:
            多行文本报告
        """
        if self.result is None:
            return "错误：尚未执行计算，请先调用 run()。"

        inp = self.input
        r = self.result
        L = []  # 行缓冲

        L.append("=" * 72)
        L.append("  氦检漏试验数据处理报告")
        L.append("  Helium Leak Test Data Processing Report")
        L.append(f"  生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        L.append("=" * 72)

        # 1. 项目信息
        L.append("")
        L.append("  [1] 项目信息")
        L.append(f"    报告编号:       {inp.report_no}")
        L.append(f"    合同号:         {inp.contract_no}")
        L.append(f"    试验日期:       {inp.test_date}")
        L.append(f"    产品代码:       {inp.product_code}")
        L.append(f"    产品名称:       {inp.product_name}")
        L.append(f"    焊缝名称:       {inp.weld_name}")
        L.append(f"    检测区域:       {inp.inspection_area}")
        L.append(f"    试验方法:       {inp.test_method}")
        L.append(f"    试验温度:       {inp.temperature} C")
        L.append(f"    试验湿度:       {inp.humidity} %")

        # 2. 标准漏孔
        L.append("")
        L.append("  [2] 标准漏孔参数")
        L.append(f"    标定漏率 Q0:     {r.Q0:.3e} Pa*m3/s "
                 f"(尾数={inp.Q0_mantissa}, 指数={inp.Q0_exponent})")
        L.append(f"    标漏检定温度:   {inp.T_cal} C")
        L.append(f"    试验温度:       {inp.temperature} C")
        L.append(f"    试验温度下 Qt:   {r.Qt:.4e} Pa*m3/s")
        L.append(f"    温度修正系数:    {inp.temp_coefficient} /C")

        # 3. 检漏仪校准
        L.append("")
        L.append("  [3] 检漏仪校准")
        L.append(f"    仪器本底 I0:     {r.I0:.3e} Pa*m3/s "
                 f"(尾数={inp.I0_mantissa}, 指数={inp.I0_exponent})")
        _in_src = "实测值" if inp.In_measured is not None else "I0/10(默认)"
        L.append(f"    仪器本底噪声 In: {r.In:.3e} Pa*m3/s [来源: {_in_src}]")
        L.append(f"    校准读数 I:      {r.I:.3e} Pa*m3/s "
                 f"(尾数={inp.I_mantissa}, 指数={inp.I_exponent})")
        L.append(f"    仪器最小可检漏率 Qmin: {r.Qmin:.4e} Pa*m3/s")

        # 4. 系统校准
        L.append("")
        L.append("  [4] 系统校准与测试")
        L.append(f"    系统本底 M0:     {r.M0:.3e} Pa*m3/s "
                 f"(尾数={inp.M0_mantissa}, 指数={inp.M0_exponent})")
        _mn_src = "实测值" if inp.Mn_measured is not None else "M0/10(默认)"
        L.append(f"    系统本底噪声 Mn: {r.Mn:.3e} Pa*m3/s [来源: {_mn_src}]")
        L.append(f"    系统校准读数 M1: {r.M1:.3e} Pa*m3/s "
                 f"(尾数={inp.M1_mantissa}, 指数={inp.M1_exponent})")
        L.append(f"    系统最小可检漏率 Qeim: {r.Qeim:.4e} Pa*m3/s")
        L.append(f"    反应时间 T:      {inp.T_response} s")
        L.append(f"    喷氦后读数 M2:   {r.M2:.3e} Pa*m3/s "
                 f"(尾数={inp.M2_mantissa}, 指数={inp.M2_exponent})")
        L.append(f"    氦浓度 TG%:      {inp.TG_percent}%")

        # 5. 实测漏率
        L.append("")
        L.append("  [5] 实测漏率")
        L.append(f"    实测漏率 Q:      {r.Q_measured:.4e} Pa*m3/s")

        # 6. 判定
        L.append("")
        L.append("  [6] 判定结果")
        L.append(f"    实测漏率:        {r.Q_measured:.4e} Pa*m3/s")
        L.append(f"    验收标准:        < {inp.acceptance_limit:.1e} Pa*m3/s")

        if r.Q_measured > 0:
            margin = inp.acceptance_limit / r.Q_measured
            L.append(f"    安全裕度:        约 {margin:.0f} 倍 "
                     f"({10 * math.log10(margin):.0f} dB)")

        if r.is_acceptable:
            L.append(f"    判定:            [PASS] 合格 (Acceptable)")
        else:
            L.append(f"    判定:            [FAIL] 不合格 (Rejected)")

        # 警告/错误
        if r.warnings:
            L.append("")
            L.append(f"  [WARN] 验证警告 ({len(r.warnings)} 条):")
            for w in r.warnings:
                L.append(f"    - {w}")

        if r.errors:
            L.append("")
            L.append(f"  [ERROR] 计算错误 ({len(r.errors)} 条):")
            for e in r.errors:
                L.append(f"    - {e}")

        L.append("")
        L.append("=" * 72)
        L.append("  报告结束")
        L.append("=" * 72)

        return "\n".join(L)

    def to_markdown(self) -> str:
        """
        生成 Markdown 格式试验报告。

        Returns:
            Markdown 文本
        """
        if self.result is None:
            return "**错误：尚未执行计算，请先调用 run()。**"

        inp = self.input
        r = self.result
        L = []

        L.append("# 氦检漏试验数据处理报告")
        L.append("")
        L.append(f"**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        L.append("")

        # 1. 项目信息
        L.append("## 1. 项目信息")
        L.append("")
        L.append("| 项目 | 内容 |")
        L.append("|------|------|")
        L.append(f"| 报告编号 | {inp.report_no} |")
        L.append(f"| 合同号 | {inp.contract_no} |")
        L.append(f"| 试验日期 | {inp.test_date} |")
        L.append(f"| 产品代码 | {inp.product_code} |")
        L.append(f"| 产品名称 | {inp.product_name} |")
        L.append(f"| 焊缝名称 | {inp.weld_name} |")
        L.append(f"| 检测区域 | {inp.inspection_area} |")
        L.append(f"| 试验方法 | {inp.test_method} |")
        L.append(f"| 试验温度 | {inp.temperature} °C |")
        L.append(f"| 试验湿度 | {inp.humidity}% |")
        L.append("")

        # 2. 标准漏孔
        L.append("## 2. 标准漏孔参数")
        L.append("")
        L.append("| 参数 | 符号 | 数值 |")
        L.append("|------|------|------|")
        L.append(f"| 标定漏率 | Q0 | {r.Q0:.3e} Pa·m³/s |")
        L.append(f"| 标漏检定温度 | T_cal | {inp.T_cal} °C |")
        L.append(f"| 试验温度 | T_test | {inp.temperature} °C |")
        L.append(f"| 试验温度下标准漏率 | Qt | {r.Qt:.4e} Pa·m³/s |")
        L.append("")

        # 3. 检漏仪校准
        L.append("## 3. 检漏仪校准")
        L.append("")
        L.append("| 参数 | 符号 | 数值 |")
        L.append("|------|------|------|")
        L.append(f"| 仪器本底 | I0 | {r.I0:.3e} Pa·m³/s |")
        L.append(f"| 仪器本底噪声 | In | {r.In:.3e} Pa·m³/s |")
        L.append(f"| 校准读数 | I | {r.I:.3e} Pa·m³/s |")
        L.append(f"| 仪器最小可检漏率 | Qmin | {r.Qmin:.4e} Pa·m³/s |")
        L.append("")

        # 4. 系统校准
        L.append("## 4. 系统校准与测试")
        L.append("")
        L.append("| 参数 | 符号 | 数值 |")
        L.append("|------|------|------|")
        L.append(f"| 系统本底 | M0 | {r.M0:.3e} Pa·m³/s |")
        L.append(f"| 系统本底噪声 | Mn | {r.Mn:.3e} Pa·m³/s |")
        L.append(f"| 系统校准读数 | M1 | {r.M1:.3e} Pa·m³/s |")
        L.append(f"| 系统最小可检漏率 | Qeim | {r.Qeim:.4e} Pa·m³/s |")
        L.append(f"| 反应时间 | T | {inp.T_response} s |")
        L.append(f"| 喷氦后读数 | M2 | {r.M2:.3e} Pa·m³/s |")
        L.append(f"| 氦浓度 | TG% | {inp.TG_percent}% |")
        L.append("")

        # 5. 实测漏率
        L.append("## 5. 实测漏率与判定")
        L.append("")
        L.append("| 项目 | 数值 |")
        L.append("|------|------|")
        L.append(f"| 实测漏率 Q | **{r.Q_measured:.4e} Pa·m³/s** |")
        L.append(f"| 验收标准 | < {inp.acceptance_limit:.1e} Pa·m³/s |")

        if r.Q_measured > 0:
            margin = inp.acceptance_limit / r.Q_measured
            L.append(f"| 安全裕度 | 约 {margin:.0f} 倍 |")

        status = "[PASS] 合格 (Acceptable)" if r.is_acceptable else "[FAIL] 不合格 (Rejected)"
        L.append(f"| **判定** | **{status}** |")
        L.append("")

        # 警告
        if r.warnings:
            L.append("## 验证警告")
            L.append("")
            for w in r.warnings:
                L.append(f"- {w}")
            L.append("")

        if r.errors:
            L.append("## 计算错误")
            L.append("")
            for e in r.errors:
                L.append(f"- {e}")
            L.append("")

        # 附录
        L.append("---")
        L.append("## 附录：计算公式")
        L.append("")
        L.append("| # | 名称 | 公式 | 说明 |")
        L.append("|---|------|------|------|")
        L.append(f"| 1 | 温度修正 | Qt = Q0 * (1 - (T_cal - T_test) * {inp.temp_coefficient}) | 氦气温度修正系数 |")
        L.append("| 2 | 仪器本底噪声 | In = max(In_measured, I0/10) | 默认 I0/10，实测值大则取实测 |")
        L.append("| 3 | 仪器最小可检漏率 | Qmin = In * Qt / (I - I0) | 信噪比法 |")
        L.append("| 4 | 系统本底噪声 | Mn = max(Mn_measured, M0/10) | 默认 M0/10，实测值大则取实测 |")
        L.append("| 5 | 系统最小可检漏率 | Qeim = Mn * Qt / (M1 - M0) | 信噪比法 |")
        L.append("| 6 | 实测漏率 | Q = Qt * (M2 - M0) / (M1 - M0) * (100/TG%) | 含氦浓度修正 |")

        return "\n".join(L)

    def save_report(self, filepath: str) -> None:
        """
        将 Markdown 报告保存到文件。

        Args:
            filepath: 输出文件路径
        """
        md = self.to_markdown()
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(md)
        print(f"[OK] 报告已保存至: {filepath}")


# ============================================================================
# 便捷函数
# ============================================================================

def quick_test(**kwargs) -> TestResult:
    """
    快速执行一次试验计算（一行代码）。

    参数同 TestInput，未提供的使用默认值。

    Example:
        >>> result = quick_test(
        ...     report_no="HD-LT26-0016",
        ...     Q0_mantissa=3.88, Q0_exponent=-9,
        ...     I0_mantissa=1, I0_exponent=-13,
        ...     M2_mantissa=2.01, M2_exponent=-13,
        ... )
        >>> print(f"Q = {result.Q_measured:.4e}")

    Returns:
        TestResult 对象
    """
    inp = TestInput(**kwargs)
    test = HeliumLeakTest(inp)
    return test.run()


# ============================================================================
# 程序入口（演示示例）
# ============================================================================

if __name__ == "__main__":
    # ----- 示例 1：使用原始 Excel 中的数据 -----
    print("=" * 72)
    print("  示例 1: HD-LT26-0016 (阀门箱 B1-C04)")
    print("=" * 72)

    test1 = HeliumLeakTest(TestInput(
        report_no="HD-LT26-0016",
        contract_no="SX300250223",
        test_date="2026.03.12",
        product_code="H09515",
        product_name="阀门箱B1-C04",
        weld_name="SWB108",
        inspection_area="IN_TUBE_01 (顶盖板内管1)",
        test_method="Helium Spray Test",
        test_procedure_no="SX300250223-JD-007",
        temperature=20,
        humidity=40.0,

        # 标准漏孔
        Q0_mantissa=3.88, Q0_exponent=-9, T_cal=20.0,

        # 检漏仪
        I0_mantissa=5,   I0_exponent=-13,
        I_mantissa=3.12, I_exponent=-9,

        # 系统
        M0_mantissa=1,   M0_exponent=-13,
        M1_mantissa=3.32, M1_exponent=-9,
        M2_mantissa=5.01, M2_exponent=-13,
        T_response=50,
        TG_percent=100,

        # 设备信息
        equipment={
            "检漏仪": "250002v02 / 90001689449 / 2027.01.03",
            "标准漏孔": "LK-9 / D200745 / 2026.10.16",
            "温度计": "WS2080B / HD-053 / 2026.06.22",
            "氦浓度计": "PLT300-He / PLT0622530488 / 2026.12.11",
        },
    ))
    test1.run()
    print(test1.report())

    # ----- 示例 2：噪声覆盖规则演示 -----
    print("\n")
    print("=" * 72)
    print("  示例 2: In / Mn 噪声覆盖规则演示")
    print("  max(实测值, 默认值 I0/10 或 M0/10)")
    print("=" * 72)

    I0 = 1e-13
    M0 = 1e-13
    default_In = I0 / 10
    default_Mn = M0 / 10
    print(f"\n  I0={I0:.1e}, 默认 In=I0/10={default_In:.1e}")
    print(f"  M0={M0:.1e}, 默认 Mn=M0/10={default_Mn:.1e}")

    test_cases = [
        # (In_measured, Mn_measured, 说明)
        (None,      None,      "无实测值 -> 使用默认公式"),
        (5e-14,     2e-14,     "实测值 > 默认值 -> 取实测值"),
        (5e-15,     5e-15,     "实测值 < 默认值 -> 取默认值(保守)"),
        (1e-14,     1e-14,     "实测值 = 默认值 -> 取默认值"),
    ]

    for In_m, Mn_m, desc in test_cases:
        r = quick_test(
            report_no=desc,
            Q0_mantissa=3.88, Q0_exponent=-9, T_cal=20.0, temperature=13.5,
            I0_mantissa=1, I0_exponent=-13,
            I_mantissa=3.12, I_exponent=-9,
            M0_mantissa=1, M0_exponent=-13,
            M1_mantissa=3.32, M1_exponent=-9,
            M2_mantissa=2.01, M2_exponent=-13,
            TG_percent=100,
            In_measured=In_m, Mn_measured=Mn_m,
        )
        print(f"\n  {desc}")
        print(f"    In_measured={In_m}, Mn_measured={Mn_m}")
        print(f"    Effective In={r.In:.3e}, Effective Mn={r.Mn:.3e}")
        print(f"    Qmin={r.Qmin:.4e}, Qeim={r.Qeim:.4e}, Q={r.Q_measured:.4e}")

    # ----- 示例 3：不同温度下的快速对比 -----
    print("\n")
    print("=" * 72)
    print("  示例 3: 不同温度下的快速对比")
    print("=" * 72)

    for temp in [10, 15, 20, 25, 30]:
        result = quick_test(
            report_no=f"T={temp}C",
            Q0_mantissa=3.88, Q0_exponent=-9, T_cal=20.0,
            temperature=temp,
            I0_mantissa=1,   I0_exponent=-13,
            I_mantissa=3.12, I_exponent=-9,
            M0_mantissa=1,   M0_exponent=-13,
            M1_mantissa=3.32, M1_exponent=-9,
            M2_mantissa=2.01, M2_exponent=-13,
            TG_percent=100,
        )
        print(f"  T={temp:5.1f}C  Qt={result.Qt:.4e}  "
              f"Q={result.Q_measured:.4e}  "
              f"{'[PASS]' if result.is_acceptable else '[FAIL]'}")

    # ----- 示例 4：保存 Markdown 报告 -----
    test1.save_report("helium_leak_test_report_pure_python.md")

    print("\n[OK] 所有示例执行完毕。")
