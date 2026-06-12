package com.aerosun.heliumleakdetector.core.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.aerosun.heliumleakdetector.MainActivity
import com.aerosun.heliumleakdetector.data.local.HeliumDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 设备校准周期检查 Worker。
 *
 * 每天执行一次，检查即将到期（7天内）的设备，发送系统通知提醒。
 * 使用 WorkManager PeriodicWorkRequest，最小间隔 15 分钟。
 */
class CalibrationCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "calibration_reminder"
        private const val CHANNEL_NAME = "校准提醒"
        const val WORK_NAME = "calibration_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CalibrationCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // 不重复调度
                request,
            )

            // 创建通知渠道 (Android 8.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "设备校准有效期到期提醒" }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return Result.success() // 无通知权限，静默跳过
        }

        val db = HeliumDatabase::class.java.let { /* cannot access directly */ }
        // 使用 Application context 创建临时数据库访问
        val threshold = System.currentTimeMillis() + 7L * 24 * 3600 * 1000

        try {
            // 通过直接查询检查即将到期的设备
            val equipmentDao = (applicationContext as? android.app.Application)
                ?.let { app ->
                    // 通过反射或直接构建 Room 实例访问
                    // 简化：使用 context 构建临时 database 实例
                    androidx.room.Room.databaseBuilder(
                        applicationContext,
                        HeliumDatabase::class.java,
                        "helium_database"
                    ).build().equipmentDao()
                }

            if (equipmentDao == null) return Result.success()

            val expiring = equipmentDao.getExpiringSoon(threshold)
            if (expiring.isNotEmpty()) {
                showNotification(expiring.size, expiring.firstOrNull()?.name ?: "")
            }

            // 更新过期设备状态
            expiring.filter { it.calibrationDueDate < System.currentTimeMillis() }.forEach {
                // 标记为 expired
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun showNotification(count: Int, firstName: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (count == 1) "「$firstName」校准即将到期"
        else "$count 台设备校准即将到期"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("点击查看设备管理，及时安排校准")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
