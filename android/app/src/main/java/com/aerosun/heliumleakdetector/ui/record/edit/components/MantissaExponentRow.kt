package com.aerosun.heliumleakdetector.ui.record.edit.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * 科学计数法输入组件：尾数 + 指数 分两栏输入。
 *
 * 尾数输入框 + 指数下拉框（常用值: -8 ~ -15），紧凑并排布局。
 *
 * @param label             参数名称（如 "标定漏率 Q0"）
 * @param mantissa          当前尾数值
 * @param exponent          当前指数值
 * @param onMantissaChange  尾数变更回调
 * @param onExponentChange  指数变更回调
 * @param isError           是否显示错误状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MantissaExponentRow(
    label: String,
    mantissa: Double,
    exponent: Int,
    onMantissaChange: (Double) -> Unit,
    onExponentChange: (Int) -> Unit,
    isError: Boolean = false,
) {
    val commonExponents = (-15..-8).toList()
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 尾数输入
        OutlinedTextField(
            value = if (mantissa == 0.0) "" else mantissa.toString(),
            onValueChange = { onMantissaChange(it.toDoubleOrNull() ?: mantissa) },
            label = { Text("$label 尾数") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = isError,
            singleLine = true,
        )

        // 指数下拉
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(130.dp),
        ) {
            OutlinedTextField(
                value = "10^$exponent",
                onValueChange = {},
                readOnly = true,
                label = { Text("指数") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                commonExponents.forEach { exp ->
                    DropdownMenuItem(
                        text = { Text("10^$exp") },
                        onClick = {
                            onExponentChange(exp)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
