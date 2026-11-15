package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.TraKhucWaterApp
import com.example.data.local.UserSettingsPreferences
import com.example.data.repository.WaterLevelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WaterLevelSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? TraKhucWaterApp
            ?: return@withContext Result.failure()
        val prefs = app.container.settingsPreferences

        try {
            if (prefs.isAppForceStopped()) {
                return@withContext Result.success()
            }

            val stationId = inputData.getString(KEY_STATION_ID)
                ?: WaterLevelRepository.DEFAULT_STATION_ID

            val syncSuccess = executeUnifiedSync(app, stationId)

            // Luôn đặt lịch tiếp cho mốc 15 phút kế tiếp trong giờ (hh:00, hh:15, hh:30, hh:45)
            if (!prefs.isAppForceStopped()) {
                scheduleNextQuarterHourOneTimeWork(applicationContext)
                QuarterHourSyncReceiver.scheduleNextQuarterHourAlarm(applicationContext)
            }

            if (syncSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (!prefs.isAppForceStopped()) {
                scheduleNextQuarterHourOneTimeWork(applicationContext)
                QuarterHourSyncReceiver.scheduleNextQuarterHourAlarm(applicationContext)
            }
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "TraKhucUnifiedPeriodicSyncWork"
        const val EXACT_QUARTER_WORK_NAME = "TraKhucExactQuarterHourSyncWork"
        const val IMMEDIATE_WORK_NAME = "TraKhucImmediateQuarterHourSyncWork"
        private const val LEGACY_WORK_NAME = "TraKhucWaterLevelPeriodicSync"
        const val KEY_STATION_ID = "station_id"
        private const val SYNC_INTERVAL_MINUTES = 15L

        /**
         * Tính khoảng thời gian (ms) từ hiện tại đến đúng mốc phút thứ 00, 15, 30 hoặc 45 tiếp theo trong giờ.
         * Các mốc cố định: hh:00:00, hh:15:00, hh:30:00, hh:45:00.
         */
        fun computeDelayToNextQuarterHourMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
            }
            val currentMinute = calendar.get(Calendar.MINUTE)
            val nextQuarterMinute = when {
                currentMinute < 15 -> 15
                currentMinute < 30 -> 30
                currentMinute < 45 -> 45
                else -> 60
            }

            if (nextQuarterMinute == 60) {
                calendar.add(Calendar.HOUR_OF_DAY, 1)
                calendar.set(Calendar.MINUTE, 0)
            } else {
                calendar.set(Calendar.MINUTE, nextQuarterMinute)
            }
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val delay = calendar.timeInMillis - nowMillis
            return if (delay < 1_500L) {
                delay + 15L * 60L * 1_000L
            } else {
                delay
            }
        }

        /**
         * Tính thời điểm epoch (ms) của mốc hh:00, hh:15, hh:30, hh:45 tiếp theo.
         */
        fun computeNextQuarterHourTimestampMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            return nowMillis + computeDelayToNextQuarterHourMillis(nowMillis)
        }

        /**
         * Thực thi đồng bộ dữ liệu Mực nước, Bản tin và Thời tiết, đồng thời kiểm tra bắn thông báo đẩy
         * (nếu người dùng đang bật thông báo đẩy trong Cài đặt).
         */
        suspend fun executeUnifiedSync(
            app: TraKhucWaterApp,
            stationId: String = WaterLevelRepository.DEFAULT_STATION_ID
        ): Boolean = coroutineScope {
            if (app.container.settingsPreferences.isAppForceStopped()) {
                return@coroutineScope true
            }

            val repository = app.container.repository
            val weatherRepository = app.container.weatherRepository
            val notificationHelper = app.container.notificationHelper

            val waterDeferred = async {
                repository.syncWaterLevel(stationId = stationId)
            }
            val bulletinDeferred = async {
                repository.syncNewsArticles()
            }
            val weatherDeferred = async {
                runCatching { weatherRepository.fetchWeather(forceRefresh = true) }
            }

            val waterResult = waterDeferred.await()
            val bulletinResult = bulletinDeferred.await()
            weatherDeferred.await()

            // 1. Kiểm tra & bắn thông báo Mực nước dâng (NotificationHelper tự động kiểm tra nút Bật/Tắt thông báo)
            val latestPoint = waterResult.latestPoint
            val metadata = waterResult.metadata
            if (waterResult.shouldTriggerRisingAlert && latestPoint != null && metadata != null) {
                notificationHelper.showRisingWaterNotification(
                    riverName = metadata.riverName,
                    timeLabel = latestPoint.timeLabel,
                    currentLevel = latestPoint.waterLevel,
                    delta = waterResult.risingDelta,
                    alarm1 = metadata.alarmLevel1,
                    alarm2 = metadata.alarmLevel2,
                    alarm3 = metadata.alarmLevel3
                )
            }

            // 2. Kiểm tra & bắn thông báo Bản tin MỚI thuộc nhóm Quảng Ngãi / Trà Khúc
            val newHighlightBulletins = bulletinResult.newHighlightArticlesToNotify
            if (newHighlightBulletins.isNotEmpty()) {
                newHighlightBulletins
                    .sortedByDescending { it.timestampMillis }
                    .take(2)
                    .forEach { newBulletin ->
                        notificationHelper.showNewBulletinNotification(newBulletin)
                    }
            }

            waterResult.isSuccess || bulletinResult.isSuccess || waterResult.usedFallback
        }

        fun schedulePeriodicSync(context: Context) {
            val appContext = context.applicationContext
            if (UserSettingsPreferences(appContext).isAppForceStopped()) return

            val workManager = WorkManager.getInstance(appContext)
            workManager.cancelUniqueWork(LEGACY_WORK_NAME)

            val initialDelayMillis = computeDelayToNextQuarterHourMillis()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // 1. Đặt lịch OneTimeWorkRequest neo chính xác vào mốc hh:00, hh:15, hh:30, hh:45 tiếp theo
            scheduleNextQuarterHourOneTimeWork(appContext, initialDelayMillis)

            // 2. Đặt lịch PeriodicWorkRequest chu kỳ 15 phút (neo initialDelay vào mốc 15 phút tiếp theo)
            val periodicWorkRequest = PeriodicWorkRequestBuilder<WaterLevelSyncWorker>(
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10L,
                    TimeUnit.MINUTES
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )

            // 3. Đặt lịch AlarmManager neo đúng mốc hh:00, hh:15, hh:30, hh:45
            QuarterHourSyncReceiver.scheduleNextQuarterHourAlarm(appContext)
        }

        fun scheduleNextQuarterHourOneTimeWork(
            context: Context,
            delayMillis: Long = computeDelayToNextQuarterHourMillis()
        ) {
            val appContext = context.applicationContext
            if (UserSettingsPreferences(appContext).isAppForceStopped()) return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val exactQuarterRequest = OneTimeWorkRequestBuilder<WaterLevelSyncWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                EXACT_QUARTER_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                exactQuarterRequest
            )
        }

        fun enqueueImmediateSync(context: Context) {
            val appContext = context.applicationContext
            if (UserSettingsPreferences(appContext).isAppForceStopped()) return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<WaterLevelSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                immediateRequest
            )
        }

        fun cancelAllSync(context: Context) {
            val appContext = context.applicationContext
            val workManager = WorkManager.getInstance(appContext)
            workManager.cancelUniqueWork(LEGACY_WORK_NAME)
            workManager.cancelUniqueWork(WORK_NAME)
            workManager.cancelUniqueWork(EXACT_QUARTER_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
            workManager.cancelAllWork()
        }
    }
}
