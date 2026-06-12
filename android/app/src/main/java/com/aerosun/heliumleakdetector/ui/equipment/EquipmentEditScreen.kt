package com.aerosun.heliumleakdetector.ui.equipment

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.ui.record.edit.components.ParamSection
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private val DEVICE_TYPES = listOf("检漏仪", "标准漏孔", "温度计", "氦浓度计", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentEditScreen(
    equipmentId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: EquipmentEditViewModel = hiltViewModel(),
) {
    LaunchedEffect(equipmentId) { viewModel.load(equipmentId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var typeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (equipmentId != null) "编辑设备" else "添加设备") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(err, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                }
            }

            ParamSection("基本信息") {
                OutlinedTextField(value = state.name, onValueChange = { v -> viewModel.updateField { s -> s.copy(name = v) } },
                    label = { Text("设备名称 *") }, modifier = Modifier.fillMaxWidth())
            }

            ParamSection("设备类型") {
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = state.type, onValueChange = {},
                        readOnly = true, label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        DEVICE_TYPES.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = {
                                viewModel.updateField { s -> s.copy(type = t) }; typeExpanded = false
                            })
                        }
                    }
                }
            }

            ParamSection("型号与编号") {
                OutlinedTextField(value = state.model, onValueChange = { v -> viewModel.updateField { s -> s.copy(model = v) } },
                    label = { Text("型号") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.serialNo, onValueChange = { v -> viewModel.updateField { s -> s.copy(serialNo = v) } },
                    label = { Text("编号") }, modifier = Modifier.fillMaxWidth())
            }

            ParamSection("校准有效期") {
                val dateText = if (state.calibrationDueDate > 0) dateFmt.format(Date(state.calibrationDueDate)) else ""
                OutlinedTextField(
                    value = dateText, onValueChange = {}, readOnly = true,
                    label = { Text("校准有效期至") }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance()
                            if (state.calibrationDueDate > 0) cal.timeInMillis = state.calibrationDueDate
                            DatePickerDialog(context, { _, y, m, d ->
                                cal.set(y, m, d)
                                viewModel.updateField { it.copy(calibrationDueDate = cal.timeInMillis) }
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        }) {
                            Icon(Icons.Default.DateRange, "选择日期")
                        }
                    },
                )
            }

            ParamSection("备注") {
                OutlinedTextField(value = state.notes, onValueChange = { v -> viewModel.updateField { s -> s.copy(notes = v) } },
                    label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 3)
            }

            Button(
                onClick = { viewModel.save(onNavigateBack) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("保存设备", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
