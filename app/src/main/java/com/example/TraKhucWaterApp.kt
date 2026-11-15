package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.WaterLevelDatabase
import com.example.data.remote.OpenMeteoApi
import com.example.data.remote.VndmsApi
import com.example.data.repository.WaterLevelRepository
import com.example.data.repository.WeatherRepository
import com.example.worker.NotificationHelper
import com.example.worker.QuarterHourSyncReceiver
import com.example.worker.WaterLevelKeepAliveService
import com.example.worker.WaterLevelSyncWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val vndmsApi: VndmsApi by lazy {
        Retrofit.Builder()
            .baseUrl(VndmsApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VndmsApi::class.java)
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    val database: WaterLevelDatabase by lazy {
        WaterLevelDatabase.getInstance(appContext)
    }

    val settingsPreferences: UserSettingsPreferences by lazy {
        UserSettingsPreferences(appContext)
    }

    val repository: WaterLevelRepository by lazy {
        WaterLevelRepository(
            api = vndmsApi,
            dao = database.waterLevelDao(),
            settingsPreferences = settingsPreferences
        )
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(
            openMeteoApi = openMeteoApi,
            context = appContext
        )
    }

    val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(
            context = appContext,
            settingsPreferences = settingsPreferences
        )
    }
}

class TraKhucWaterApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var quarterHourTickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Nếu người dùng chưa bấm "Tắt hoàn toàn ứng dụng" trong Cài đặt thì khởi chạy hệ thống cập nhật 4 mốc/giờ
        if (!container.settingsPreferences.isAppForceStopped()) {
            ensureBackgroundSyncRunning()
        }
    }

    /**
     * Khởi chạy và đảm bảo hệ thống cập nhật dữ liệu chạy ngầm ở đúng 4 mốc trong giờ:
     * hh:00, hh:15, hh:30, hh:45 (kể cả khi đóng ứng dụng hoặc vuốt khỏi đa nhiệm).
     */
    fun ensureBackgroundSyncRunning() {
        appScope.launch {
            if (container.settingsPreferences.isAppForceStopped()) return@launch

            WaterLevelSyncWorker.schedulePeriodicSync(this@TraKhucWaterApp)
            QuarterHourSyncReceiver.scheduleNextQuarterHourAlarm(this@TraKhucWaterApp)
            WaterLevelKeepAliveService.startIfAllowed(this@TraKhucWaterApp)
            startQuarterHourInProcessLoopIfNeeded()
        }
    }

    /**
     * Bộ đếm thời gian chính xác theo mốc hh:00, hh:15, hh:30, hh:45 ngay trong tiến trình ứng dụng.
     */
    fun startQuarterHourInProcessLoopIfNeeded() {
        if (container.settingsPreferences.isAppForceStopped()) return
        if (quarterHourTickerJob?.isActive == true) return

        quarterHourTickerJob = appScope.launch {
            while (isActive && !container.settingsPreferences.isAppForceStopped()) {
                val waitMillis = WaterLevelSyncWorker.computeDelayToNextQuarterHourMillis()
                delay(waitMillis)

                if (!isActive || container.settingsPreferences.isAppForceStopped()) {
                    break
                }

                runCatching {
                    WaterLevelSyncWorker.executeUnifiedSync(this@TraKhucWaterApp)
                }
                QuarterHourSyncReceiver.scheduleNextQuarterHourAlarm(this@TraKhucWaterApp)
                WaterLevelSyncWorker.scheduleNextQuarterHourOneTimeWork(this@TraKhucWaterApp)
            }
        }
    }

    /**
     * Tắt hoàn toàn ứng dụng theo xác nhận của người dùng trong Cài đặt:
     * - Đặt cờ Force Stopped = true
     * - Hủy toàn bộ tiến trình đồng bộ ngầm (WorkManager, AlarmManager, Coroutine Ticker, KeepAliveService)
     * - Xóa toàn bộ thông báo đẩy đang hiển thị
     */
    fun forceStopApplication() {
        container.settingsPreferences.setAppForceStopped(true)
        quarterHourTickerJob?.cancel()
        quarterHourTickerJob = null
        QuarterHourSyncReceiver.cancelScheduledAlarm(this)
        WaterLevelSyncWorker.cancelAllSync(this)
        WaterLevelKeepAliveService.stop(this)
        container.notificationHelper.cancelAllNotifications()
    }
}
