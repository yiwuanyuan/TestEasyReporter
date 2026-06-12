package com.aerosun.heliumleakdetector.ui.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aerosun.heliumleakdetector.ui.record.edit.components.ParamSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帮助") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // === 操作指南 ===
            ParamSection(title = "操作指南") {
                HelpStep("1", "新建记录", "点击首页右下角 + 按钮，输入试验参数")
                HelpStep("2", "填写参数", "按表单依次填写项目信息和测量数据")
                HelpStep("3", "执行计算", "点击底部的「计算」按钮")
                HelpStep("4", "查看结果", "计算结果以卡片弹出，含判定和安全裕度")
                HelpStep("5", "保存记录", "确认无误后保存，可在首页列表检索")
                HelpStep("6", "导出报告", "点击详情页右上角分享图标，可导出 PDF/JSON/CSV")
            }

            // === 计算公式 ===
            ParamSection(title = "计算公式 (6 大公式)") {
                FormulaItem("① 温度修正", "Qt = Q0 × [1 - (T_cal - T_test) × 0.03]", "氦气漏率温度补偿，系数 3%/°C")
                FormulaItem("② 仪器噪声", "In = max(In_measured, I0 / 10)", "保守取大，防止低估噪声")
                FormulaItem("③ 仪器可检漏率", "Qmin = In × Qt / (I - I0)", "信噪比法 — 检漏仪最小可检漏率")
                FormulaItem("④ 系统噪声", "Mn = max(Mn_measured, M0 / 10)", "保守取大")
                FormulaItem("⑤ 系统可检漏率", "Qeim = Mn × Qt / (M1 - M0)", "信噪比法 — 系统最小可检漏率")
                FormulaItem("⑥ 实测漏率", "Q = Qt × (M2 - M0) / (M1 - M0) × 100/TG%", "含氦浓度修正")
            }

            // === 参数说明 ===
            ParamSection(title = "参数符号说明") {
                ParamDesc("Q0", "标准漏孔标定漏率 (Pa·m³/s)")
                ParamDesc("Qt", "试验温度下的标准漏率")
                ParamDesc("I0", "仪器本底读数")
                ParamDesc("In", "仪器本底噪声")
                ParamDesc("I", "仪器校准读数（标漏开启后）")
                ParamDesc("Qmin", "仪器最小可检漏率")
                ParamDesc("M0", "系统本底读数")
                ParamDesc("Mn", "系统本底噪声")
                ParamDesc("M1", "系统校准读数（标漏开启后）")
                ParamDesc("Qeim", "系统最小可检漏率")
                ParamDesc("M2", "喷氦后读数")
                ParamDesc("TG%", "氦浓度百分比 (100% = 纯氦)")
                ParamDesc("T", "反应时间 (s)")
            }

            // === 验收标准说明 ===
            ParamSection(title = "验收判定") {
                Text(
                    text = "实测漏率 Q < 验收限值 → 合格 (PASS)\n" +
                           "实测漏率 Q ≥ 验收限值 → 不合格 (FAIL)\n\n" +
                           "安全裕度 = 验收限值 / 实测漏率\n" +
                           "裕度越大表示密封性能越好。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // === 校准周期建议 ===
            ParamSection(title = "设备校准周期建议") {
                Text(
                    text = "• 检漏仪: 每 12 个月校准\n" +
                           "• 标准漏孔: 每 12 个月校准\n" +
                           "• 温度计: 每 6 个月校准\n" +
                           "• 氦浓度计: 每 12 个月校准\n\n" +
                           "请在设备管理页面记录校准有效期。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HelpStep(num: String, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(num, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FormulaItem(title: String, formula: String, desc: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(formula, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ParamDesc(symbol: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            "$symbol  ",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(60.dp),
        )
        Text(desc, style = MaterialTheme.typography.bodySmall)
    }
}
