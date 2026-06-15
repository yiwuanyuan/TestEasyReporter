package com.aerosun.heliumleakdetector.ui.record.list

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import java.text.SimpleDateFormat
import java.util.*

private val monthFmt = SimpleDateFormat("yyyy年M月", Locale.CHINA)
private val dateFmt = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    onCreateClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
    viewModel: RecordListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCalendar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.selectedDate != null) Text("📅 ${state.selectedDate}",
                        style = MaterialTheme.typography.titleMedium)
                    else Text("检测记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    IconButton(onClick = { showCalendar = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "日历",
                            tint = if (state.selectedDate != null) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.onSearchToggled(!state.isSearchActive) }) {
                        Icon(if (state.isSearchActive) Icons.Default.Close else Icons.Default.Search, "搜索")
                    }
                    if (state.selectedDate != null) {
                        TextButton(onClick = { viewModel.onDateSelected(null) }) {
                            Text("清除", style = MaterialTheme.typography.labelLarge) }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick, shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Icon(Icons.Default.Add, contentDescription = "新建记录") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 搜索栏
            AnimatedVisibility(visible = state.isSearchActive,
                enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.searchQuery, onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("搜索报告编号 / 合同号 / 产品 / 焊缝...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                    )
                }
            }

            // 筛选标签
            FilterChipsRow(current = state.filter, onFilterClick = viewModel::onFilterChanged)

            // 列表
            Crossfade(targetState = state.isLoading) { loading ->
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator() }
                    state.records.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(if (state.selectedDate != null) "该日期暂无检测记录" else "暂无检测记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.records, key = { it.id }) { record ->
                                SwipeToDeleteCard(record = record,
                                    onClick = { onRecordClick(record.id) },
                                    onToggleFavorite = { viewModel.onToggleFavorite(record.id, record.isFavorite) },
                                    onDelete = { viewModel.onDeleteRecord(record.id) })
                            }
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }

    // 日历 ModalBottomSheet
    if (showCalendar) {
        CalendarSheet(
            datesWithRecords = state.datesWithRecords,
            selectedDate = state.selectedDate,
            onDateSelected = { date -> viewModel.onDateSelected(date); showCalendar = false },
            onDismiss = { showCalendar = false },
        )
    }
}

// ============================
// 自定义日历
// ============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarSheet(
    datesWithRecords: Set<String>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("选择日期", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // 月份导航
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    val c = Calendar.getInstance(); c.time = currentMonth.time
                    c.add(Calendar.MONTH, -1); currentMonth = c
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, "上月")
                }
                Text(
                    monthFmt.format(currentMonth.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = {
                    val c = Calendar.getInstance(); c.time = currentMonth.time
                    c.add(Calendar.MONTH, 1); currentMonth = c
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, "下月")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 星期行
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(4.dp))

            // 日期网格
            val cal = Calendar.getInstance()
            cal.time = currentMonth.time
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // 构建网格: 前导空白 + 日期数字
            val gridItems = mutableListOf<Any>()
            repeat(firstDayOfWeek) { gridItems.add("") }  // 空白
            for (d in 1..daysInMonth) {
                cal.set(Calendar.DAY_OF_MONTH, d)
                val dateStr = dateFmt.format(cal.time)
                val hasRecord = dateStr in datesWithRecords
                gridItems.add(DayCell(d, dateStr, hasRecord))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                userScrollEnabled = false,
            ) {
                items(gridItems) { item ->
                    when (item) {
                        is String -> Box(modifier = Modifier.aspectRatio(1f))
                        is DayCell -> {
                            val isToday = item.dateStr == dateFmt.format(Calendar.getInstance().time)
                            val isSelected = item.dateStr == selectedDate
                            val hasRecord = item.hasRecord

                            Box(
                                modifier = Modifier.aspectRatio(1f).padding(2.dp).then(
                                    if (hasRecord) Modifier.clickable { onDateSelected(item.dateStr) }
                                    else Modifier
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${item.day}",
                                            color = when {
                                                !hasRecord -> MaterialTheme.colorScheme.outlineVariant
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (hasRecord) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 底部统计
            val monthCount = datesWithRecords.count { it.startsWith(
                "%04d.%02d".format(currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1)) }
            Text("本月 ${monthCount} 天有检测记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            Button(onClick = onDismiss, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text("关闭") }
        }
    }
}

private data class DayCell(val day: Int, val dateStr: String, val hasRecord: Boolean)

// ============================
// 左滑删除
// ============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteCard(
    record: DetectionRecordEntity, onClick: () -> Unit,
    onToggleFavorite: () -> Unit, onDelete: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { showConfirm = true; false } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(vertical = 2.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 24.dp)) }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        RecordCard(record = record, onClick = onClick, onToggleFavorite = onToggleFavorite)
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除报告「${record.reportNo}」吗？") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) {
                Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } },
        )
    }
}

// ============================
// 筛选标签
// ============================

@Composable
private fun FilterChipsRow(current: RecordListViewModel.Filter, onFilterClick: (RecordListViewModel.Filter) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(selectedTabIndex = RecordListViewModel.Filter.entries.indexOf(current),
            modifier = Modifier.fillMaxWidth(), edgePadding = 16.dp, divider = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            RecordListViewModel.Filter.entries.forEach { filter ->
                val label = when (filter) {
                    RecordListViewModel.Filter.ALL -> "全部"
                    RecordListViewModel.Filter.PASS -> "合格"
                    RecordListViewModel.Filter.FAIL -> "不合格"
                    RecordListViewModel.Filter.FAVORITES -> "收藏"
                }
                FilterChip(selected = current == filter, onClick = { onFilterClick(filter) },
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

// ============================
// 记录卡片
// ============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordCard(
    record: DetectionRecordEntity, onClick: () -> Unit, onToggleFavorite: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 0.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${record.reportNo}  ${record.testDate}",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${record.productName} · ${record.weldName}",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Q = ${"%.2e".format(record.qMeasured)} Pa·m³/s",
                        style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    val (bg, tc, lbl) = if (record.isAcceptable)
                        Triple(MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer, "合格")
                    else Triple(MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer, "不合格")
                    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                        Text(lbl, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tc) }
                }
            }
            // 收藏按钮 — 缩小给文本更多空间
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Text(if (record.isFavorite) "★" else "☆", style = MaterialTheme.typography.titleMedium,
                    color = if (record.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onToggleFavorite() })
            }
        }
    }
}
