package com.aerosun.heliumleakdetector.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 氦检漏计算器 — Material Design 3 工业配色体系。
 *
 * 设计原则：
 *   - 使用 M3 Surface Container 色阶区分层级，避免生硬分割线
 *   - 语义色低饱和柔和呈现，状态信息不刺眼
 *   - 品牌色选用深邃工业蓝，传递专业可靠感
 */

// ═══════════════════════════════════════════
// 品牌色 (Brand)
// ═══════════════════════════════════════════
val BrandBlue = Color(0xFF1A56DB)              // 主色 — 工业蓝
val BrandBlueLight = Color(0xFF5E8AF7)         // 浅变体
val BrandBlueDark = Color(0xFF003A9E)          // 深变体
val BrandBlueContainer = Color(0xFFDBE4FF)     // 容器色 — 淡淡的蓝底
val OnBrandBlueContainer = Color(0xFF001A41)   // 容器上文字

// ═══════════════════════════════════════════
// 语义色 (Semantic) — 低饱和，不刺眼
// ═══════════════════════════════════════════

// 合格 (Pass)
val PassGreen = Color(0xFF2E7D32)
val PassGreenContainer = Color(0xFFE8F5E9)
val OnPassGreenContainer = Color(0xFF1B5E20)

// 不合格 / 危险 (Fail)
val FailRed = Color(0xFFC62828)
val FailRedContainer = Color(0xFFFFEBEE)
val OnFailRedContainer = Color(0xFFB71C1C)

// 警告 / 即将到期 (Warning)
val WarningOrange = Color(0xFFE65100)
val WarningOrangeContainer = Color(0xFFFFF3E0)
val OnWarningOrangeContainer = Color(0xFFBF360C)

// ═══════════════════════════════════════════
// Excel 参数分类标识色 (保留原策划方案)
// ═══════════════════════════════════════════
val ManualInputGreen = Color(0xFF92D050)       // 人工输入 — 对应 Excel 亮绿底
val OverridableGreen = Color(0xFF00B050)       // 可覆盖公式 — 对应 Excel 深绿底

// ═══════════════════════════════════════════
// M3 Surface Container 色阶 (Light & Dark)
// ═══════════════════════════════════════════

// Light
val SurfaceLight = Color(0xFFF8F9FC)
val SurfaceContainerLowLight = Color(0xFFF0F2F7)
val SurfaceContainerLight = Color(0xFFE8EAF0)
val SurfaceContainerHighLight = Color(0xFFE0E2E8)
val OutlineLight = Color(0xFF74777F)
val OutlineVariantLight = Color(0xFFC4C6D0)
val OnSurfaceLight = Color(0xFF1A1C1E)
val OnSurfaceVariantLight = Color(0xFF44474E)

// Dark
val SurfaceDark = Color(0xFF111318)
val SurfaceContainerLowDark = Color(0xFF1A1D24)
val SurfaceContainerDark = Color(0xFF22252D)
val SurfaceContainerHighDark = Color(0xFF2A2D36)
val OutlineDark = Color(0xFF8E9099)
val OutlineVariantDark = Color(0xFF44474F)
val OnSurfaceDark = Color(0xFFE2E2E9)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
