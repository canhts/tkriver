package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.ThemeMode
import com.example.ui.theme.AppColorPalette

class UserSettingsPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Volatile
    private var cachedThemeMode: ThemeMode? = null

    @Volatile
    private var cachedColorPalette: AppColorPalette? = null

    @Volatile
    private var cachedPushNotificationEnabled: Boolean? = null

    @Volatile
    private var cachedAppForceStopped: Boolean? = null

    // --- Cài đặt giao diện (Theme & Color Palette) ---

    fun getThemeMode(): ThemeMode {
        cachedThemeMode?.let { return it }
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val resolved = runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
        cachedThemeMode = resolved
        return resolved
    }

    fun saveThemeMode(mode: ThemeMode) {
        cachedThemeMode = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getColorPalette(): AppColorPalette {
        cachedColorPalette?.let { return it }
        val raw = prefs.getString(KEY_COLOR_PALETTE, AppColorPalette.OCEAN_BLUE.name)
            ?: AppColorPalette.OCEAN_BLUE.name
        val resolved = runCatching { AppColorPalette.valueOf(raw) }.getOrDefault(AppColorPalette.OCEAN_BLUE)
        cachedColorPalette = resolved
        return resolved
    }

    fun saveColorPalette(palette: AppColorPalette) {
        cachedColorPalette = palette
        prefs.edit().putString(KEY_COLOR_PALETTE, palette.name).apply()
    }

    // --- Cài đặt Bật / Tắt Thông báo đẩy (Push Notifications Toggle) ---
    // Chỉ tắt thông báo đẩy, hệ thống tự động cập nhật dữ liệu theo các mốc giờ vẫn hoạt động bình thường.

    fun isPushNotificationEnabled(): Boolean {
        cachedPushNotificationEnabled?.let { return it }
        val enabled = prefs.getBoolean(KEY_PUSH_NOTIFICATION_ENABLED, true)
        cachedPushNotificationEnabled = enabled
        return enabled
    }

    fun setPushNotificationEnabled(enabled: Boolean) {
        cachedPushNotificationEnabled = enabled
        prefs.edit().putBoolean(KEY_PUSH_NOTIFICATION_ENABLED, enabled).apply()
    }

    // --- Trạng thái Tắt hoàn toàn ứng dụng từ Cài đặt (Force Stop State) ---
    // Khi người dùng bấm "Tắt hoàn toàn ứng dụng" và xác nhận đồng ý trong Cài đặt, cờ này = true
    // để chặn toàn bộ cập nhật ngầm và thông báo cho tới khi người dùng chủ động mở lại app.

    fun isAppForceStopped(): Boolean {
        cachedAppForceStopped?.let { return it }
        val stopped = prefs.getBoolean(KEY_APP_FORCE_STOPPED, false)
        cachedAppForceStopped = stopped
        return stopped
    }

    fun setAppForceStopped(stopped: Boolean) {
        cachedAppForceStopped = stopped
        prefs.edit().putBoolean(KEY_APP_FORCE_STOPPED, stopped).commit()
    }

    // --- Chống Spam Thông báo Mực nước (Anti-spam Water Level Alert) ---

    fun getLastNotifiedWaterTimeLabel(stationId: String): String? {
        return prefs.getString("${KEY_LAST_NOTIFIED_WATER_TIME}_$stationId", null)
    }

    fun setLastNotifiedWaterTimeLabel(stationId: String, timeLabel: String) {
        prefs.edit()
            .putString("${KEY_LAST_NOTIFIED_WATER_TIME}_$stationId", timeLabel)
            .apply()
    }

    fun getLastSeenWaterTimeLabel(stationId: String): String? {
        return prefs.getString("${KEY_LAST_SEEN_WATER_TIME}_$stationId", null)
    }

    fun getLastSeenWaterLevel(stationId: String): Float? {
        val key = "${KEY_LAST_SEEN_WATER_LEVEL}_$stationId"
        return if (prefs.contains(key)) prefs.getFloat(key, 0f) else null
    }

    fun saveLastSeenWaterPoint(stationId: String, timeLabel: String, waterLevel: Float) {
        prefs.edit()
            .putString("${KEY_LAST_SEEN_WATER_TIME}_$stationId", timeLabel)
            .putFloat("${KEY_LAST_SEEN_WATER_LEVEL}_$stationId", waterLevel)
            .apply()
    }

    // --- Chống Spam Thông báo Bản tin Quảng Ngãi / Trà Khúc (Anti-spam Bulletin Alert) ---

    fun isBulletinBaselineInitialized(): Boolean {
        return prefs.getBoolean(KEY_BULLETIN_BASELINE_INIT, false)
    }

    fun getNotifiedBulletinUrls(): Set<String> {
        return prefs.getStringSet(KEY_NOTIFIED_BULLETIN_URLS, emptySet()) ?: emptySet()
    }

    fun getLastNotifiedBulletinTimestamp(): Long {
        return prefs.getLong(KEY_LAST_NOTIFIED_BULLETIN_TS, 0L)
    }

    fun markBulletinsAsNotified(urls: Collection<String>, maxTimestampMillis: Long) {
        val existing = LinkedHashSet(getNotifiedBulletinUrls())
        existing.addAll(urls)
        // Giữ tối đa 100 URL gần nhất để bộ nhớ SharedPreferences luôn siêu nhẹ
        val trimmed = if (existing.size > 100) {
            existing.drop(existing.size - 100).toSet()
        } else {
            existing
        }
        val prevMaxTs = getLastNotifiedBulletinTimestamp()
        val updatedTs = maxOf(prevMaxTs, maxTimestampMillis)

        prefs.edit()
            .putBoolean(KEY_BULLETIN_BASELINE_INIT, true)
            .putStringSet(KEY_NOTIFIED_BULLETIN_URLS, trimmed)
            .putLong(KEY_LAST_NOTIFIED_BULLETIN_TS, updatedTs)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "tkriver_user_settings_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_COLOR_PALETTE = "key_color_palette"
        private const val KEY_PUSH_NOTIFICATION_ENABLED = "key_push_notification_enabled"
        private const val KEY_APP_FORCE_STOPPED = "key_app_force_stopped"

        private const val KEY_LAST_NOTIFIED_WATER_TIME = "key_last_notified_water_time"
        private const val KEY_LAST_SEEN_WATER_TIME = "key_last_seen_water_time"
        private const val KEY_LAST_SEEN_WATER_LEVEL = "key_last_seen_water_level"

        private const val KEY_BULLETIN_BASELINE_INIT = "key_bulletin_baseline_init"
        private const val KEY_NOTIFIED_BULLETIN_URLS = "key_notified_bulletin_urls"
        private const val KEY_LAST_NOTIFIED_BULLETIN_TS = "key_last_notified_bulletin_ts"
    }
}
