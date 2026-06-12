package com.aerosun.heliumleakdetector.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户偏好设置。
 *
 * MVP 阶段使用内存状态，后续可迁移到 DataStore Preferences 持久化。
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultAcceptanceLimit: Double = 1.0e-10,
    val defaultTempCoefficient: Double = 0.03,
    val defaultExportFormat: ExportFormat = ExportFormat.PDF,
)

enum class ThemeMode(val label: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }
enum class ExportFormat(val label: String) { PDF("PDF"), JSON("JSON"), CSV("CSV") }

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateThemeMode(mode: ThemeMode) {
        _settings.update { it.copy(themeMode = mode) }
    }

    fun updateAcceptanceLimit(value: Double) {
        if (value > 0) _settings.update { it.copy(defaultAcceptanceLimit = value) }
    }

    fun updateTempCoefficient(value: Double) {
        _settings.update { it.copy(defaultTempCoefficient = value) }
    }

    fun updateExportFormat(format: ExportFormat) {
        _settings.update { it.copy(defaultExportFormat = format) }
    }
}
