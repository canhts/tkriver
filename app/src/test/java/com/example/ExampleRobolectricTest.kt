package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserSettingsPreferences
import com.example.worker.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TKRIVER", appName)
  }

  @Test
  fun `rising water notification triggers only when level is at or above alarm 2`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    val prefs = UserSettingsPreferences(app).apply {
      setAppForceStopped(false)
      setPushNotificationEnabled(true)
    }
    val helper = NotificationHelper(app, prefs)

    // Dưới BĐ2 (4.99m < 5.00m) dù tăng 0.30m vẫn KHÔNG bắn thông báo
    val belowAlarm2 = helper.showRisingWaterNotification(
      riverName = "Trà Khúc",
      timeLabel = "10h 12/11",
      currentLevel = 4.99f,
      delta = 0.30f,
      alarm1 = 3.50f,
      alarm2 = 5.00f,
      alarm3 = 6.50f
    )
    assertFalse(belowAlarm2)

    // Từ BĐ2 trở lên (5.00m >= 5.00m) và đang tăng -> Bắn thông báo
    val atAlarm2 = helper.showRisingWaterNotification(
      riverName = "Trà Khúc",
      timeLabel = "11h 12/11",
      currentLevel = 5.00f,
      delta = 0.10f,
      alarm1 = 3.50f,
      alarm2 = 5.00f,
      alarm3 = 6.50f
    )
    assertTrue(atAlarm2)
  }
}
