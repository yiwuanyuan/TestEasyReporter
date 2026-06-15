package com.aerosun.heliumleakdetector.ui.record.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import android.app.DatePickerDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.ui.record.edit.components.MantissaExponentRow
import com.aerosun.heliumleakdetector.ui.result.ResultSheet

// Excel 参数分类配色
private val ColorManualInput = Color(0xFF92D050)      // 人工输入 — 亮绿
private val ColorOverridable = Color(0xFF00B050)       // 可覆盖公式 — 深绿
private val ColorAutoCalc = Color(0xFFFFFFFF)          // 自动计算 — 白

/**
 * 新建/编辑检测记录页面 (优化版)。
 *
 * 视觉改进：
 *   - 分区卡片 + 左侧色条（对应 Excel 参数分类）
 *   - 分段图标和编号
 *   - 底部固定计算按钮
 *   - 必填字段 * 标记
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(
    recordId: Long?,
    onNavigateBack: () -> Unit,
    onOpenEquipmentSelector: (Set<Long>) -> Unit = {},
    onEquipmentResult: Set<Long>? = null,
    viewModel: RecordEditViewModel = hiltViewModel(),
) {
    LaunchedEffect(recordId) { viewModel.initialize(recordId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val isEdit = recordId != null
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 处理 EquipmentSelector 返回的设备选择结果
    if (onEquipmentResult != null) {
        LaunchedEffect(onEquipmentResult) {
            viewModel.onEquipmentSelected(onEquipmentResult)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecordEditViewModel.EditEvent.NavigateBack -> onNavigateBack()
                is RecordEditViewModel.EditEvent.ShowError -> { /* Snackbar */ }
                is RecordEditViewModel.EditEvent.OpenEquipmentSelector -> onOpenEquipmentSelector(event.currentIds)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑检测记录" else "新建检测记录",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            // 底部固定计算按钮
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { viewModel.onCalculate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(52.dp),
                    enabled = !state.isCalculating,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (state.isCalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state.isCalculating) "计算中..." else "开始计算",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            val input = state.input
            val onUpdate: (TestInput) -> Unit = { viewModel.onInputChanged(it) }

            // ====== 1. 项目信息 (人工输入 — 亮绿) ======
            SectionCard(
                number = "1", title = "项目信息",
                icon = Icons.Default.CheckCircle,
                accentColor = ColorManualInput,
                subtitle = "报告基本信息与试验环境",
            ) {
                RequiredField(label = "报告编号") {
                    OutlinedTextField(
                        value = input.reportNo, onValueChange = { onUpdate(input.copy(reportNo = it)) },
                        label = { Text("报告编号") }, modifier = Modifier.fillMaxWidth(),
                        isError = state.validationErrors.containsKey("reportNo"),
                        supportingText = state.validationErrors["reportNo"]?.let { { Text(it) } },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = input.contractNo, onValueChange = { onUpdate(input.copy(contractNo = it)) },
                    label = { Text("合同号") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = input.testDate, onValueChange = { onUpdate(input.copy(testDate = it)) },
                    label = { Text("试验日期") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("yyyy.MM.dd") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance()
                            val parts = input.testDate.split(".")
                            if (parts.size == 3) {
                                cal.set(parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR),
                                        (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1,
                                        parts[2].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH))
                            }
                            DatePickerDialog(context, { _, y, m, d ->
                                onUpdate(input.copy(testDate = "%04d.%02d.%02d".format(y, m + 1, d)))
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        }) {
                            Icon(Icons.Default.DateRange, "选择日期")
                        }
                    },
                )
                OutlinedTextField(
                    value = input.productName, onValueChange = { onUpdate(input.copy(productName = it)) },
                    label = { Text("产品名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = input.productSerialNo, onValueChange = { onUpdate(input.copy(productSerialNo = it)) },
                    label = { Text("出厂编号（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = input.weldName, onValueChange = { onUpdate(input.copy(weldName = it)) },
                        label = { Text("焊缝名称") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                    OutlinedTextField(
                        value = input.inspectionArea, onValueChange = { onUpdate(input.copy(inspectionArea = it)) },
                        label = { Text("检测区域") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.tempText,
                        onValueChange = { viewModel.onTextChanged("temperature", it) },
                        label = { Text("温度 (°C)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.validationErrors.containsKey("temperature"),
                        prefix = { Text("🌡 ") },
                    )
                    OutlinedTextField(
                        value = state.humidityText,
                        onValueChange = { viewModel.onTextChanged("humidity", it) },
                        label = { Text("湿度 (%)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("💧 ") },
                    )
                }
            }

            // ====== 2. 标准漏孔 (人工输入 — 亮绿) ======
            SectionCard(
                number = "2", title = "标准漏孔参数",
                icon = Icons.Default.Build,
                accentColor = ColorManualInput,
            ) {
                MantissaExponentRow(
                    label = "标定漏率 Q0",
                    mantissa = input.q0Mantissa, exponent = input.q0Exponent,
                    onMantissaChange = { onUpdate(input.copy(q0Mantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(q0Exponent = it)) },
                    isError = state.validationErrors.containsKey("q0Mantissa"),
                )
                OutlinedTextField(
                    value = input.tCal.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { t -> onUpdate(input.copy(tCal = t)) } },
                    label = { Text("标漏检定温度 (°C)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            // ====== 3. 检漏仪参数 ======
            SectionCard(
                number = "3", title = "检漏仪参数",
                icon = Icons.Default.Star,
                accentColor = ColorManualInput,
            ) {
                MantissaExponentRow(
                    label = "仪器本底 I0",
                    mantissa = input.i0Mantissa, exponent = input.i0Exponent,
                    onMantissaChange = { onUpdate(input.copy(i0Mantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(i0Exponent = it)) },
                    isError = state.validationErrors.containsKey("i0Mantissa"),
                )
                MantissaExponentRow(
                    label = "校准读数 I",
                    mantissa = input.iMantissa, exponent = input.iExponent,
                    onMantissaChange = { onUpdate(input.copy(iMantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(iExponent = it)) },
                    isError = state.validationErrors.containsKey("iMantissa"),
                )
                MantissaExponentRow(
                    label = "本底噪声 In",
                    mantissa = input.inMeasuredMantissa, exponent = input.inMeasuredExponent,
                    onMantissaChange = { onUpdate(input.copy(inMeasuredMantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(inMeasuredExponent = it)) },
                )
            }

            // ====== 4. 系统参数 ======
            SectionCard(
                number = "4", title = "系统参数",
                icon = Icons.Default.Build,
                accentColor = ColorManualInput,
            ) {
                MantissaExponentRow(
                    label = "系统本底 M0",
                    mantissa = input.m0Mantissa, exponent = input.m0Exponent,
                    onMantissaChange = { onUpdate(input.copy(m0Mantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(m0Exponent = it)) },
                    isError = state.validationErrors.containsKey("m0Mantissa"),
                )
                MantissaExponentRow(
                    label = "校准读数 M1",
                    mantissa = input.m1Mantissa, exponent = input.m1Exponent,
                    onMantissaChange = { onUpdate(input.copy(m1Mantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(m1Exponent = it)) },
                    isError = state.validationErrors.containsKey("m1Mantissa"),
                )
                MantissaExponentRow(
                    label = "喷氦后读数 M2",
                    mantissa = input.m2Mantissa, exponent = input.m2Exponent,
                    onMantissaChange = { onUpdate(input.copy(m2Mantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(m2Exponent = it)) },
                    isError = state.validationErrors.containsKey("m2Mantissa"),
                )
                MantissaExponentRow(
                    label = "本底噪声 Mn",
                    mantissa = input.mnMeasuredMantissa, exponent = input.mnMeasuredExponent,
                    onMantissaChange = { onUpdate(input.copy(mnMeasuredMantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(mnMeasuredExponent = it)) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.tResponseText,
                        onValueChange = { viewModel.onTextChanged("tResponse", it) },
                        label = { Text("反应时间 (s)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = state.tgPercentText,
                        onValueChange = { viewModel.onTextChanged("tgPercent", it) },
                        label = { Text("氦浓度 TG%") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.validationErrors.containsKey("tgPercent"),
                    )
                }
            }

            // ====== 5. 验收标准 ======
            SectionCard(
                number = "5", title = "验收标准与系数",
                icon = Icons.Default.Info,
                accentColor = ColorOverridable,
                subtitle = "可调整的判定阈值和修正系数",
            ) {
                MantissaExponentRow(
                    label = "验收限值",
                    mantissa = input.acceptanceLimitMantissa, exponent = input.acceptanceLimitExponent,
                    onMantissaChange = { onUpdate(input.copy(acceptanceLimitMantissa = it)) },
                    onExponentChange = { onUpdate(input.copy(acceptanceLimitExponent = it)) },
                )
                OutlinedTextField(
                    value = input.tempCoefficient.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { c -> onUpdate(input.copy(tempCoefficient = c)) } },
                    label = { Text("温度修正系数") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(" /°C") },
                )
            }

            // 底部间距
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // === 计算结果底部弹出 ===
    if (state.showResult && state.result != null) {
        ResultSheet(
            input = state.input,
            result = state.result!!,
            onDismiss = { viewModel.onDismissResult() },
            onSave = { viewModel.onSave() },
            isSaving = state.isSaving,
            selectedEquipment = state.selectedEquipment,
            onEquipmentClick = { viewModel.onRequestEquipmentSelection() },
        )
    }
}

// ====== 美化组件 ======

/**
 * 分区卡片 — 左侧色条 + 图标 + 编号。
 * 色条颜色对应 Excel 参数分类：
 *   亮绿 #92D050 → 人工输入参数
 *   深绿 #00B050 → 可覆盖公式参数
 */
@Composable
private fun SectionCard(
    number: String,
    title: String,
    icon: ImageVector,
    accentColor: Color,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左侧色条
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(accentColor),
            )
            // 内容区
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                // 标题行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 编号圆
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(number, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge, color = accentColor)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 42.dp))
                }
                Spacer(Modifier.height(12.dp))
                // 字段区
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content()
                }
            }
        }
    }
}

/** 必填字段包装 — 在 label 前加红色 * */
@Composable
private fun RequiredField(label: String, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("* ", color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        content()
    }
}
