package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NewsArticleEntity
import com.example.data.local.UserSettingsPreferences
import java.util.Locale
import kotlin.math.abs

class NotificationHelper(
    private val context: Context,
    private val settingsPreferences: UserSettingsPreferences = UserSettingsPreferences(context)
) {

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val waterChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
            }

            val bulletinChannel = NotificationChannel(
                BULLETIN_CHANNEL_ID,
                "Bản tin Thủy văn Quảng Ngãi & Trà Khúc",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi có bản tin thủy văn nguy hiểm mới liên quan đến Quảng Ngãi và sông Trà Khúc"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(waterChannel)
            notificationManager.createNotificationChannel(bulletinChannel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun canShowPushNotification(): Boolean {
        if (settingsPreferences.isAppForceStopped()) return false
        if (!settingsPreferences.isPushNotificationEnabled()) return false
        return hasNotificationPermission()
    }

    fun cancelAllNotifications() {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }

    fun showRisingWaterNotification(
        riverName: String,
        timeLabel: String,
        currentLevel: Float,
        delta: Float,
        alarm1: Float,
        alarm2: Float,
        alarm3: Float
    ): Boolean {
        if (!canShowPushNotification()) return false
        if (delta <= 0.0001f || currentLevel < alarm2) return false

        val alarmStatusText = when {
            currentLevel >= alarm3 -> "Hiện đang trên BĐ3."
            currentLevel >= alarm2 -> "Hiện đang từ BĐ2 trở lên."
            currentLevel >= alarm1 -> "Hiện đang trên BĐ1."
            else -> "Hiện đang dưới BĐ1."
        }

        val formattedCurrent = String.format(Locale.US, "%.2f", currentLevel)
        val formattedDelta = String.format(Locale.US, "▲ %.2f", abs(delta))

        val title = "⚠️ Cảnh báo: Nước sông $riverName đang dâng!"
        val message =
            "Mực nước lúc $timeLabel là ${formattedCurrent}m ($formattedDelta m so với mốc trước). $alarmStatusText"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun showNewBulletinNotification(article: NewsArticleEntity): Boolean {
        if (!canShowPushNotification()) return false

        val title = "📰 Bản tin mới: Quảng Ngãi • Trà Khúc"
        val message = "${article.title} (${article.pubDateStr})"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_OPEN_BULLETIN_TAB, true)
        }
        val notificationId = BULLETIN_NOTIFICATION_BASE_ID + (article.url.hashCode() and 0x0FFF)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, BULLETIN_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "tra_khuc_flood_alert_channel"
        const val BULLETIN_CHANNEL_ID = "tra_khuc_bulletin_alert_channel"
        const val NOTIFICATION_ID = 71540
        const val BULLETIN_NOTIFICATION_BASE_ID = 72000
        const val EXTRA_OPEN_BULLETIN_TAB = "extra_open_bulletin_tab"
    }
}
