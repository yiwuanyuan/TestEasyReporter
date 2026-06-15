package com.aerosun.heliumleakdetector.ui.equipment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.aerosun.heliumleakdetector.ui.theme.FailRed
import com.aerosun.heliumleakdetector.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

/**
 * 试验设备选择页面（选项卡）。
 * 可从数据库中选择检漏仪、标准漏孔、温度计、氦浓度计等设备。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentSelectorScreen(
    preselectedIds: Set<Long> = emptySet(),
    onNavigateBack: () -> Unit,
    onSave: (Set<Long>) -> Unit,
    viewModel: EquipmentSelectorViewModel = hiltViewModel(),
) {
    LaunchedEffect(preselectedIds) { viewModel.setPreselectedIds(preselectedIds) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择试验设备", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "已选择 ${state.selectedIds.size} 台设备",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("取消") }
                        Button(
                            onClick = { onSave(state.selectedIds) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("保存设备选择") }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 选项卡
            ScrollableTabRow(
                selectedTabIndex = viewModel.tabs.indexOf(state.currentTab).coerceAtLeast(0),
                edgePadding = 16.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                viewModel.tabs.forEach { tab ->
                    Tab(
                        selected = state.currentTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab) },
                    )
                }
            }

            // 设备列表
            val filtered = state.equipment.filter { it.type == state.currentTab || (state.currentTab == "其他" && it.type !in listOf("检漏仪", "标准漏孔", "温度计", "氦浓度计")) }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("该类型暂无设备", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("请先在「设备管理」中添加", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { eq ->
                        EquipmentSelectCard(
                            equipment = eq,
                            isSelected = state.selectedIds.contains(eq.id),
                            onToggle = { viewModel.toggleSelection(eq.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentSelectCard(
    equipment: EquipmentEntity,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val expired = equipment.calibrationDueDate in 1 until now
    val expiring = equipment.calibrationDueDate > now &&
        equipment.calibrationDueDate <= now + 30L * 24 * 3600 * 1000

    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${equipment.name}  ${equipment.model}",
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text("编号: ${equipment.serialNo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (equipment.calibrationDueDate > 0) {
                    Text(
                        "有效期: ${dateFmt.format(Date(equipment.calibrationDueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (expired) FailRed else if (expiring) WarningOrange
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // 过期警告
            if (expired) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text("已过期", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}
