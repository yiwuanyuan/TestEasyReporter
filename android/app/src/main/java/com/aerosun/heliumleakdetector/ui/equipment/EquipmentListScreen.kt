package com.aerosun.heliumleakdetector.ui.equipment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentListScreen(
    onCreateClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: EquipmentListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设备管理") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "添加设备")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 筛选标签
            FilterRow(current = state.filter, onSelect = viewModel::setFilter)

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.equipment.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无设备记录", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.equipment, key = { it.id }) { equipment ->
                        EquipmentCard(
                            equipment = equipment,
                            onEdit = { onEditClick(equipment.id) },
                            onDelete = { viewModel.delete(equipment.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(current: String, onSelect: (String) -> Unit) {
    val filters = listOf(
        "all" to "全部", "active" to "有效", "expiring" to "即将到期", "expired" to "已过期"
    )
    ScrollableTabRow(selectedTabIndex = filters.indexOfFirst { it.first == current }.coerceAtLeast(0),
        modifier = Modifier.fillMaxWidth(), edgePadding = 16.dp, divider = {}) {
        filters.forEachIndexed { _, (key, label) ->
            FilterChip(selected = current == key, onClick = { onSelect(key) },
                label = { Text(label) }, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentCard(equipment: EquipmentEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val now = System.currentTimeMillis()
    val daysLeft = if (equipment.calibrationDueDate > 0)
        ((equipment.calibrationDueDate - now) / (24 * 3600 * 1000)).toInt() else Int.MAX_VALUE

    // 使用 MaterialTheme 语义色容器
    val (chipBg, chipLabel, chipColor) = when {
        equipment.status == "expired" || daysLeft < 0 ->
            Triple(MaterialTheme.colorScheme.errorContainer, "已过期", MaterialTheme.colorScheme.onErrorContainer)
        daysLeft in 0..7 ->
            Triple(MaterialTheme.colorScheme.tertiaryContainer, "⚠ ${daysLeft}天后到期", MaterialTheme.colorScheme.onTertiaryContainer)
        daysLeft in 8..30 ->
            Triple(MaterialTheme.colorScheme.secondaryContainer, "${daysLeft}天后到期", MaterialTheme.colorScheme.onSecondaryContainer)
        else ->
            Triple(MaterialTheme.colorScheme.secondaryContainer, "有效", MaterialTheme.colorScheme.onSecondaryContainer)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${equipment.name}  ${equipment.model}",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("编号: ${equipment.serialNo} ｜ 类型: ${equipment.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (equipment.calibrationDueDate > 0) {
                        Text("校准有效期: ${dateFmt.format(Date(equipment.calibrationDueDate))}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(8.dp), color = chipBg) {
                        Text(chipLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = chipColor, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除设备「${equipment.name}」吗？") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } },
        )
    }
}
