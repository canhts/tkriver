package com.example.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import com.example.TraKhucWaterApp
import com.example.data.local.UserSettingsPreferences

/**
 * Service chạy ngầm nhẹ (START_STICKY) đảm bảo khi người dùng thoát app hoặc vuốt đóng trong đa nhiệm Android
 * (onTaskRemoved), hệ thống tự động cập nhật dữ liệu ở 4 mốc giờ (hh:00, hh:15, hh:30, hh:45) và thông báo đẩy
 * vẫn tiếp tục hoạt động bình thường.
 * Chỉ khi người dùng bấm "Tắt hoàn toàn ứng dụng" trong Cài đặt và xác nhận đồng ý thì Service mới dừng hẳn.
 */
class WaterLevelKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = (applicationContext as? TraKhucWaterApp)?.container?.settingsPreferences
            ?: UserSettingsPreferences(applicationContext)
        if (prefs.isAppForceStopped()) {
            stopSelf()
            return START_NOT_STICKY
        }

        (applicationContext as? TraKhucWaterApp)?.startQuarterHourInProcessLoopIfNeeded()
        WaterLevelSyncWorker.schedulePeriodicSync(applicationContext)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val prefs = (applicationContext as? TraKhucWaterApp)?.container?.settingsPreferences
            ?: UserSettingsPreferences(applicationContext)
        if (!prefs.isAppForceStopped()) {
            // Người dùng chỉ vuốt ứng dụng khỏi màn hình đa nhiệm Android -> giữ nguyên tiến trình chạy ngầm
            WaterLevelSyncWorker.schedulePeriodicSync(applicationContext)
            QuarterHourSyncReceiver.scheduleNextQuarterHourAlarm(applicationContext)

            val restartIntent = Intent(applicationContext, WaterLevelKeepAliveService::class.java)
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_ONE_SHOT
            }
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                71542,
                restartIntent,
                pendingFlags
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            runCatching {
                alarmManager?.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 1_500L,
                    pendingIntent
                )
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        fun startIfAllowed(context: Context) {
            val appContext = context.applicationContext
            if (UserSettingsPreferences(appContext).isAppForceStopped()) return
            runCatching {
                appContext.startService(Intent(appContext, WaterLevelKeepAliveService::class.java))
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.stopService(Intent(appContext, WaterLevelKeepAliveService::class.java))
            }
        }
    }
}
