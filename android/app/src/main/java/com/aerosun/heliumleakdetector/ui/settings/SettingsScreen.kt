package com.aerosun.heliumleakdetector.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.ui.record.edit.components.ParamSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ParamSection(title = "外观") {
                ThemeMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.updateThemeMode(mode) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = settings.themeMode == mode, onClick = { viewModel.updateThemeMode(mode) })
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            ParamSection(title = "默认计算参数") {
                OutlinedTextField(
                    value = settings.defaultAcceptanceLimit.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateAcceptanceLimit(v) } },
                    label = { Text("默认验收限值") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    suffix = { Text("Pa·m³/s") }, singleLine = true,
                )
                OutlinedTextField(
                    value = settings.defaultTempCoefficient.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateTempCoefficient(v) } },
                    label = { Text("温度修正系数") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    suffix = { Text("/°C") }, singleLine = true,
                )
            }

            ParamSection(title = "导出默认格式") {
                ExportFormat.entries.forEach { fmt ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.updateExportFormat(fmt) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = settings.defaultExportFormat == fmt, onClick = { viewModel.updateExportFormat(fmt) })
                        Spacer(Modifier.width(8.dp))
                        Text(fmt.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            ParamSection(title = "关于") {
                ListItem(headlineContent = { Text("应用版本") }, supportingContent = { Text("V1.0.0") })
                ListItem(headlineContent = { Text("技术栈") }, supportingContent = { Text("Kotlin 2.1 + Compose + Room + Hilt") })
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
