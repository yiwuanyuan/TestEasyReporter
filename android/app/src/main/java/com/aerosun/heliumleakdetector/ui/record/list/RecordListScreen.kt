package com.aerosun.heliumleakdetector.ui.record.list

import androidx.compose.animation.*  // animateContentSize, AnimatedVisibility, Crossfade 等
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity

/**
 * 首页 — 检测记录列表（工业级重构版）。
 *
 * 视觉: 大圆角卡片 + Surface 色阶层级 + 等宽数字 + 语义色容器 + 微动效。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    onCreateClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
    viewModel: RecordListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("检测记录", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = { viewModel.onSearchToggled(!state.isSearchActive) }) {
                        Icon(
                            if (state.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                shape = RoundedCornerShape(16.dp),        // 大圆角 FAB
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建记录")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 搜索栏 — 平滑展开
            AnimatedVisibility(
                visible = state.isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("搜索报告编号 / 产品 / 焊缝...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),           // 大圆角输入框
                        singleLine = true,
                    )
                }
            }

            // 筛选标签行
            FilterChipsRow(current = state.filter, onFilterClick = viewModel::onFilterChanged)

            // 内容区
            Crossfade(targetState = state.isLoading) { loading ->     // 加载 → 内容平滑过渡
                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.records.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📋", style = MaterialTheme.typography.displaySmall)
                                Spacer(Modifier.height(8.dp))
                                Text("暂无检测记录", style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),   // 卡片间距呼吸感
                        ) {
                            items(state.records, key = { it.id }) { record ->
                                RecordCard(
                                    record = record,
                                    onClick = { onRecordClick(record.id) },
                                    onToggleFavorite = { viewModel.onToggleFavorite(record.id, record.isFavorite) },
                                    onDelete = { viewModel.onDeleteRecord(record.id) },
                                )
                            }
                            item { Spacer(Modifier.height(72.dp)) }   // FAB 留白
                        }
                    }
                }
            }
        }
    }
}

/** 筛选标签行 — surfaceContainerLow 底色区分于主列表 */
@Composable
private fun FilterChipsRow(
    current: RecordListViewModel.Filter,
    onFilterClick: (RecordListViewModel.Filter) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ScrollableTabRow(
            selectedTabIndex = RecordListViewModel.Filter.entries.indexOf(current),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 16.dp,
            divider = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            RecordListViewModel.Filter.entries.forEachIndexed { _, filter ->
                val label = when (filter) {
                    RecordListViewModel.Filter.ALL -> "全部"
                    RecordListViewModel.Filter.PASS -> "合格"
                    RecordListViewModel.Filter.FAIL -> "不合格"
                    RecordListViewModel.Filter.FAVORITES -> "收藏"
                }
                FilterChip(
                    selected = current == filter,
                    onClick = { onFilterClick(filter) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(20.dp),              // 胶囊形 Chip
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** 记录卡片 — 大圆角 + surfaceContainerLow 层级 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordCard(
    record: DetectionRecordEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDelete by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().animateContentSize(),     // 尺寸变化平滑
        shape = RoundedCornerShape(16.dp),                           // 大圆角
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),                     // 内部 16dp 呼吸感
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 第一行: 报告编号 + 日期
                Text(
                    text = "${record.reportNo}  ${record.testDate}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                // 第二行: 产品名称 + 焊缝
                Text(
                    text = "${record.productName} · ${record.weldName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                // 第三行: 漏率(等宽数字) + 判定容器
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Q = ${"%.2e".format(record.qMeasured)} Pa·m³/s",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,           // 等宽 — 科技感
                    )
                    Spacer(Modifier.width(8.dp))

                    // 判定 — 使用语义色容器，而非纯文字色
                    val (chipBg, chipText, chipLabel) = if (record.isAcceptable)
                        Triple(MaterialTheme.colorScheme.secondaryContainer,
                               MaterialTheme.colorScheme.onSecondaryContainer, "合格")
                    else
                        Triple(MaterialTheme.colorScheme.errorContainer,
                               MaterialTheme.colorScheme.onErrorContainer, "不合格")

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = chipBg,
                    ) {
                        Text(
                            text = chipLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = chipText,
                        )
                    }
                }
            }
            // 收藏
            IconButton(onClick = onToggleFavorite) {
                Text(
                    if (record.isFavorite) "★" else "☆",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (record.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
