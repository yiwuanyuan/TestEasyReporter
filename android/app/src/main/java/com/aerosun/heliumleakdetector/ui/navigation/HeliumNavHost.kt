package com.aerosun.heliumleakdetector.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aerosun.heliumleakdetector.ui.equipment.EquipmentEditScreen
import com.aerosun.heliumleakdetector.ui.equipment.EquipmentListScreen
import com.aerosun.heliumleakdetector.ui.equipment.EquipmentSelectorScreen
import com.aerosun.heliumleakdetector.ui.help.HelpScreen
import com.aerosun.heliumleakdetector.ui.record.detail.RecordDetailScreen
import com.aerosun.heliumleakdetector.ui.record.edit.RecordEditScreen
import com.aerosun.heliumleakdetector.ui.record.list.RecordListScreen
import com.aerosun.heliumleakdetector.ui.settings.SettingsScreen

/**
 * 应用主导航宿主。
 *
 * 底部导航栏包含 4 个一级入口：
 *   记录 → 设备 → 设置 → 帮助
 *
 * 二级页面（新建、编辑、详情）不显示底部导航栏。
 */

/** 底部导航栏项定义 */
private data class BottomNavItem(
    val label: String,
    val icon: @Composable () -> Unit,
    val route: Routes,
)

private val bottomNavItems = listOf(
    BottomNavItem("记录", { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "记录") }, Routes.RecordList),
    BottomNavItem("设备", { Icon(Icons.Default.Build, contentDescription = "设备") }, Routes.EquipmentList),
    BottomNavItem("设置", { Icon(Icons.Default.Settings, contentDescription = "设置") }, Routes.Settings),
    BottomNavItem("帮助", { Icon(Icons.Default.Info, contentDescription = "帮助") }, Routes.Help),
)

/** 占位页面（功能开发中） */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "$title\n\n功能开发中...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun HeliumNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    // 设备选择结果缓存（避免 savedStateHandle 时序问题）
    val pendingEquipmentResult = remember { mutableStateOf<Set<Long>?>(null) }

    // 判断是否显示底部导航栏（仅一级页面显示）
    val showBottomBar = bottomNavItems.any { item ->
        currentDest?.hasRoute(item.route::class) == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDest?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.RecordList,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            // 首页 — 记录列表
            composable<Routes.RecordList> {
                RecordListScreen(
                    onCreateClick = { navController.navigate(Routes.RecordCreate) },
                    onRecordClick = { id -> navController.navigate(Routes.RecordDetail(id)) },
                )
            }

            // 新建记录
            composable<Routes.RecordCreate> {
                val result = pendingEquipmentResult.value
                pendingEquipmentResult.value = null  // 消费后立即清空

                RecordEditScreen(
                    recordId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenEquipmentSelector = { _ ->
                        navController.navigate(Routes.EquipmentSelector()) {
                            launchSingleTop = true
                        }
                    },
                    onEquipmentResult = result,
                )
            }

            // 编辑记录
            composable<Routes.RecordEdit> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.RecordEdit>()
                val result = pendingEquipmentResult.value
                pendingEquipmentResult.value = null

                RecordEditScreen(
                    recordId = route.recordId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenEquipmentSelector = { _ ->
                        navController.navigate(Routes.EquipmentSelector(fromRecordId = route.recordId)) {
                            launchSingleTop = true
                        }
                    },
                    onEquipmentResult = result,
                )
            }

            // 记录详情
            composable<Routes.RecordDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.RecordDetail>()
                RecordDetailScreen(
                    recordId = route.recordId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenEquipmentSelector = { _ ->
                        navController.navigate(Routes.EquipmentSelector(fromRecordId = route.recordId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            // 设备管理
            composable<Routes.EquipmentList> {
                EquipmentListScreen(
                    onCreateClick = { navController.navigate(Routes.EquipmentCreate) },
                    onEditClick = { id -> navController.navigate(Routes.EquipmentEdit(id)) },
                )
            }

            // 添加设备
            composable<Routes.EquipmentCreate> {
                EquipmentEditScreen(
                    equipmentId = null,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // 编辑设备
            composable<Routes.EquipmentEdit> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.EquipmentEdit>()
                EquipmentEditScreen(
                    equipmentId = route.equipmentId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // 试验设备选择器
            composable<Routes.EquipmentSelector> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.EquipmentSelector>()
                EquipmentSelectorScreen(
                    preselectedIds = emptySet(),
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { ids ->
                        // 如果有 sourceRecordId, 直接更新数据库中的 equipment_ids
                        val recordId = route.fromRecordId
                        if (recordId > 0) {
                            try {
                                val db = androidx.room.Room.databaseBuilder(
                                    navController.context.applicationContext,
                                    com.aerosun.heliumleakdetector.data.local.HeliumDatabase::class.java,
                                    "helium_database"
                                ).build()
                                kotlinx.coroutines.runBlocking {
                                    val entity = db.recordDao().getById(recordId)
                                    if (entity != null) {
                                        val eqJson = if (ids.isEmpty()) "" else com.google.gson.Gson().toJson(ids.toList())
                                        db.recordDao().update(entity.copy(equipmentIds = eqJson))
                                    }
                                }
                                db.close()
                            } catch (_: Exception) {}
                        } else {
                            // 新建记录场景：通过 pendingEquipmentResult 传递回 RecordEditScreen
                            pendingEquipmentResult.value = ids
                        }
                        navController.popBackStack()
                    },
                )
            }

            // 设置
            composable<Routes.Settings> {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // 帮助
            composable<Routes.Help> {
                HelpScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
