package com.aerosun.heliumleakdetector.ui.record.detail

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.data.export.DocxReportGenerator
import com.aerosun.heliumleakdetector.data.export.PdfReportGenerator
import com.aerosun.heliumleakdetector.data.export.ReportExporter
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.aerosun.heliumleakdetector.ui.record.edit.components.ParamSection
import com.aerosun.heliumleakdetector.ui.theme.FailRed
import com.aerosun.heliumleakdetector.ui.theme.WarningOrange

/**
 * 记录详情页（工业级重构版）— 工业仪表盘 Telemetry 风格。
 *
 * 关键数值使用等宽大号字体，判定使用语义色容器，参数区卡片化。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    recordId: Long,
    onNavigateBack: () -> Unit,
    onOpenEquipmentSelector: (Set<Long>) -> Unit = {},
    viewModel: RecordDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(recordId) { viewModel.loadRecord(recordId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    // 监听从 EquipmentSelector 返回的设备选择结果
    // 使用 HeliumNavHost 传递的 savedStateHandle 结果
    var refreshKey by remember { mutableStateOf(0L) }
    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) viewModel.refreshEquipment()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 每次页面恢复时检查是否有设备更新标志
                refreshKey = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.entity?.reportNo ?: "记录详情", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.entity != null) {
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(Icons.Default.Share, contentDescription = "导出")
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false },
                            ) {
                                DropdownMenuItem(text = { Text("导出 PDF 报告") }, onClick = {
                                    showExportMenu = false
                                    context.startActivity(Intent.createChooser(
                                        PdfReportGenerator.exportPdf(context, state.entity!!), "导出 PDF"))
                                })
                                DropdownMenuItem(text = { Text("导出 DOCX 文档") }, onClick = {
                                    showExportMenu = false
                                    context.startActivity(Intent.createChooser(
                                        DocxReportGenerator.exportDocx(context, state.entity!!), "导出 DOCX"))
                                })
                                DropdownMenuItem(text = { Text("导出 JSON") }, onClick = {
                                    showExportMenu = false
                                    context.startActivity(Intent.createChooser(
                                        ReportExporter.exportJson(context, state.entity!!), "导出 JSON"))
                                })
                                DropdownMenuItem(text = { Text("导出 CSV") }, onClick = {
                                    showExportMenu = false
                                    context.startActivity(Intent.createChooser(
                                        ReportExporter.exportCsv(context, state.entity!!), "导出 CSV"))
                                })
                            }
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Crossfade(targetState = state.isLoading) { loading ->
            when {
                loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error) }
                state.input != null && state.result != null -> {
                    DetailContent(
                        input = state.input!!,
                        result = state.result!!,
                        equipment = state.equipment,
                        onEquipmentClick = if (onOpenEquipmentSelector != {})
                            { { onOpenEquipmentSelector(viewModel.getSavedEquipmentIds()) } }
                        else null,
                        modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    input: TestInput,
    result: TestResult,
    equipment: List<EquipmentEntity> = emptyList(),
    onEquipmentClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),       // 模块间呼吸感
    ) {
        // ── 判定卡片 — 语义色容器 ──
        VerdictCard(result, input.acceptanceLimitValue())

        // ── 参数区 — 可折叠卡片 ──
        ParamSection(title = "项目信息") {
            DetailRow("报告编号", input.reportNo, mono = false)
            DetailRow("合同号", input.contractNo, mono = false)
            DetailRow("试验日期", input.testDate, mono = false)
            DetailRow("产品名称", input.productName, mono = false)
            DetailRow("焊缝 / 区域", "${input.weldName} · ${input.inspectionArea}", mono = false)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("温度", "${input.temperature} °C", mono = false, mod = Modifier.weight(1f))
                DetailRow("湿度", "${input.humidity}%", mono = false, mod = Modifier.weight(1f))
            }
        }

        ParamSection(title = "标准漏孔参数") {
            DetailRow("Q0 标定漏率", "%.3e Pa·m³/s".format(result.q0))
            DetailRow("Qt 试验温度下", "%.4e Pa·m³/s".format(result.qt))
        }

        ParamSection(title = "检漏仪校准") {
            DetailRow("I0 仪器本底", "%.3e Pa·m³/s".format(result.i0))
            DetailRow("In 本底噪声", "%.3e [${result.inSource}]".format(result.inValue))
            DetailRow("I  校准读数", "%.3e Pa·m³/s".format(result.i))
            DetailRow("Qmin 仪器可检漏率", "%.4e Pa·m³/s".format(result.qmin))
        }

        ParamSection(title = "系统校准与测试") {
            DetailRow("M0 系统本底", "%.3e Pa·m³/s".format(result.m0))
            DetailRow("Mn 本底噪声", "%.3e [${result.mnSource}]".format(result.mnValue))
            DetailRow("M1 系统校准读数", "%.3e Pa·m³/s".format(result.m1))
            DetailRow("Qeim 系统可检漏率", "%.4e Pa·m³/s".format(result.qeim))
            DetailRow("M2 喷氦后读数", "%.3e Pa·m³/s".format(result.m2))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("反应时间", "${input.tResponse} s", mono = false, mod = Modifier.weight(1f))
                DetailRow("氦浓度", "${input.tgPercent}%", mono = false, mod = Modifier.weight(1f))
            }
        }

        // ── 实测漏率 — 工业仪表盘 ──
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("实测漏率", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%.4e Pa·m³/s".format(result.qMeasured),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (result.isAcceptable) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                DetailRow("验收标准", "< %.1e Pa·m³/s".format(input.acceptanceLimitValue()), mono = false)
                if (result.qMeasured > 0) {
                    val m = input.acceptanceLimitValue() / result.qMeasured
                    val db = 10.0 * kotlin.math.log10(m)
                    DetailRow("安全裕度", "约 ${m.toLong()} 倍 (${db.toInt()} dB)", mono = false)
                }
            }
        }

        // ── 警告 — errorContainer ──
        if (result.warnings.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚠ 验证警告 (${result.warnings.size} 条)", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    result.warnings.forEach { w ->
                        Text("· $w", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f))
                    }
                }
            }
        }

        // ── 试验设备 ──
        if (onEquipmentClick != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onEquipmentClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    if (equipment.isEmpty()) "+ 记录测试设备" else "修改测试设备 (${equipment.size} 台)",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (equipment.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("试验设备", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        equipment.forEach { eq ->
                            val now = System.currentTimeMillis()
                            val expired = eq.calibrationDueDate in 1 until now
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text("${eq.name}  ${eq.model}", fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("编号: ${eq.serialNo}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (eq.calibrationDueDate > 0) {
                                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    Text("有效期: ${fmt.format(java.util.Date(eq.calibrationDueDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (expired) FailRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (eq != equipment.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** 判定结果卡片 — 语义色大圆角容器 */
@Composable
private fun VerdictCard(result: TestResult, acceptanceLimit: Double) {
    val (bg, color, text, sub) = if (result.isAcceptable)
        Tuple4(MaterialTheme.colorScheme.secondaryContainer,
               MaterialTheme.colorScheme.onSecondaryContainer, "✓ 合格", "PASS")
    else
        Tuple4(MaterialTheme.colorScheme.errorContainer,
               MaterialTheme.colorScheme.onErrorContainer, "✗ 不合格", "FAIL")

    Surface(shape = RoundedCornerShape(20.dp), color = bg, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(sub, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            Text("实测漏率: %.4e Pa·m³/s".format(result.qMeasured),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace, color = color)
            Text("验收标准: < %.1e Pa·m³/s".format(acceptanceLimit),
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.7f))
        }
    }
}

data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun DetailRow(label: String, value: String, mono: Boolean = true, mod: Modifier = Modifier) {
    Row(modifier = mod.padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (mono) FontWeight.Medium else FontWeight.Normal)
    }
}
