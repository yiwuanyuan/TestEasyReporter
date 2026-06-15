package com.aerosun.heliumleakdetector.ui.result

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult
import com.aerosun.heliumleakdetector.ui.theme.FailRed
import com.aerosun.heliumleakdetector.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

/**
 * 计算结果底部弹出卡片（含试验设备关联）。
 *
 * 功能: 判定展示、设备选择入口、设备表格、导出保存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    input: TestInput,
    result: TestResult,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean = false,
    selectedEquipment: List<EquipmentEntity> = emptyList(),
    onEquipmentClick: (() -> Unit)? = null,
) {
    var showDetails by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── 判定标签 ──
            val vColor = if (result.isAcceptable)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer
            val vBg = if (result.isAcceptable)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
            val vText = if (result.isAcceptable) "合 格" else "不 合 格"

            Surface(shape = RoundedCornerShape(20.dp), color = vBg, modifier = Modifier.padding(top = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)) {
                    Text(vText, style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, color = vColor,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Text(if (result.isAcceptable) "PASS" else "FAIL",
                        style = MaterialTheme.typography.labelMedium, color = vColor.copy(alpha = 0.7f))
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 漏率 ──
            Text("实测漏率", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("%.4e".format(result.qMeasured), style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Monospace, color = vColor)
            Text("Pa·m³/s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // ── 安全裕度 ──
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("验收标准  < %.1e Pa·m³/s".format(input.acceptanceLimitValue()),
                        style = MaterialTheme.typography.bodyMedium)
                    if (result.qMeasured > 0) {
                        val margin = input.acceptanceLimitValue() / result.qMeasured
                        val db = 10.0 * kotlin.math.log10(margin)
                        Spacer(Modifier.height(4.dp))
                        Text("安全裕度  约 ${margin.toLong()} 倍  (${db.toInt()} dB)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 详细参数 ──
            TextButton(onClick = { showDetails = !showDetails }, shape = RoundedCornerShape(12.dp)) {
                Text(if (showDetails) "收起详细参数 ▲" else "展开详细参数 ▼",
                    style = MaterialTheme.typography.labelLarge)
            }
            AnimatedVisibility(visible = showDetails, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                DetailTable(input, result)
            }

            // ── 警告 ──
            if (result.warnings.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("⚠ 验证警告 (${result.warnings.size} 条)",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelLarge)
                        result.warnings.forEach { w ->
                            Text("· $w", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 试验设备 ──
            if (onEquipmentClick != null) {
                OutlinedButton(
                    onClick = onEquipmentClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (selectedEquipment.isEmpty()) "+ 记录测试设备"
                        else "修改测试设备 (${selectedEquipment.size} 台)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // 已选设备表格
                if (selectedEquipment.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("试验设备", fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            selectedEquipment.forEach { eq ->
                                val now = System.currentTimeMillis()
                                val expired = eq.calibrationDueDate in 1 until now
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${eq.name} ${eq.model}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f))
                                    Text(
                                        if (expired && eq.calibrationDueDate > 0) "⚠ 已过期"
                                        else if (eq.calibrationDueDate > 0) dateFmt.format(Date(eq.calibrationDueDate))
                                        else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (expired) FailRed else WarningOrange,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 操作按钮 ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)) { Text("返回修改") }
                Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp)) {
                    if (isSaving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp)) }
                    Text("保存记录")
                }
            }
        }
    }
}

@Composable
private fun DetailTable(input: TestInput, r: TestResult) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            DetailRow("Q0 标准漏孔标定漏率", "%.3e Pa·m³/s".format(r.q0))
            DetailRow("Qt 试验温度下标准漏率", "%.4e Pa·m³/s".format(r.qt))
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow("I0 仪器本底", "%.3e Pa·m³/s".format(r.i0))
            DetailRow("In 本底噪声", "%.3e [${r.inSource}]".format(r.inValue))
            DetailRow("I  校准读数", "%.3e Pa·m³/s".format(r.i))
            DetailRow("Qmin 仪器最小可检漏率", "%.4e Pa·m³/s".format(r.qmin))
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow("M0 系统本底", "%.3e Pa·m³/s".format(r.m0))
            DetailRow("Mn 本底噪声", "%.3e [${r.mnSource}]".format(r.mnValue))
            DetailRow("M1 系统校准读数", "%.3e Pa·m³/s".format(r.m1))
            DetailRow("Qeim 系统最小可检漏率", "%.4e Pa·m³/s".format(r.qeim))
            DetailRow("M2 喷氦后读数", "%.3e Pa·m³/s".format(r.m2))
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow("反应时间 / 氦浓度", "${input.tResponse} s / ${input.tgPercent}%")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
