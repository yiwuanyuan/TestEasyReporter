package com.aerosun.heliumleakdetector.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 氦检漏计算器 — Material Design 3 主题 (工业级重构版)。
 *
 * 使用 M3 Surface Container 完整色阶：
 *   surface → surfaceContainerLow → surfaceContainer → surfaceContainerHigh
 *   逐级加深，用色彩而非分割线区分内容层级。
 */

// ═══════════════════════════════════════════
// Light Color Scheme
// ═══════════════════════════════════════════
private val LightScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = OnBrandBlueContainer,

    secondary = PassGreen,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = PassGreenContainer,
    onSecondaryContainer = OnPassGreenContainer,

    tertiary = BrandBlueLight,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = BrandBlueContainer,
    onTertiaryContainer = OnBrandBlueContainer,

    error = FailRed,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = FailRedContainer,
    onErrorContainer = OnFailRedContainer,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,

    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,

    inverseSurface = OnSurfaceLight,
    inverseOnSurface = SurfaceLight,
)

// ═══════════════════════════════════════════
// Dark Color Scheme
// ═══════════════════════════════════════════
private val DarkScheme = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = BrandBlueDark,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandBlueContainer,

    secondary = androidx.compose.ui.graphics.Color(0xFF81C784),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF003300),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF1B5E20),
    onSecondaryContainer = PassGreenContainer,

    tertiary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF001A41),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF003C8F),
    onTertiaryContainer = BrandBlueContainer,

    error = androidx.compose.ui.graphics.Color(0xFFEF9A9A),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = FailRedContainer,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,

    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,

    inverseSurface = OnSurfaceDark,
    inverseOnSurface = SurfaceDark,
)

// ═══════════════════════════════════════════
// Theme Composable
// ═══════════════════════════════════════════
@Composable
fun HeliumLeakDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HeliumTypography,
        content = content,
    )
}
