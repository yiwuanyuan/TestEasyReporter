# 氦检漏计算器 Android 应用 —— 项目策划方案

> 版本：V1.0 | 日期：2026-06-10 | 状态：待评审

---

## 目录

1. [项目概述与背景](#1-项目概述与背景)
2. [技术选型](#2-技术选型)
3. [功能模块划分](#3-功能模块划分)
4. [业务流程图](#4-业务流程图)
5. [数据库表结构设计](#5-数据库表结构设计)
6. [界面原型与交互设计](#6-界面原型与交互设计)
7. [项目架构设计](#7-项目架构设计)
8. [开发阶段与时间规划](#8-开发阶段与时间规划)
9. [测试方案与质量保证](#9-测试方案与质量保证)
10. [打包发布与维护](#10-打包发布与维护)
11. [附录：Python 计算逻辑映射表](#11-附录python-计算逻辑映射表)

---

## 1. 项目概述与背景

### 1.1 项目定位

将已验证的 Python 氦检漏计算引擎（`helium_leak_test.py`）移植为专业的 Android 原生应用，面向工业无损检测（NDT）领域的现场工程师，提供便携、离线可用的氦质谱检漏数据计算与记录管理工具。

### 1.2 核心目标

| 目标            | 指标                                                |
| ------------- | ------------------------------------------------- |
| 100% 计算逻辑保真   | Python 6 大公式、噪声覆盖规则、7 项合理性验证全部无差异复现               |
| 原生 Android 体验 | Material Design 3 工业风格，APK ≤ 15MB，支持 Android 8.0+ |
| 离线数据管理        | SQLite 本地存储，支持 ≥ 5000 条历史记录的流畅检索                  |
| 数据互通          | 支持 JSON/CSV 导出，可分享至微信/邮件/网盘                       |

### 1.3 目标用户画像

| 角色 | 使用场景 |
|------|---------|
| 现场检测工程师 | 在车间/户外输入检测参数，即时获取漏率计算结果与合格判定 |
| 质检审核人员 | 查看历史记录、复核计算过程、导出报告归档 |
| 设备管理员 | 管理检测设备台账、监控设备校准有效期 |

---

## 2. 技术选型

### 2.1 总体技术栈

```
┌─────────────────────────────────────────────────────┐
│                    技术栈全景图                        │
├──────────────┬──────────────────────────────────────┤
│ 编程语言      │ Kotlin 2.0+                           │
│ UI 框架      │ Jetpack Compose + Material Design 3   │
│ 架构模式      │ MVVM + Repository + UseCase           │
│ 依赖注入      │ Hilt (Dagger)                         │
│ 数据库        │ Room (SQLite 抽象层)                   │
│ 异步处理      │ Kotlin Coroutines + Flow              │
│ 导航         │ Jetpack Navigation Compose             │
│ 构建系统      │ Gradle 8.x + Kotlin DSL + Version Catalog │
│ 最低 SDK     │ API 26 (Android 8.0)                   │
│ 目标 SDK     │ API 35 (Android 15)                    │
│ 测试框架      │ JUnit5 + MockK + Turbine + Compose Testing│
│ 代码静态分析  │ Ktlint + Detekt                        │
│ CI/CD       │ GitHub Actions                         │
└──────────────┴──────────────────────────────────────┘
```

### 2.2 关键依赖库

```toml
# gradle/libs.versions.toml (Version Catalog)

[versions]
kotlin = "2.3.21"
agp = "8.7.3"
compose-bom = "2026.05.00"
room = "2.8.4"
hilt = "2.53"
hilt-navigation-compose = "1.3.0"
navigation = "2.8.7"
lifecycle = "2.10.0"
activity-compose = "1.13.0"
coroutines = "1.9.0"

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.3.21-1.0.28" }

[libraries]
# Compose (版本由 BOM 统一管理)
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-material3-adaptive = { group = "androidx.compose.material3.adaptive", name = "adaptive" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }

# Activity & Lifecycle
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Data
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version = "1.1.3" }
gson = { group = "com.google.code.gson", name = "gson", version = "2.11" }

# Testing
junit5 = { group = "org.junit.jupiter", name = "junit-jupiter", version = "5.11" }
mockk = { group = "io.mockk", name = "mockk", version = "1.13.14" }
turbine = { group = "app.cash.turbine", name = "turbine", version = "1.2.0" }
```

### 2.3 选型理由

| 选择                  | 理由                                                    |
| ------------------- | ----------------------------------------------------- |
| **Kotlin**          | Google 官方推荐；空安全避免 NullPointerException；协程简化异步         |
| **Jetpack Compose** | 声明式 UI，代码量比 XML 减少 40%；Material 3 内置主题系统              |
| **Room**            | 编译时 SQL 校验；Flow 响应式查询；自动迁移                            |
| **Hilt**            | 官方推荐 DI 方案；对 Jetpack 组件的一等支持                          |
| **MVVM**            | 与 Compose + Room 天然契合；ViewModel 管理配置变更                |
| **UseCase 层**       | 将 Python 计算函数映射为纯净的 Kotlin UseCase，零 Android 依赖，可独立单测 |

---

## 3. 功能模块划分

### 3.1 功能模块总览

```
HeliumLeakDetector (APP)
│
├── 📐 计算引擎模块 (engine)
│   ├── 温度修正计算        Qt = Q0*(1-(T_cal-T_test)*0.03)
│   ├── 噪声判定            In=max(In_measured, I0/10), Mn=max(Mn_measured, M0/10)
│   ├── 仪器/系统灵敏度      Qmin, Qeim
│   ├── 实测漏率            Q = Qt*(M2-M0)/(M1-M0)*(100/TG%)
│   ├── 验收判定            Q < acceptance_limit → PASS/FAIL
│   └── 7项合理性验证       (温度方向、信噪比、范围检查等)
│
├── 📝 检测记录模块 (records)
│   ├── 新建检测记录        (输入所有人工参数)
│   ├── 编辑已有记录        (修改后自动重算)
│   ├── 删除记录            (软删除/硬删除 + 确认)
│   ├── 记录详情查看        (完整参数 + 计算过程展示)
│   ├── 列表浏览            (分页 + 搜索 + 排序)
│   ├── 收藏/标记           (快速定位重要记录)
│   └── 批量操作            (多选删除/导出)
│
├── 📊 报告导出模块 (export)
│   ├── Markdown 报告导出   (与 Python to_markdown() 输出一致)
│   ├── JSON 数据导出       (结构化，支持二次导入)
│   ├── CSV 表格导出        (Excel 可直接打开)
│   ├── 分享到外部应用      (微信/邮件/蓝牙)
│   └── 打印/PDF            (Android Print API)
│
├── ⚙️ 设备管理模块 (equipment)
│   ├── 设备台账            (检漏仪/标准漏孔/温度计/氦浓度计)
│   ├── 校准有效期管理       (到期提醒通知)
│   └── 设备关联检测记录     (追溯某设备参与的所有检测)
│
├── 🎛️ 设置模块 (settings)
│   ├── 验收标准默认值       (1.0e-10 Pa·m³/s 可改)
│   ├── 温度修正系数         (默认 0.03，可改)
│   ├── 噪声比              (默认 10)
│   ├── 语言切换            (中文/English)
│   ├── 深色模式            (跟随系统 / 手动)
│   └── 数据备份与恢复       (ZIP 压缩 + 导入)
│
└── ℹ️ 帮助模块 (help)
    ├── 公式说明            (6大公式 + 物理意义)
    ├── 操作指南            (图文步骤)
    ├── 关于应用            (版本号 / 许可证)
    └── 问题反馈            (邮件/issue)
```

### 3.2 功能优先级（MVP → 完整版）

| 阶段 | 功能范围 |
|------|---------|
| **MVP (4周)** | 计算引擎 + 新建/查看记录 + 列表浏览 + Markdown 导出 |
| **V1.1 (2周)** | 编辑/删除 + JSON/CSV 导出 + 搜索排序 |
| **V1.2 (2周)** | 设备管理 + 校准提醒 |
| **V2.0 (3周)** | 数据备份恢复 + 批量操作 + 深色模式 + 国际化 |

---

## 4. 业务流程图

### 4.1 主业务流程

```
┌─────────────┐
│  应用启动     │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌─────────────────┐
│  记录列表     │────▶│ 点击 "+" FAB     │
│  (首页)      │     │ 新建检测记录     │
└──────┬──────┘     └───────┬─────────┘
       │                    │
       │ 点击已有记录         │ 填写参数表单
       ▼                    ▼
┌─────────────┐     ┌─────────────────┐
│  记录详情     │     │  参数输入表单    │
│  (只读)      │     │  - 项目信息      │
│             │     │  - 标准漏孔参数   │
│  点击"编辑"  │     │  - 检漏仪参数    │
│  ──────────▶│     │  - 系统参数      │
│             │     │  - 噪声实测值(可选)│
└──────┬──────┘     └───────┬─────────┘
       │                    │
       │                    │ 点击"计算"
       ▼                    ▼
┌─────────────┐     ┌─────────────────┐
│  编辑模式     │     │  CALCULATION     │
│  (修改参数)   │     │  ENGINE          │
│             │     │  ─────────────── │
│  保存后自动   │     │  1. 合并尾数/指数  │
│  重新计算    │     │  2. 温度修正 → Qt  │
└──────┬──────┘     │  3. 噪声判定 → In,Mn│
       │            │  4. 仪器校准 → Qmin │
       │            │  5. 系统校准 → Qeim │
       ▼            │  6. 实测漏率 → Q    │
┌─────────────┐     │  7. 合理性验证      │
│  保存/取消    │     │  8. 验收判定       │
└─────────────┘     └───────┬─────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │  计算结果展示     │
                   │  ┌─────────────┐ │
                   │  │ PASS / FAIL │ │
                   │  │ Q = x.xxe-xx│ │
                   │  │ 安全裕度: xx dB│ │
                   │  │ 点击展开详情  │ │
                   │  └─────────────┘ │
                   │                 │
                   │ [保存] [导出] [分享]│
                   └─────────────────┘
```

### 4.2 计算引擎内部流程

```
              input: TestInput (所有人工参数)
                          │
         ┌────────────────┼────────────────┐
         ▼                ▼                 ▼
   mantissa * 10^exp  T_cal, T_test    In_measured?
   → Q0, I0, I, M0,                       │
     M1, M2                        ┌──────┴──────┐
         │                         │  None → I0/10│
         ▼                         │  val  → max() │
   [1] compute_Qt(Q0,T_cal,T_test) └──────┬──────┘
         │                                │
         ▼                                ▼
        Qt                          In_effective
         │                                │
         ├────────────────────────────────┤
         │                                │
         ▼                                ▼
   [2] compute_Qmin(In, Qt, I, I0)  compute_Qeim(Mn, Qt, M1, M0)
         │                                │
         ▼                                ▼
       Qmin                             Qeim
                                          │
   [3] compute_Q(Qt, M2, M0, M1, TG%) ◀──┘
         │
         ▼
        Q_measured
         │
         ▼
   [4] Q < acceptance_limit?  ──▶ PASS / FAIL
         │
         ▼
   [5] 7 validation checks  ──▶ warnings[]
         │
         ▼
              output: TestResult
```

---

## 5. 数据库表结构设计

### 5.1 ER 图（实体关系）

```
┌──────────────────────┐       ┌──────────────────────────┐
│   detection_records   │       │      equipment            │
├──────────────────────┤       ├──────────────────────────┤
│ PK id (INTEGER)      │──┐    │ PK id (INTEGER)          │
│    report_no (TEXT)  │  │    │    name (TEXT)           │
│    contract_no       │  │    │    model (TEXT)          │
│    test_date         │  │    │    serial_no (TEXT)      │
│    product_code      │  │    │    category (TEXT)       │
│    product_name      │  │    │    valid_until (TEXT)    │
│    weld_name         │  │    │    status (INTEGER)      │
│    inspection_area   │  │    │    notes (TEXT)          │
│    test_method       │  │    │    created_at            │
│    temperature       │  │    │    updated_at            │
│    humidity          │  │    └──────────────────────────┘
│    Q0_mantissa       │  │               │
│    Q0_exponent       │  │               │ N:M via
│    T_cal             │  │               │ record_equipment
│    I0_mantissa       │  │               │ (junction table)
│    I0_exponent       │  │               │
│    I_mantissa        │  │    ┌──────────────────────────┐
│    I_exponent        │  │    │   reminders               │
│    M0_mantissa       │  │    ├──────────────────────────┤
│    M0_exponent       │  │    │ PK id (INTEGER)          │
│    M1_mantissa       │  │    │ FK equipment_id          │
│    M1_exponent       │  │    │    title (TEXT)          │
│    M2_mantissa       │  │    │    message (TEXT)        │
│    M2_exponent       │  │    │    remind_date (TEXT)    │
│    T_response        │  │    │    is_dismissed (INT)    │
│    TG_percent        │  │    │    created_at            │
│    In_measured (NULL)│  │    └──────────────────────────┘
│    Mn_measured (NULL)│  │
│    -- 计算结果(冗余)  │  │
│    Q0_value (REAL)   │  │
│    Qt_value (REAL)   │  │
│    I0_value (REAL)   │  │
│    In_value (REAL)   │  │
│    Qmin_value        │  │
│    M0_value          │  │
│    Mn_value          │  │
│    Qeim_value        │  │
│    Q_measured        │  │
│    is_acceptable(INT)│  │
│    warnings (TEXT)   │  │
│    -- 元数据          │  │
│    is_favorite(INT)  │  │
│    is_deleted (INT)  │  │
│    notes (TEXT)      │  │
│    created_at        │  │
│    updated_at        │──┤
└──────────────────────┘  │
                          │
┌──────────────────────────┴──────────────────────────────┐
│              record_equipment (关联表)                    │
├─────────────────────────────────────────────────────────┤
│ FK record_id (INTEGER)                                  │
│ FK equipment_id (INTEGER)                               │
│ PK (record_id, equipment_id)                            │
└─────────────────────────────────────────────────────────┘
```

### 5.2 建表 DDL

```sql
-- ==========================================
-- 表 1: 检测记录表 (主表)
-- ==========================================
CREATE TABLE detection_records (
    -- 主键
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 项目信息 (人工输入, 绿色底)
    report_no       TEXT    NOT NULL DEFAULT '',
    contract_no     TEXT    NOT NULL DEFAULT '',
    test_date       TEXT    NOT NULL DEFAULT '',   -- 格式 yyyy.MM.dd
    product_code    TEXT    NOT NULL DEFAULT '',
    product_name    TEXT    NOT NULL DEFAULT '',
    weld_name       TEXT    NOT NULL DEFAULT '',
    inspection_area TEXT    NOT NULL DEFAULT '',
    test_method     TEXT    NOT NULL DEFAULT 'Helium Spray Test',
    test_procedure_no TEXT  NOT NULL DEFAULT '',

    -- 环境条件 (人工输入, 绿色底)
    temperature     REAL    NOT NULL DEFAULT 20.0,   -- °C
    humidity        REAL    NOT NULL DEFAULT 40.0,   -- %

    -- 标准漏孔 (人工输入, 绿色底)
    Q0_mantissa     REAL    NOT NULL DEFAULT 3.88,
    Q0_exponent     INTEGER NOT NULL DEFAULT -9,
    T_cal           REAL    NOT NULL DEFAULT 20.0,

    -- 检漏仪 (人工输入, 绿色底)
    I0_mantissa     REAL    NOT NULL DEFAULT 1.0,
    I0_exponent     INTEGER NOT NULL DEFAULT -13,
    I_mantissa      REAL    NOT NULL DEFAULT 3.12,
    I_exponent      INTEGER NOT NULL DEFAULT -9,

    -- 系统 (人工输入, 绿色底)
    M0_mantissa     REAL    NOT NULL DEFAULT 1.0,
    M0_exponent     INTEGER NOT NULL DEFAULT -13,
    M1_mantissa     REAL    NOT NULL DEFAULT 3.32,
    M1_exponent     INTEGER NOT NULL DEFAULT -9,
    M2_mantissa     REAL    NOT NULL DEFAULT 2.01,
    M2_exponent     INTEGER NOT NULL DEFAULT -13,
    T_response      REAL    NOT NULL DEFAULT 50.0,
    TG_percent      REAL    NOT NULL DEFAULT 100.0,

    -- 噪声实测值 (可覆盖, 深绿色底)
    In_measured     REAL,           -- NULL = 使用 I0/10 默认公式
    Mn_measured     REAL,           -- NULL = 使用 M0/10 默认公式

    -- 验收标准 (可配置)
    acceptance_limit REAL   NOT NULL DEFAULT 1.0e-10,

    -- 温度修正系数 (可配置)
    temp_coefficient  REAL  NOT NULL DEFAULT 0.03,

    -- ======== 以下为计算结果 (冗余存储, 白底自动计算) ========
    Q0_value        REAL,
    Qt_value        REAL,
    I0_value        REAL,
    In_value        REAL,
    I_value         REAL,
    Qmin_value      REAL,
    M0_value        REAL,
    Mn_value        REAL,
    M1_value        REAL,
    Qeim_value      REAL,
    M2_value        REAL,
    Q_measured      REAL,
    is_acceptable   INTEGER NOT NULL DEFAULT 0,  -- 0=否, 1=是
    warnings        TEXT,                         -- JSON array of strings

    -- 元数据
    is_favorite     INTEGER NOT NULL DEFAULT 0,
    is_deleted      INTEGER NOT NULL DEFAULT 0,   -- 软删除标记
    notes           TEXT    NOT NULL DEFAULT '',
    created_at      INTEGER NOT NULL,             -- Unix timestamp ms
    updated_at      INTEGER NOT NULL
);

-- 索引
CREATE INDEX idx_records_created_at ON detection_records(created_at DESC);
CREATE INDEX idx_records_report_no  ON detection_records(report_no);
CREATE INDEX idx_records_product    ON detection_records(product_name);
CREATE INDEX idx_records_test_date  ON detection_records(test_date);
CREATE INDEX idx_records_acceptable ON detection_records(is_acceptable);
CREATE INDEX idx_records_deleted    ON detection_records(is_deleted);
CREATE INDEX idx_records_favorite   ON detection_records(is_favorite);

-- 全文搜索 (FTS5)
CREATE VIRTUAL TABLE detection_records_fts USING fts5(
    report_no, contract_no, product_name, weld_name,
    inspection_area, notes,
    content='detection_records',
    content_rowid='id'
);


-- ==========================================
-- 表 2: 设备台账表
-- ==========================================
CREATE TABLE equipment (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,          -- 设备名称 (如 "检漏仪")
    model           TEXT    NOT NULL DEFAULT '',
    serial_no       TEXT    NOT NULL DEFAULT '', -- 出厂编号
    category        INTEGER NOT NULL DEFAULT 0,  -- 0=检漏仪,1=标准漏孔,2=温度计,3=氦浓度计
    valid_from      TEXT,                       -- 校准生效日期
    valid_until     TEXT,                       -- 校准有效期至
    status          INTEGER NOT NULL DEFAULT 1,  -- 0=停用, 1=正常, 2=即将过期, 3=已过期
    notes           TEXT    NOT NULL DEFAULT '',
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);

CREATE INDEX idx_equipment_category ON equipment(category);
CREATE INDEX idx_equipment_status   ON equipment(status);
CREATE INDEX idx_equipment_valid    ON equipment(valid_until);


-- ==========================================
-- 表 3: 记录-设备关联表 (多对多)
-- ==========================================
CREATE TABLE record_equipment (
    record_id    INTEGER NOT NULL,
    equipment_id INTEGER NOT NULL,
    PRIMARY KEY (record_id, equipment_id),
    FOREIGN KEY (record_id)    REFERENCES detection_records(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipment(id)          ON DELETE CASCADE
);


-- ==========================================
-- 表 4: 校准提醒表
-- ==========================================
CREATE TABLE reminders (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_id  INTEGER NOT NULL,
    title         TEXT    NOT NULL,
    message       TEXT    NOT NULL DEFAULT '',
    remind_date   TEXT    NOT NULL,            -- 提醒日期
    is_dismissed  INTEGER NOT NULL DEFAULT 0,
    created_at    INTEGER NOT NULL,
    FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE
);

CREATE INDEX idx_reminders_date     ON reminders(remind_date);
CREATE INDEX idx_reminders_dismissed ON reminders(is_dismissed);
```

### 5.3 Room Entity 映射示例

```kotlin
// DetectionRecordEntity.kt
@Entity(
    tableName = "detection_records",
    indices = [
        Index("created_at"),
        Index("report_no"),
        Index("is_deleted"),
    ]
)
data class DetectionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // 项目信息
    @ColumnInfo(name = "report_no")       val reportNo: String = "",
    @ColumnInfo(name = "contract_no")     val contractNo: String = "",
    @ColumnInfo(name = "test_date")       val testDate: String = "",
    @ColumnInfo(name = "product_code")    val productCode: String = "",
    @ColumnInfo(name = "product_name")    val productName: String = "",
    @ColumnInfo(name = "weld_name")       val weldName: String = "",
    @ColumnInfo(name = "inspection_area") val inspectionArea: String = "",
    @ColumnInfo(name = "test_method")     val testMethod: String = "Helium Spray Test",

    // 环境
    val temperature: Double = 20.0,
    val humidity: Double = 40.0,

    // 标准漏孔
    @ColumnInfo(name = "Q0_mantissa") val q0Mantissa: Double = 3.88,
    @ColumnInfo(name = "Q0_exponent") val q0Exponent: Int = -9,
    @ColumnInfo(name = "T_cal")       val tCal: Double = 20.0,

    // 检漏仪
    @ColumnInfo(name = "I0_mantissa") val i0Mantissa: Double = 1.0,
    @ColumnInfo(name = "I0_exponent") val i0Exponent: Int = -13,
    @ColumnInfo(name = "I_mantissa")  val iMantissa: Double = 3.12,
    @ColumnInfo(name = "I_exponent")  val iExponent: Int = -9,

    // 系统
    @ColumnInfo(name = "M0_mantissa") val m0Mantissa: Double = 1.0,
    @ColumnInfo(name = "M0_exponent") val m0Exponent: Int = -13,
    @ColumnInfo(name = "M1_mantissa") val m1Mantissa: Double = 3.32,
    @ColumnInfo(name = "M1_exponent") val m1Exponent: Int = -9,
    @ColumnInfo(name = "M2_mantissa") val m2Mantissa: Double = 2.01,
    @ColumnInfo(name = "M2_exponent") val m2Exponent: Int = -13,
    @ColumnInfo(name = "T_response")  val tResponse: Double = 50.0,
    @ColumnInfo(name = "TG_percent")  val tgPercent: Double = 100.0,

    // 噪声实测值
    @ColumnInfo(name = "In_measured") val inMeasured: Double? = null,
    @ColumnInfo(name = "Mn_measured") val mnMeasured: Double? = null,

    // 验收 & 系数
    @ColumnInfo(name = "acceptance_limit") val acceptanceLimit: Double = 1.0e-10,
    @ColumnInfo(name = "temp_coefficient") val tempCoefficient: Double = 0.03,

    // 计算结果 (冗余)
    @ColumnInfo(name = "Q_measured")    val qMeasured: Double = 0.0,
    @ColumnInfo(name = "is_acceptable") val isAcceptable: Boolean = false,
    val warnings: String = "",   // JSON

    // 元数据
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_deleted")  val isDeleted: Boolean = false,
    val notes: String = "",
    @ColumnInfo(name = "created_at")  val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")  val updatedAt: Long = System.currentTimeMillis(),
)
```

---

## 6. 界面原型与交互设计

### 6.1 信息架构

```
App Shell (Scaffold + NavigationBar)
│
├── 🏠 首页 (记录列表)     ← BottomNav Item 0
│   ├── TopAppBar: "氦检漏记录" + 搜索图标 + 筛选图标
│   ├── SearchBar (可展开)
│   ├── FilterChips: [全部] [合格] [不合格] [收藏]
│   ├── LazyColumn (记录卡片列表)
│   │   └── RecordCard
│   │       ├── 报告编号 + 日期
│   │       ├── 产品名称 + 焊缝
│   │       ├── 漏率值 + PASS/FAIL 标签
│   │       └── 收藏星标
│   └── FAB: "+" 新建记录
│
├── 📊 设备管理             ← BottomNav Item 1
│   ├── TopAppBar: "设备台账"
│   ├── TabRow: [检漏仪] [标准漏孔] [温度计] [氦浓度计]
│   ├── 设备卡片 (含有效期状态指示器)
│   └── FAB: "+" 添加设备
│
├── ⚙️ 设置                ← BottomNav Item 2
│   ├── 验收标准
│   ├── 温度修正系数
│   ├── 噪声比
│   ├── 语言
│   ├── 深色模式
│   ├── 数据备份
│   └── 数据恢复
│
└── ℹ️ 帮助                ← BottomNav Item 3
    ├── 公式说明
    ├── 操作指南
    └── 关于
```

### 6.2 关键界面线框图

#### A. 首页 — 记录列表

```
┌─────────────────────────────────┐
│  氦检漏记录          🔍  🔽     │  ← TopAppBar
├─────────────────────────────────┤
│  [全部] [合格] [不合格] [收藏]    │  ← FilterChips
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ HD-LT26-0016    2026.03.12 │ │
│ │ 阀门箱B1-C04  SWB108       │ │
│ │ Q = 9.50e-14    ✅ 合格     │ │  ← RecordCard
│ │ 安全裕度: ~1052倍 (30 dB)   │ │
│ │                     ⭐     │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ HD-LT26-0017    2026.03.15 │ │
│ │ 阀门箱B2-C05  SWB112       │ │
│ │ Q = 2.30e-09    ❌ 不合格   │ │
│ │                     ☆     │ │
│ └─────────────────────────────┘ │
│                                 │
│                          [＋]   │  ← FAB
└─────────────────────────────────┘
```

#### B. 新建/编辑记录 — 参数输入

```
┌─────────────────────────────────┐
│  ← 新建检测记录            💾   │
├─────────────────────────────────┤
│ ▸ 项目信息                  ▼   │  ← 可折叠 Section
│   报告编号: [HD-LT26-____]      │
│   合同号:   [SX300250223  ]     │
│   产品名称: [阀门箱B1-C04  ]     │
│   焊缝名称: [SWB108        ]     │
│   ...                          │
├─────────────────────────────────┤
│ ▸ 标准漏孔参数              ▼   │
│   Q0 尾数:  [3.88 ] 指数: [-9▼]│  ← 尾数+指数分两栏
│   标漏检定温度: [20.0  ] °C     │
├─────────────────────────────────┤
│ ▸ 检漏仪参数                ▼   │
│   I0 尾数:  [1    ] 指数: [-13▼]│
│   In 实测值:[      ] (可选) Pa  │  ← 可选字段用虚线边框
│   ...                          │
├─────────────────────────────────┤
│ ▸ 系统参数                  ▼   │
│   ...                          │
├─────────────────────────────────┤
│  验收标准: [1.0e-10] Pa·m³/s    │
│  温度系数: [0.03   ] /°C       │
├─────────────────────────────────┤
│  ┌──────────────────────────┐   │
│  │       🧮  计  算         │   │  ← 主 CTA 按钮
│  └──────────────────────────┘   │
└─────────────────────────────────┘
```

#### C. 计算结果 — 底部弹出卡片

```
┌─────────────────────────────────┐
│       (参数表单背景半透明遮罩)     │
│                                 │
│ ┌─ 计算结果 ──────────────────┐ │
│ │                             │ │
│ │     ┌──────────────┐        │ │
│ │     │   ✅ 合格     │        │ │  ← 判定标签 (大)
│ │     └──────────────┘        │ │
│ │                             │ │
│ │  实测漏率 Q = 9.50×10⁻¹⁴   │ │
│ │            Pa·m³/s          │ │  ← 关键结果 (大字)
│ │                             │ │
│ │  验收标准  < 1.0×10⁻¹⁰      │ │
│ │  安全裕度  ~1052 倍 (30 dB) │ │
│ │                             │ │
│ │  ┌─────────────────────┐    │ │
│ │  │ 详细参数       展开 ▼│    │ │  ← 可折叠
│ │  │ Qt  = 3.1234e-09   │    │ │
│ │  │ Qmin= 1.0011e-14   │    │ │
│ │  │ Qeim= 9.4081e-15   │    │ │
│ │  │ In  = 1.0000e-14   │    │ │
│ │  │ Mn  = 1.0000e-14   │    │ │
│ │  └─────────────────────┘    │ │
│ │                             │ │
│ │  [保存] [导出Markdown] [分享] │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

#### D. 记录详情 — 完整报告视图

```
┌─────────────────────────────────┐
│  ← HD-LT26-0016      ✏️  🔽   │
├─────────────────────────────────┤
│  ┌─────────────────────────┐    │
│  │  ✅ 合格    1052倍裕度   │    │  ← Hero 区域
│  │  Q = 9.50×10⁻¹⁴ Pa·m³/s│    │
│  └─────────────────────────┘    │
│                                 │
│  ▸ 项目信息                     │
│  ▸ 标准漏孔参数                  │
│  ▸ 检漏仪校准 (含噪声来源标识)    │
│  ▸ 系统校准与测试                │
│  ▸ 实测漏率与判定                │
│  ▸ 验证警告 (0条)               │
│                                 │
│  [编辑] [导出MD] [导出JSON] [删除]│
└─────────────────────────────────┘
```

### 6.3 交互规范

| 交互 | 实现方式 |
|------|---------|
| 表单折叠 | `AnimatedVisibility` + 旋转箭头图标 |
| 尾数/指数输入 | 两栏并排的 `OutlinedTextField`，指数用 `ExposedDropdownMenuBox` (常用值: -8~-15) |
| 实时校验 | 尾数必须 > 0，指数必选，温度 0~50°C |
| 计算结果展示 | `ModalBottomSheet` 从底部弹出，点击遮罩关闭，结果不丢失 |
| 删除确认 | `AlertDialog` 二次确认 + Snackbar 撤销 (5s) |
| 下拉刷新 | `pullToRefresh` 刷新记录列表 |
| 空状态 | Lottie 动画 + "暂无检测记录" + "新建第一条" 按钮 |
| 加载状态 | 骨架屏 (Shimmer) 替代空白卡片 |

### 6.4 Material Design 3 主题

```kotlin
// 工业应用配色方案
private val IndustrialLightColorScheme = lightColorScheme(
    primary            = Color(0xFF1565C0),  // 工业蓝
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFD1E4FF),
    secondary          = Color(0xFF2E7D32),  // 合格绿
    tertiary           = Color(0xFFC62828),  // 不合格红
    error              = Color(0xFFBA1A1A),
    surface            = Color(0xFFF8F9FF),
    surfaceVariant     = Color(0xFFE0E2EC),
    outline            = Color(0xFF74777F),
)

// 关键语义色
object HeliumColors {
    val Pass = Color(0xFF2E7D32)       // 合格绿色
    val Fail = Color(0xFFC62828)       // 不合格红色
    val Warning = Color(0xFFE65100)    // 警告橙色
    val ManualInput = Color(0xFF92D050) // 人工输入标识色 (与Excel一致)
    val Overridable = Color(0xFF00B050) // 可覆盖公式标识色
}
```

---

## 7. 项目架构设计

### 7.1 分层架构图

```
┌──────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │ Screens  │ │Components│ │  Theme   │ │  Navigation  │ │
│  │ RecordList│ │RecordCard│ │Colors   │ │  NavHost     │ │
│  │ RecordEdit│ │ParamInput│ │Typography│ │  Routes     │ │
│  │ ResultView│ │ResultCard│ │Shapes   │ │  Args       │ │
│  │ Equipment │ │EquipCard │ │         │ │             │ │
│  │ Settings  │ │          │ │         │ │             │ │
│  └─────┬────┘ └──────────┘ └──────────┘ └─────────────┘ │
│        │ State (StateFlow) / Events                       │
├────────┼─────────────────────────────────────────────────┤
│        │            ViewModel Layer                       │
│  ┌─────┴──────────────────────────────────────────────┐  │
│  │  RecordListVM    RecordEditVM    ResultVM          │  │
│  │  EquipmentVM     SettingsVM                         │  │
│  │  - UiState (data class)                            │  │
│  │  - Event (sealed class)                             │  │
│  │  - StateFlow<UiState>                              │  │
│  └─────┬──────────────────────────────────────────────┘  │
│        │                                                 │
├────────┼─────────────────────────────────────────────────┤
│        │            Domain Layer (纯 Kotlin)              │
│  ┌─────┴──────────────────────────────────────────────┐  │
│  │  UseCases (每个用例一个类, @Inject constructor)      │  │
│  │                                                     │  │
│  │  CalculateHeliumLeakUseCase                         │  │
│  │    ├── computeQt()        ← Python [1]              │  │
│  │    ├── computeEffectiveNoise() ← Python [2][4]      │  │
│  │    ├── computeQmin()      ← Python [3]              │  │
│  │    ├── computeQeim()      ← Python [5]              │  │
│  │    ├── computeQ()         ← Python [6]              │  │
│  │    └── validate()         ← 7 validation checks     │  │
│  │                                                     │  │
│  │  SaveRecordUseCase                                  │  │
│  │  LoadRecordUseCase                                  │  │
│  │  ExportMarkdownUseCase                              │  │
│  │  ExportJsonUseCase                                  │  │
│  │  ExportCsvUseCase                                   │  │
│  │  ManageEquipmentUseCase                             │  │
│  │  CheckCalibrationStatusUseCase                      │  │
│  │                                                     │  │
│  │  Domain Models (data class)                         │  │
│  │  TestInput / TestResult / CalculationTemplate       │  │
│  │  Equipment / Reminder                               │  │
│  └─────┬──────────────────────────────────────────────┘  │
│        │                                                 │
├────────┼─────────────────────────────────────────────────┤
│        │            Data Layer                            │
│  ┌─────┴──────────────────────────────────────────────┐  │
│  │  Repositories (interface + impl)                    │  │
│  │  RecordRepository                                  │  │
│  │  EquipmentRepository                               │  │
│  │  SettingsRepository (DataStore)                    │  │
│  │                                                     │  │
│  │  Data Sources                                       │  │
│  │  - RecordDao (Room)                                 │  │
│  │  - EquipmentDao (Room)                             │  │
│  │  - FtsDao (全文搜索)                                │  │
│  │  - FileExporter (JSON/CSV/MD)                      │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 7.2 包结构

```
com.aerosun.heliumleakdetector/
│
├── HeliumApp.kt                    // Application + Hilt
├── MainActivity.kt                 // 单 Activity, Compose 宿主
│
├── core/
│   ├── di/                         // Hilt Modules
│   │   ├── DatabaseModule.kt
│   │   ├── RepositoryModule.kt
│   │   └── UseCaseModule.kt
│   ├── extensions/                 // Kotlin 扩展函数
│   └── util/                       // 工具类
│       ├── ScientificNotation.kt   // 科学计数法转换
│       ├── DateFormatter.kt
│       └── ShareUtil.kt
│
├── domain/
│   ├── model/                      // 领域模型 (纯Kotlin, 零Android依赖)
│   │   ├── TestInput.kt
│   │   ├── TestResult.kt
│   │   ├── Equipment.kt
│   │   └── CalculationTemplate.kt
│   ├── usecase/
│   │   ├── CalculateHeliumLeakUseCase.kt
│   │   ├── SaveRecordUseCase.kt
│   │   ├── LoadRecordsUseCase.kt
│   │   ├── ExportMarkdownUseCase.kt
│   │   ├── ExportJsonUseCase.kt
│   │   ├── ExportCsvUseCase.kt
│   │   └── ManageEquipmentUseCase.kt
│   └── repository/
│       ├── RecordRepository.kt     // interface
│       ├── EquipmentRepository.kt  // interface
│       └── SettingsRepository.kt   // interface
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── RecordDao.kt
│   │   │   ├── EquipmentDao.kt
│   │   │   └── FtsDao.kt
│   │   ├── entity/
│   │   │   ├── DetectionRecordEntity.kt
│   │   │   ├── EquipmentEntity.kt
│   │   │   ├── RecordEquipmentCrossRef.kt
│   │   │   └── ReminderEntity.kt
│   │   ├── converter/
│   │   │   └── Converters.kt       // TypeConverters for Room
│   │   └── HeliumDatabase.kt       // @Database
│   ├── repository/
│   │   ├── RecordRepositoryImpl.kt
│   │   ├── EquipmentRepositoryImpl.kt
│   │   └── SettingsRepositoryImpl.kt
│   └── mapper/
│       ├── RecordMappers.kt        // Entity ↔ Domain
│       └── EquipmentMappers.kt
│
├── ui/
│   ├── navigation/
│   │   ├── HeliumNavHost.kt
│   │   ├── Routes.kt               // sealed class
│   │   └── BottomNavBar.kt
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Shape.kt
│   ├── record/
│   │   ├── list/
│   │   │   ├── RecordListScreen.kt
│   │   │   ├── RecordListViewModel.kt
│   │   │   └── components/
│   │   │       ├── RecordCard.kt
│   │   │       ├── SearchBar.kt
│   │   │       └── FilterChips.kt
│   │   ├── edit/
│   │   │   ├── RecordEditScreen.kt
│   │   │   ├── RecordEditViewModel.kt
│   │   │   └── components/
│   │   │       ├── ParamSection.kt       // 可折叠分区
│   │   │       ├── MantissaExponentRow.kt
│   │   │       └── OptionalNoiseField.kt
│   │   └── detail/
│   │       ├── RecordDetailScreen.kt
│   │       ├── RecordDetailViewModel.kt
│   │       └── components/
│   │           ├── ResultHero.kt
│   │           ├── ParamTable.kt
│   │           └── WarningSection.kt
│   ├── result/
│   │   ├── ResultSheet.kt           // ModalBottomSheet
│   │   └── ResultViewModel.kt
│   ├── equipment/
│   │   ├── EquipmentScreen.kt
│   │   └── EquipmentViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── help/
│       └── HelpScreen.kt
│
└── (test)/
    ├── domain/usecase/
    │   └── CalculateHeliumLeakUseCaseTest.kt  ← 关键！与Python交叉验证
    ├── data/repository/
    └── ui/record/
```

### 7.3 计算引擎用例（核心代码结构）

```kotlin
// domain/usecase/CalculateHeliumLeakUseCase.kt
// 100% 复现 Python helium_leak_test.py 全部计算逻辑

class CalculateHeliumLeakUseCase @Inject constructor() {

    companion object {
        const val TEMP_COEFFICIENT = 0.03
        const val NOISE_RATIO = 10
    }

    /**
     * 执行完整计算流程
     * 对应 Python: HeliumLeakTest.run()
     */
    operator fun invoke(input: TestInput): TestResult {
        val q0 = input.q0Mantissa * 10.0.pow(input.q0Exponent)
        val i0 = input.i0Mantissa * 10.0.pow(input.i0Exponent)
        val i  = input.iMantissa  * 10.0.pow(input.iExponent)
        val m0 = input.m0Mantissa * 10.0.pow(input.m0Exponent)
        val m1 = input.m1Mantissa * 10.0.pow(input.m1Exponent)
        val m2 = input.m2Mantissa * 10.0.pow(input.m2Exponent)

        // [1] 温度修正
        val qt = computeQt(q0, input.tCal, input.temperature, input.tempCoefficient)

        // [2][4] 噪声判定 (max规则)
        val inEff = computeEffectiveNoise(i0, input.inMeasured)
        val mnEff = computeEffectiveNoise(m0, input.mnMeasured)

        // [3][5] 灵敏度
        val qmin = computeQmin(inEff, qt, i, i0)
        val qeim = computeQeim(mnEff, qt, m1, m0)

        // [6] 实测漏率
        val q = computeQ(qt, m2, m0, m1, input.tgPercent)

        // 验收判定
        val acceptable = q > 0 && q < input.acceptanceLimit

        // 7项合理性验证
        val warnings = validate(input, qt, i0, i, inEff, m0, m1, m2, mnEff, q)

        return TestResult(
            q0 = q0, qt = qt, i0 = i0, inEff = inEff, i = i, qmin = qmin,
            m0 = m0, mnEff = mnEff, m1 = m1, qeim = qeim, m2 = m2,
            qMeasured = q, isAcceptable = acceptable, warnings = warnings
        )
    }

    // ====== 6大公式 (直接映射Python) ======

    fun computeQt(q0: Double, tCal: Double, tTest: Double,
                  coeff: Double = TEMP_COEFFICIENT): Double {
        return q0 * (1 - (tCal - tTest) * coeff)
    }

    fun computeEffectiveNoise(baseSignal: Double,
                              measured: Double?,
                              ratio: Int = NOISE_RATIO): Double {
        val default = baseSignal / ratio
        return measured?.let { maxOf(it, default) } ?: default
    }

    fun computeQmin(noise: Double, qt: Double, i: Double, i0: Double): Double {
        val denom = i - i0
        require(denom > 0) {
            "分母 (I - I0) = $denom <= 0，无法计算 Qmin"
        }
        return noise * qt / denom
    }

    fun computeQeim(noise: Double, qt: Double, m1: Double, m0: Double): Double {
        val denom = m1 - m0
        require(denom > 0) {
            "分母 (M1 - M0) = $denom <= 0，无法计算 Qeim"
        }
        return noise * qt / denom
    }

    fun computeQ(qt: Double, m2: Double, m0: Double, m1: Double,
                 tgPercent: Double = 100.0): Double {
        val deltaM = m2 - m0
        val calFactor = qt / (m1 - m0)
        val concCorrection = if (tgPercent > 0) 100.0 / tgPercent else 1.0
        return deltaM * calFactor * concCorrection
    }

    // ====== 7项验证 ======
    private fun validate(input: TestInput, qt: Double, i0: Double, i: Double,
                         inEff: Double, m0: Double, m1: Double, m2: Double,
                         mnEff: Double, q: Double): List<String> {
        val warnings = mutableListOf<String>()
        // 1~7 与 Python _validate() 完全一致
        // ...
        return warnings
    }
}
```

### 7.4 ViewModel 示例

```kotlin
// ui/record/edit/RecordEditViewModel.kt
@HiltViewModel
class RecordEditViewModel @Inject constructor(
    private val calculateUseCase: CalculateHeliumLeakUseCase,
    private val saveRecordUseCase: SaveRecordUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // UI 状态
    data class EditUiState(
        val input: TestInput = TestInput(),
        val result: TestResult? = null,
        val validationErrors: Map<String, String> = emptyMap(),
        val isCalculating: Boolean = false,
        val isSaving: Boolean = false,
        val showResult: Boolean = false,
        val snackbarMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    // 事件 (一次性)
    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events: Flow<EditEvent> = _events.receiveAsFlow()

    sealed class EditEvent {
        data class NavigateBack(val recordId: Long) : EditEvent()
        data class ShowError(val message: String) : EditEvent()
    }

    // --- 用户操作 ---

    fun onInputChanged(updated: TestInput) {
        _uiState.update { it.copy(input = updated, result = null, showResult = false) }
    }

    fun onCalculate() {
        val input = _uiState.value.input
        // 表单校验
        val errors = validateForm(input)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }
            try {
                val result = calculateUseCase(input)  // ← 核心计算
                _uiState.update {
                    it.copy(
                        result = result,
                        isCalculating = false,
                        showResult = true,
                        validationErrors = emptyMap()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCalculating = false) }
                _events.send(EditEvent.ShowError(e.message ?: "计算失败"))
            }
        }
    }

    fun onSave() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val id = saveRecordUseCase(state.input, state.result!!)
            _uiState.update { it.copy(isSaving = false) }
            _events.send(EditEvent.NavigateBack(id))
        }
    }

    private fun validateForm(input: TestInput): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (input.q0Mantissa <= 0) errors["Q0_mantissa"] = "尾数必须 > 0"
        if (input.temperature !in 0.0..50.0) errors["temperature"] = "温度应在 0~50°C"
        // ... 更多校验
        return errors
    }
}
```

---

## 8. 开发阶段与时间规划

### 8.1 总体时间线 (11 周)

```
Week 1    Week 2    Week 3    Week 4    Week 5    Week 6    Week 7    Week 8    Week 9    Week 10   Week 11
├─────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┤
│ 阶段0    │  阶段1: MVP 开发 (4周)                         │ 阶段2: V1.1 (2周)    │ 阶段3: 测试&发布(2周)│
│ 项目初始化│                                                │                      │                      │
└─────────┴────────────────────────────────────────────────┴──────────────────────┴──────────────────────┘
```

### 8.2 详细阶段划分

#### 阶段 0：项目初始化 (3天)

| 任务 | 产出 | 估时 |
|------|------|------|
| Android 项目脚手架 (Gradle + Version Catalog) | 可编译的空 APK | 0.5d |
| Hilt + Room + Navigation 集成配置 | DI/DB/路由可用 | 0.5d |
| Material 3 主题 + 工业配色方案 | Theme.kt / Color.kt | 0.5d |
| 包结构创建 + 基础架构类 | 完整目录树 | 0.5d |
| CI 流水线 (GitHub Actions: lint + test) | `.github/workflows/` | 0.5d |
| Python 交叉验证测试框架搭建 | 数据驱动测试基类 | 0.5d |

#### 阶段 1：MVP 开发 (4 周)

**Sprint 1.1 — 计算引擎 (1 周)**

| 任务 | 关键产出 |
|------|---------|
| Domain 模型: `TestInput`, `TestResult` | 与 Python dataclass 1:1 映射 |
| `CalculateHeliumLeakUseCase` (6 公式 + 7 验证) | 100% 覆盖 Python 计算逻辑 |
| `ScientificNotation` 工具类 | mantissa/exponent ↔ value 互转 |
| **单元测试**: 使用 Python 已有数据驱动, 30+ test cases | 与 Python 输出逐位比对, 误差 < 1e-15 |
| 噪声覆盖规则专项测试 | 4 种场景全覆盖 |

**Sprint 1.2 — 数据层 (1 周)**

| 任务 | 关键产出 |
|------|---------|
| Room Database + Entity + DAO | 3 张表 + 索引 + FTS |
| `RecordRepository` + `EquipmentRepository` | CRUD + Flow 查询 |
| `SettingsRepository` (DataStore) | 验收标准/温度系数持久化 |
| Mapper: Entity ↔ Domain | 双向映射 + 冗余计算字段 |

**Sprint 1.3 — 记录 CRUD UI (1 周)**

| 任务 | 关键产出 |
|------|---------|
| `RecordListScreen` + `RecordListVM` | 列表 + 筛选 + 搜索 |
| `RecordEditScreen` + `RecordEditVM` | 参数表单 + 校验 + 折叠分区 |
| `ResultSheet` (ModalBottomSheet) | 计算结果展示 |
| `RecordDetailScreen` | 完整报告视图 |
| 导航: Routes + NavHost | 页面间参数传递 |

**Sprint 1.4 — 导出 + 打磨 (1 周)**

| 任务 | 关键产出 |
|------|---------|
| Markdown 导出 (与 Python `to_markdown()` 输出一致) | `ExportMarkdownUseCase` |
| JSON / CSV 导出 | `ExportJsonUseCase` / `ExportCsvUseCase` |
| 分享 (Android Sharesheet) | `ShareUtil` |
| 空状态 / 加载态 / 错误态 | 完整 UI 状态覆盖 |
| 首次用户引导 (3 步引导) | Onboarding 屏幕 |

#### 阶段 2：V1.1 增强 (2 周)

| Sprint | 任务 |
|--------|------|
| 2.1 (1周) | 设备管理 CRUD + 校准有效期状态指示器 + 到期通知 |
| 2.2 (1周) | 批量操作 + 数据备份/恢复 (ZIP) + 深色模式 + 英文国际化 |

#### 阶段 3：测试与发布 (2 周)

| 任务 | 产出 |
|------|------|
| 计算精度回归测试 (Python 30+ cases) | 全 Pass |
| UI 自动化测试 (Compose Testing) | 关键流程覆盖 |
| 兼容性测试 (Android 8~15, 5 款真机 + 模拟器) | 兼容性报告 |
| 性能测试 (5000 条记录列表帧率 > 55fps) | 性能报告 |
| 安全审查 (数据存储加密) | 安全报告 |
| 签名打包 (Release APK) | `helium-leak-detector-v1.0.0.apk` |
| 应用商店上架准备 (华为/小米/Google Play) | 商店素材 |

---

## 9. 测试方案与质量保证

### 9.1 测试金字塔

```
            ╱─────╲
           ╱  E2E  ╲          5%  - Compose UI Test (关键流程)
          ╱─────────╲
         ╱ Integration╲      15%  - DAO + Repository + UseCase 集成
        ╱───────────────╲
       ╱   Unit Tests    ╲    80%  - 计算引擎为核心
      ╱───────────────────╲
```

### 9.2 计算引擎测试（最高优先级）

```kotlin
// domain/usecase/CalculateHeliumLeakUseCaseTest.kt
class CalculateHeliumLeakUseCaseTest {

    private lateinit var useCase: CalculateHeliumLeakUseCase

    @BeforeEach
    fun setUp() { useCase = CalculateHeliumLeakUseCase() }

    // ====== 与 Python 交叉验证 (逐值比对) ======

    @Test
    fun `HD-LT26-0016 - full calculation matches Python output`() {
        val input = TestInput(
            reportNo = "HD-LT26-0016",
            q0Mantissa = 3.88, q0Exponent = -9, tCal = 20.0,
            temperature = 13.5,
            i0Mantissa = 1.0, i0Exponent = -13,
            iMantissa = 3.12, iExponent = -9,
            m0Mantissa = 1.0, m0Exponent = -13,
            m1Mantissa = 3.32, m1Exponent = -9,
            m2Mantissa = 2.01, m2Exponent = -13,
            tgPercent = 100.0,
        )

        val result = useCase(input)

        // Python 输出: Qt=3.1234e-09, Qmin=1.0011e-14, Qeim=9.4081e-15, Q=9.5022e-14
        assertEquals(3.1234e-09, result.qt, 1e-15)
        assertEquals(1.0011e-14, result.qmin, 1e-15)
        assertEquals(9.4081e-15, result.qeim, 1e-15)
        assertEquals(9.5022e-14, result.qMeasured, 1e-15)
        assertTrue(result.isAcceptable)
        assertTrue(result.warnings.isEmpty())
    }

    // ====== 噪声覆盖规则 ======

    @Test
    fun `noise - no measured value uses default`() {
        val inEff = useCase.computeEffectiveNoise(1e-13, null)
        assertEquals(1e-14, inEff, 1e-20)
    }

    @Test
    fun `noise - measured > default, use measured`() {
        val inEff = useCase.computeEffectiveNoise(1e-13, 5e-14)
        assertEquals(5e-14, inEff, 1e-20)
    }

    @Test
    fun `noise - measured < default, use default`() {
        val inEff = useCase.computeEffectiveNoise(1e-13, 5e-15)
        assertEquals(1e-14, inEff, 1e-20)
    }

    // ====== Qmin 分母 <= 0 应抛出异常 ======

    @Test
    fun `Qmin throws when I <= I0`() {
        assertThrows<IllegalArgumentException> {
            useCase.computeQmin(1e-14, 3e-9, 1e-13, 2e-13) // I < I0
        }
    }

    // ====== 温度对比数据驱动测试 ======

    @ParameterizedTest
    @CsvSource(
        "10.0, 2.7160e-09",
        "15.0, 3.2980e-09",
        "20.0, 3.8800e-09",
        "25.0, 4.4620e-09",
        "30.0, 5.0440e-09",
    )
    fun `Qt temperature correction matches Python`(temp: Double, expectedQt: Double) {
        val qt = useCase.computeQt(3.88e-9, 20.0, temp)
        assertEquals(expectedQt, qt, 1e-15)
    }
}
```

### 9.3 Room DAO 测试

```kotlin
// data/local/dao/RecordDaoTest.kt
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class RecordDaoTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var db: HeliumDatabase
    private lateinit var dao: RecordDao

    @Before
    fun setup() {
        hiltRule.inject()
        dao = db.recordDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertAndQuery() = runTest {
        val record = createTestRecord()
        val id = dao.insert(record)
        val loaded = dao.getById(id)
        assertEquals(record.reportNo, loaded?.reportNo)
    }

    @Test
    fun searchByReportNo() = runTest { /* FTS 测试 */ }

    @Test
    fun softDelete() = runTest {
        val id = dao.insert(createTestRecord())
        dao.softDelete(id)
        val deleted = dao.getById(id)
        assertNull(deleted) // 查询自动过滤 is_deleted=1
    }
}
```

### 9.4 兼容性测试矩阵

| 设备 | Android 版本 | 屏幕尺寸 | 测试重点 |
|------|-------------|---------|---------|
| Pixel 6a | 15 | 6.1" | 基准设备 |
| Samsung Galaxy S21 | 14 | 6.2" | OneUI 兼容 |
| Xiaomi 13 | 14 | 6.36" | MIUI 权限 |
| Samsung Galaxy Tab S8 | 13 | 11" | 平板适配 |
| 模拟器 (低配) | 8.0 | 5.0" | 低版本兼容 |

---

## 10. 打包发布与维护

### 10.1 签名与构建

```bash
# 生成签名密钥
keytool -genkey -v -keystore helium-release.keystore \
  -alias helium -keyalg RSA -keysize 2048 -validity 10000

# 在 gradle.properties 中配置签名 (CI 环境变量注入)
RELEASE_STORE_FILE=helium-release.keystore
RELEASE_STORE_PASSWORD=${HELIUM_STORE_PASSWORD}
RELEASE_KEY_ALIAS=helium
RELEASE_KEY_PASSWORD=${HELIUM_KEY_PASSWORD}

# 构建 Release APK
./gradlew assembleRelease

# 输出: app/build/outputs/apk/release/helium-leak-detector-v1.0.0.apk
```

### 10.2 发布渠道

| 渠道 | 优先级 | 备注 |
|------|--------|------|
| 企业内部分发 (APK 直装) | P0 | 工业应用核心分发方式 |
| 华为应用市场 | P1 | 国内工业企业常用 |
| 小米应用商店 | P2 | |
| Google Play | P2 | 国际版 (英文) |

### 10.3 版本规划

| 版本 | 内容 | 时间 |
|------|------|------|
| v1.0.0 | MVP (计算 + 记录管理 + 导出) | Week 7 |
| v1.1.0 | 设备管理 + 批量操作 + 备份恢复 | Week 9 |
| v1.2.0 | 多语言 + 深色模式 + 打印 | Week 11 |
| v2.0.0 | 账号系统 + 云同步 (Firebase) | TBD |

### 10.4 维护策略

- **Bug 修复**: 关键 Bug 24h 内 hotfix 发布
- **公式更新**: 当检测标准或温度修正系数变更时，更新 `CalculateHeliumLeakUseCase` 并同步更新 Python 参考实现
- **Android 版本适配**: 每年 Q3 适配最新 Android 大版本
- **用户反馈渠道**: 应用内"问题反馈" → 邮件 → GitHub Issues

---

## 11. 附录：Python 计算逻辑映射表

| Python (`helium_leak_test.py`)                     | Kotlin (`CalculateHeliumLeakUseCase.kt`)             | 验证状态      |
| -------------------------------------------------- | ---------------------------------------------------- | --------- |
| `TestInput` dataclass                              | `TestInput` data class                               | 字段 1:1 对应 |
| `TestResult` dataclass                             | `TestResult` data class                              | 字段 1:1 对应 |
| `compute_Qt(Q0, T_cal, T_test, coeff)`             | `computeQt(q0, tCal, tTest, coeff)`                  | 公式一致      |
| `compute_effective_noise(signal, measured, ratio)` | `computeEffectiveNoise(baseSignal, measured, ratio)` | 公式一致      |
| `compute_Qmin(noise, Qt, I, I0)`                   | `computeQmin(noise, qt, i, i0)`                      | 公式一致      |
| `compute_Qeim(noise, Qt, M1, M0)`                  | `computeQeim(noise, qt, m1, m0)`                     | 公式一致      |
| `compute_Q(Qt, M2, M0, M1, TG%)`                   | `computeQ(qt, m2, m0, m1, tgPercent)`                | 公式一致      |
| `_validate()` (7 checks)                           | `validate()` (7 checks)                              | 逻辑一致      |
| `to_markdown()`                                    | `ExportMarkdownUseCase`                              | 输出格式一致    |
| `TEMP_COEFFICIENT = 0.03`                          | `const val TEMP_COEFFICIENT = 0.03`                  | 常量一致      |
| `NOISE_RATIO = 10`                                 | `const val NOISE_RATIO = 10`                         | 常量一致      |
| `ACCEPTANCE_LIMIT_DEFAULT = 1.0e-10`               | `acceptanceLimit = 1.0e-10`                          | 默认一致      |

---

> **文档状态**: 待评审
> **下一步**: 确认技术选型 → 搭建项目脚手架 → 启动 Sprint 1.1
>
> **核心原则**: 计算逻辑零偏差、工业场景可用性优先、代码质量>开发速度
