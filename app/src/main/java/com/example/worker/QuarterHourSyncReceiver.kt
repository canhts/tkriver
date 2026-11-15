package com.example.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.TraKhucWaterApp
import com.example.data.local.UserSettingsPreferences

class QuarterHourSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val prefs = (appContext as? TraKhucWaterApp)?.container?.settingsPreferences
            ?: UserSettingsPreferences(appContext)

        // Nếu người dùng đã bấm "Tắt hoàn toàn ứng dụng" trong Cài đặt, không chạy ngầm cho đến khi mở lại app
        if (prefs.isAppForceStopped()) {
            cancelScheduledAlarm(appContext)
            return
        }

        // Khởi động lại lịch cho mốc 15 phút kế tiếp (hh:00, hh:15, hh:30, hh:45)
        scheduleNextQuarterHourAlarm(appContext)
        WaterLevelSyncWorker.scheduleNextQuarterHourOneTimeWork(appContext)

        // Thực thi đồng bộ dữ liệu ngay tại mốc giờ (hh:00, hh:15, hh:30, hh:45)
        WaterLevelSyncWorker.enqueueImmediateSync(appContext)

        // Đảm bảo tiến trình chạy ngầm được duy trì kể cả khi thoát app hoặc vuốt khỏi đa nhiệm
        (appContext as? TraKhucWaterApp)?.ensureBackgroundSyncRunning()
    }

    companion object {
        const val ACTION_QUARTER_HOUR_SYNC = "com.example.action.QUARTER_HOUR_SYNC"
        private const val REQUEST_CODE_QUARTER_HOUR_ALARM = 71541

        private fun buildPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context.applicationContext, QuarterHourSyncReceiver::class.java).apply {
                action = ACTION_QUARTER_HOUR_SYNC
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(
                context.applicationContext,
                REQUEST_CODE_QUARTER_HOUR_ALARM,
                intent,
                flags
            )
        }

        fun scheduleNextQuarterHourAlarm(context: Context) {
            val appContext = context.applicationContext
            if (UserSettingsPreferences(appContext).isAppForceStopped()) return

            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return

            val triggerAtMillis = WaterLevelSyncWorker.computeNextQuarterHourTimestampMillis()
            val pendingIntent = buildPendingIntent(appContext)

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        }

        fun cancelScheduledAlarm(context: Context) {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return
            runCatching {
                alarmManager.cancel(buildPendingIntent(appContext))
            }
        }
    }
}
