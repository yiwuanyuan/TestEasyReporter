package com.aerosun.heliumleakdetector.ui.navigation

import kotlinx.serialization.Serializable

/**
 * 类型安全的 Navigation 路由定义。
 *
 * 使用 @Serializable 注解实现编译期路由校验，
 * 避免字符串路由的拼写错误。
 */
sealed interface Routes {

    /** 首页 — 检测记录列表 */
    @Serializable
    data object RecordList : Routes

    /** 新建检测记录 */
    @Serializable
    data object RecordCreate : Routes

    /** 编辑已有记录 (recordId = 数据库主键) */
    @Serializable
    data class RecordEdit(val recordId: Long) : Routes

    /** 记录详情 (recordId = 数据库主键) */
    @Serializable
    data class RecordDetail(val recordId: Long) : Routes

    /** 设备管理 */
    @Serializable
    data object EquipmentList : Routes

    /** 设置 */
    @Serializable
    data object Settings : Routes

    /** 帮助/关于 */
    @Serializable
    data object Help : Routes

    /** 设备管理 */
    @Serializable
    data object EquipmentCreate : Routes

    /** 编辑设备 */
    @Serializable
    data class EquipmentEdit(val equipmentId: Long) : Routes

    /** 试验设备选择（关联到记录） */
    @Serializable
    data class EquipmentSelector(val fromRecordId: Long = -1) : Routes
}
