package com.example.data.forecast

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.data.local.NewsArticleEntity
import com.example.data.local.StationMetadataEntity
import com.example.data.local.WaterPointEntity
import com.example.ui.theme.AlarmLevel1Green
import com.example.ui.theme.AlarmLevel2Orange
import com.example.ui.theme.AlarmLevel3Red
import com.example.ui.theme.DeltaFallingBlue
import com.example.ui.theme.DeltaRisingRed
import com.example.ui.theme.DeltaStableGray
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Immutable
data class WaterLevelPrediction(
    val currentLevel: Float,
    val targetTimeLabel: String,
    val predictedLevel: Float,
    val predictedDelta: Float,
    val trendBadge: String,
    val trendColor: Color,
    val alarmStatus: String,
    val alarmColor: Color,
    val confidencePercent: Int,
    val safetyRecommendation: String
)

object WaterLevelForecastEngine {

    private val LOCALE_VI = Locale("vi", "VN")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun computeNextHourForecast(
        points: List<WaterPointEntity>,
        articles: List<NewsArticleEntity>,
        metadata: StationMetadataEntity
    ): WaterLevelPrediction {
        if (points.isEmpty()) {
            return WaterLevelPrediction(
                currentLevel = 0f,
                targetTimeLabel = "1 giờ tới",
                predictedLevel = 0f,
                predictedDelta = 0f,
                trendBadge = "Chưa đủ dữ liệu",
                trendColor = DeltaStableGray,
                alarmStatus = "Đang chờ mốc đo",
                alarmColor = DeltaStableGray,
                confidencePercent = 0,
                safetyRecommendation = "Theo dõi các bản tin tiếp theo."
            )
        }

        val latest = points.last()
        val currentLevel = latest.waterLevel
        val delta1h = latest.delta
        val delta2h = if (points.size >= 3) {
            points[points.size - 2].waterLevel - points[points.size - 3].waterLevel
        } else {
            delta1h
        }

        // Tốc độ và gia tốc biến thiên mực nước
        val velocity = delta1h
        val acceleration = delta1h - delta2h

        // Phân tích dữ liệu khí tượng từ bản tin NCHMF gần nhất
        val latestRelevantArticle = articles.firstOrNull { it.isHighlight } ?: articles.firstOrNull()
        val bulletinText = if (latestRelevantArticle != null) {
            buildString(latestRelevantArticle.title.length + (latestRelevantArticle.cachedContent?.length ?: 0) + 1) {
                append(latestRelevantArticle.title)
                if (!latestRelevantArticle.cachedContent.isNullOrEmpty()) {
                    append(' ')
                    append(latestRelevantArticle.cachedContent)
                }
            }.uppercase(LOCALE_VI)
        } else {
            ""
        }

        val meteorologyTrendWeight = when {
            bulletinText.contains("LŨ TIẾP TỤC LÊN") || bulletinText.contains("MƯA RẤT TO") || bulletinText.contains("LŨ KHẨN CẤP") -> 0.08f
            bulletinText.contains("ĐẠT ĐỈNH") || bulletinText.contains("BIẾN ĐỔI CHẬM") || bulletinText.contains("DAO ĐỘNG") -> 0.01f
            bulletinText.contains("RÚT") || bulletinText.contains("XUỐNG CHẬM") || bulletinText.contains("GIẢM") -> -0.06f
            else -> 0f
        }

        // Mô hình thủy văn dự báo 1 giờ tiếp theo kết hợp quán tính dòng chảy và hiệu chỉnh khí tượng:
        // deltaForecast = 0.75 * v + 0.15 * a + 0.35 * meteorologyTrendWeight
        val rawPredictedDelta = (0.75f * velocity) + (0.15f * acceleration) + (0.35f * meteorologyTrendWeight)
        // Giới hạn biên độ biến thiên tối đa trong 1 giờ để đảm bảo tính thực tế thủy văn (tối đa +/- 0.45m/h)
        val clampedDelta = rawPredictedDelta.coerceIn(-0.45f, 0.45f)
        val predictedLevel = max(0.1f, currentLevel + clampedDelta)

        val targetTimeLabel = generateNextHourLabel(latest.timeLabel)

        // Phân loại xu thế đồng nhất với chiều biến thiên dự báo
        val (trendBadge, trendColor) = when {
            clampedDelta >= 0.08f -> "Dâng nhanh" to DeltaRisingRed
            clampedDelta > 0.0001f -> "Tăng nhẹ" to DeltaRisingRed
            clampedDelta <= -0.08f -> "Rút nhanh" to DeltaFallingBlue
            clampedDelta < -0.0001f -> "Rút chậm" to DeltaFallingBlue
            else -> "Ổn định" to DeltaStableGray
        }

        // Đánh giá cấp độ báo động dự kiến
        val (alarmStatus, alarmColor) = when {
            predictedLevel >= metadata.alarmLevel3 -> "Vượt Báo động 3" to AlarmLevel3Red
            predictedLevel >= metadata.alarmLevel2 -> "Vượt Báo động 2" to AlarmLevel2Orange
            predictedLevel >= metadata.alarmLevel1 -> "Vượt Báo động 1" to AlarmLevel1Green
            else -> "Dưới Báo động 1" to AlarmLevel1Green
        }

        // Khuyến cáo ứng phó an toàn
        val recommendation = when {
            predictedLevel >= metadata.alarmLevel3 ->
                "Mực nước dự kiến ở mức nguy hiểm. Đề phòng ngập sâu diện rộng hạ lưu sông Trà Khúc (TP. Quảng Ngãi, Tư Nghĩa, Sơn Tịnh); di dời người và tài sản khỏi vùng trũng thấp."
            predictedLevel >= metadata.alarmLevel2 ->
                "Mực nước dâng trên mức BĐ2. Các khu vực ven sông, vùng cồn bãi cần chủ động kê cao tài sản, nghiêm cấm người dân đánh bắt thủy sản tại bờ kè ven sông."
            predictedLevel >= metadata.alarmLevel1 ->
                "Mực nước ở ngưỡng cảnh báo BĐ1. Lưu ý vùng hạ lưu trũng thấp ven sông và theo dõi sát diễn biến mưa lớn thượng nguồn."
            else ->
                "Mực nước trong ngưỡng an toàn, dòng chảy bình thường. Tiếp tục theo dõi thông tin cập nhật các khung giờ kế tiếp."
        }

        val confidence = min(96, max(85, 90 + (points.size.coerceAtMost(24) / 4)))

        return WaterLevelPrediction(
            currentLevel = currentLevel,
            targetTimeLabel = targetTimeLabel,
            predictedLevel = predictedLevel,
            predictedDelta = clampedDelta,
            trendBadge = trendBadge,
            trendColor = trendColor,
            alarmStatus = alarmStatus,
            alarmColor = alarmColor,
            confidencePercent = confidence,
            safetyRecommendation = recommendation
        )
    }

    private fun generateNextHourLabel(currentTimeLabel: String): String {
        val trimmed = currentTimeLabel.trim()
        val parts = trimmed.split(WHITESPACE_REGEX)
        val hourPart = parts.firstOrNull().orEmpty().lowercase(Locale.ROOT).replace("h", "")
        val currentHour = hourPart.toIntOrNull()

        return if (currentHour != null) {
            val nextHour = (currentHour + 1) % 24
            val datePart = if (parts.size >= 2) " " + parts.last() else ""
            "${nextHour}h$datePart"
        } else {
            "1 giờ tới"
        }
    }
}
