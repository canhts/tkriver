package com.example.data.repository

import androidx.compose.runtime.Immutable
import com.example.data.local.NewsArticleEntity
import com.example.data.local.StationMetadataEntity
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.WaterLevelDao
import com.example.data.local.WaterPointEntity
import com.example.data.remote.NchmfScraper
import com.example.data.remote.VndmsApi
import com.example.data.remote.WaterLevelDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.abs

@Immutable
data class SyncResult(
    val isSuccess: Boolean,
    val usedFallback: Boolean,
    val shouldTriggerRisingAlert: Boolean,
    val latestPoint: WaterPointEntity?,
    val risingDelta: Float,
    val metadata: StationMetadataEntity?
)

@Immutable
data class BulletinSyncResult(
    val isSuccess: Boolean,
    val newHighlightArticlesToNotify: List<NewsArticleEntity> = emptyList()
)

class WaterLevelRepository(
    private val api: VndmsApi,
    private val dao: WaterLevelDao,
    private val settingsPreferences: UserSettingsPreferences,
    private val nchmfScraper: NchmfScraper = NchmfScraper()
) {

    private val waterSyncMutex = Mutex()
    private val newsSyncMutex = Mutex()

    fun observeStationMetadata(stationId: String = DEFAULT_STATION_ID): Flow<StationMetadataEntity?> {
        return dao.observeStationMetadata(stationId)
    }

    fun observeWaterPoints(stationId: String = DEFAULT_STATION_ID): Flow<List<WaterPointEntity>> {
        return dao.observeWaterPoints(stationId)
    }

    fun observeNewsArticles(): Flow<List<NewsArticleEntity>> {
        return dao.observeNewsArticles()
    }

    suspend fun ensureSeededIfEmpty(stationId: String = DEFAULT_STATION_ID) = withContext(Dispatchers.IO) {
        try {
            val existingPoints = dao.getWaterPoints(stationId)
            if (existingPoints.isEmpty()) {
                val fallbackDto = createRealFallbackDto(stationId)
                val (_, seededPoints) = saveDtoToDatabase(
                    dto = fallbackDto,
                    stationId = stationId,
                    isFallback = true
                )
                seededPoints.lastOrNull()?.let { latestSeed ->
                    if (settingsPreferences.getLastSeenWaterTimeLabel(stationId) == null) {
                        settingsPreferences.saveLastSeenWaterPoint(
                            stationId = stationId,
                            timeLabel = latestSeed.timeLabel,
                            waterLevel = latestSeed.waterLevel
                        )
                        settingsPreferences.setLastNotifiedWaterTimeLabel(
                            stationId = stationId,
                            timeLabel = latestSeed.timeLabel
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore seed failure safely
        }

        try {
            val existingArticles = dao.getNewsArticles()
            if (existingArticles.isEmpty()) {
                val fallbackArticles = createFallbackNewsArticles()
                dao.insertNewsArticles(fallbackArticles)
            }
        } catch (_: Exception) {
            // Ignore seed failure safely
        }
    }

    suspend fun syncNewsArticles(): BulletinSyncResult = withContext(Dispatchers.IO) {
        newsSyncMutex.withLock {
            try {
                val scrapedArticles = nchmfScraper.scrapeHydrologicalBulletins(
                    targetCount = NchmfScraper.MAX_BULLETIN_COUNT,
                    maxPages = 6
                )
                if (scrapedArticles.isNotEmpty()) {
                    val isBaselineInitialized = settingsPreferences.isBulletinBaselineInitialized()
                    val alreadyNotifiedUrls = settingsPreferences.getNotifiedBulletinUrls()
                    val lastNotifiedTs = settingsPreferences.getLastNotifiedBulletinTimestamp()

                    // Chỉ lọc bản tin MỚI liên quan đến Quảng Ngãi / Trà Khúc (isHighlight == true)
                    // và chưa từng được thông báo trước đó (Anti-spam)
                    val newHighlightsToNotify = if (!isBaselineInitialized) {
                        emptyList()
                    } else {
                        scrapedArticles.filter { article ->
                            article.isHighlight &&
                                article.url !in alreadyNotifiedUrls &&
                                article.timestampMillis >= lastNotifiedTs
                        }
                    }

                    // Cập nhật danh sách URL và mốc thời gian đã kiểm tra vào bộ nhớ cục bộ để chống spam
                    val maxScrapedTs = scrapedArticles.maxOfOrNull { it.timestampMillis } ?: 0L
                    settingsPreferences.markBulletinsAsNotified(
                        urls = scrapedArticles.map { it.url },
                        maxTimestampMillis = maxScrapedTs
                    )

                    dao.replaceNewsArticlesPreservingCache(scrapedArticles)
                    BulletinSyncResult(
                        isSuccess = true,
                        newHighlightArticlesToNotify = newHighlightsToNotify
                    )
                } else {
                    val existing = dao.getNewsArticles()
                    if (existing.isEmpty()) {
                        dao.insertNewsArticles(createFallbackNewsArticles())
                    }
                    BulletinSyncResult(isSuccess = false)
                }
            } catch (_: Exception) {
                try {
                    val existing = dao.getNewsArticles()
                    if (existing.isEmpty()) {
                        dao.insertNewsArticles(createFallbackNewsArticles())
                    }
                } catch (_: Exception) {
                }
                BulletinSyncResult(isSuccess = false)
            }
        }
    }

    suspend fun getOrFetchArticleContent(article: NewsArticleEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cachedInDb = dao.getNewsArticleByUrl(article.url)?.cachedContent ?: article.cachedContent
            if (!cachedInDb.isNullOrBlank()) {
                return@withContext Result.success(cachedInDb)
            }

            val scrapedContent = nchmfScraper.scrapeArticleDetailContent(article.url)
            if (scrapedContent.isNotBlank()) {
                dao.updateArticleContent(article.url, scrapedContent)
                Result.success(scrapedContent)
            } else {
                Result.failure(IllegalStateException("Nội dung bản tin trống."))
            }
        } catch (e: Exception) {
            val fallbackCached = dao.getNewsArticleByUrl(article.url)?.cachedContent ?: article.cachedContent
            if (!fallbackCached.isNullOrBlank()) {
                Result.success(fallbackCached)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun syncWaterLevel(
        stationId: String = DEFAULT_STATION_ID,
        source: String = DEFAULT_SOURCE,
        timeSelect: String = DEFAULT_TIME_SELECT
    ): SyncResult = withContext(Dispatchers.IO) {
        waterSyncMutex.withLock {
            val previousLatestInDb = try {
                dao.getLatestWaterPoint(stationId)
            } catch (_: Exception) {
                null
            }

            val lastSeenTimeLabel = settingsPreferences.getLastSeenWaterTimeLabel(stationId)
                ?: previousLatestInDb?.timeLabel
            val lastSeenLevel = settingsPreferences.getLastSeenWaterLevel(stationId)
                ?: previousLatestInDb?.waterLevel
            val lastNotifiedTimeLabel = settingsPreferences.getLastNotifiedWaterTimeLabel(stationId)

            try {
                val responseDto = api.fetchStationWaterLevel(
                    stationId = stationId,
                    source = source,
                    timeSelect = timeSelect
                )

                val parsed = parseDto(
                    dto = responseDto,
                    stationId = stationId,
                    isFallback = false
                )

                if (parsed.second.isEmpty()) {
                    throw IllegalStateException("Dữ liệu trả về từ máy chủ không có mốc đo hợp lệ.")
                }

                val metadata = parsed.first
                val points = parsed.second
                val newLatest = points.last()
                val precedingHourPoint = if (points.size >= 2) points[points.size - 2] else null

                val referencePreviousLevel = when {
                    lastSeenLevel != null && lastSeenTimeLabel != null && lastSeenTimeLabel != newLatest.timeLabel ->
                        lastSeenLevel
                    precedingHourPoint != null ->
                        precedingHourPoint.waterLevel
                    lastSeenLevel != null ->
                        lastSeenLevel
                    else ->
                        newLatest.waterLevel
                }

                val risingDelta = newLatest.waterLevel - referencePreviousLevel

                // Điều kiện chống spam (Anti-spam) & ngưỡng kích hoạt:
                // 1. Đã có mốc cơ sở trước đó (lastSeenTimeLabel != null)
                // 2. Có mốc đo MỚI xuất hiện (newLatest.timeLabel != lastSeenTimeLabel)
                // 3. Mốc đo này chưa từng được bắn thông báo (newLatest.timeLabel != lastNotifiedTimeLabel)
                // 4. Mực nước mới thực sự TĂNG so với mốc gần nhất (risingDelta > 0.0001f)
                // 5. Mực nước mới phải từ mức Báo động 2 trở lên (newLatest.waterLevel >= metadata.alarmLevel2)
                val isNewTimePoint = lastSeenTimeLabel != null && newLatest.timeLabel != lastSeenTimeLabel
                val notYetNotified = newLatest.timeLabel != lastNotifiedTimeLabel
                val isAtOrAboveAlarm2 = newLatest.waterLevel >= metadata.alarmLevel2
                val shouldAlert = isNewTimePoint && notYetNotified && risingDelta > 0.0001f && isAtOrAboveAlarm2

                // Lưu lại mốc mới nhất vào SharedPreferences để các lần kiểm tra tiếp theo đối chiếu chính xác
                settingsPreferences.saveLastSeenWaterPoint(
                    stationId = stationId,
                    timeLabel = newLatest.timeLabel,
                    waterLevel = newLatest.waterLevel
                )
                if (shouldAlert || lastNotifiedTimeLabel == null) {
                    settingsPreferences.setLastNotifiedWaterTimeLabel(
                        stationId = stationId,
                        timeLabel = newLatest.timeLabel
                    )
                }

                dao.replaceStationData(
                    stationId = stationId,
                    metadata = metadata,
                    points = points
                )

                SyncResult(
                    isSuccess = true,
                    usedFallback = false,
                    shouldTriggerRisingAlert = shouldAlert,
                    latestPoint = newLatest,
                    risingDelta = if (shouldAlert) risingDelta else newLatest.delta,
                    metadata = metadata
                )
            } catch (_: Exception) {
                try {
                    val currentPoints = dao.getWaterPoints(stationId)
                    val currentMeta = dao.getStationMetadata(stationId)

                    if (currentPoints.isEmpty() || currentMeta == null) {
                        val fallbackDto = createRealFallbackDto(stationId)
                        val (fallbackMeta, fallbackPoints) = saveDtoToDatabase(
                            dto = fallbackDto,
                            stationId = stationId,
                            isFallback = true
                        )
                        val latest = fallbackPoints.lastOrNull()
                        val delta = latest?.delta ?: 0f
                        SyncResult(
                            isSuccess = false,
                            usedFallback = true,
                            shouldTriggerRisingAlert = false,
                            latestPoint = latest,
                            risingDelta = delta,
                            metadata = fallbackMeta
                        )
                    } else {
                        val latest = currentPoints.lastOrNull()
                        val delta = latest?.delta ?: 0f
                        SyncResult(
                            isSuccess = false,
                            usedFallback = true,
                            shouldTriggerRisingAlert = false,
                            latestPoint = latest,
                            risingDelta = delta,
                            metadata = currentMeta
                        )
                    }
                } catch (_: Exception) {
                    SyncResult(
                        isSuccess = false,
                        usedFallback = true,
                        shouldTriggerRisingAlert = false,
                        latestPoint = null,
                        risingDelta = 0f,
                        metadata = null
                    )
                }
            }
        }
    }

    private suspend fun saveDtoToDatabase(
        dto: WaterLevelDto,
        stationId: String,
        isFallback: Boolean
    ): Pair<StationMetadataEntity, List<WaterPointEntity>> {
        val (metadata, points) = parseDto(dto, stationId, isFallback)
        if (points.isNotEmpty()) {
            dao.replaceStationData(
                stationId = stationId,
                metadata = metadata,
                points = points
            )
        }
        return metadata to points
    }

    private fun extractLastValidFloat(csv: String?, defaultVal: Float): Float {
        if (csv.isNullOrBlank()) return defaultVal
        var lastValid = defaultVal
        csv.splitToSequence(',').forEach { token ->
            val parsed = token.trim().toFloatOrNull()
            if (parsed != null && !parsed.isNaN() && !parsed.isInfinite()) {
                lastValid = parsed
            }
        }
        return lastValid
    }

    fun parseDto(
        dto: WaterLevelDto,
        stationId: String = DEFAULT_STATION_ID,
        isFallback: Boolean = false
    ): Pair<StationMetadataEntity, List<WaterPointEntity>> {
        val now = System.currentTimeMillis()
        val effectiveStationId = dto.stationId?.trim()?.takeIf { it.isNotEmpty() } ?: stationId

        val rawLabels = (dto.labels ?: "")
            .split(',')
            .map { it.trim() }

        val rawValues = (dto.value ?: "")
            .split(',')
            .map { it.trim().toFloatOrNull() }

        val defaultAlarm1 = extractLastValidFloat(dto.baoDong1, 3.5f)
        val defaultAlarm2 = extractLastValidFloat(dto.baoDong2, 5.0f)
        val defaultAlarm3 = extractLastValidFloat(dto.baoDong3, 6.5f)
        val defaultHist = extractLastValidFloat(dto.historicalFloodValue, 8.76f)

        val validPairs = rawLabels.zip(rawValues).mapNotNull { (label, level) ->
            if (label.isNotEmpty() && level != null && !level.isNaN() && !level.isInfinite()) {
                label to level
            } else {
                null
            }
        }

        val allPointsWithDelta = validPairs.mapIndexed { index, (label, level) ->
            val prevLevel = if (index > 0) validPairs[index - 1].second else level
            val rawDelta = level - prevLevel
            val cleanDelta = if (abs(rawDelta) < 0.0001f) 0f else rawDelta
            Triple(label, level, cleanDelta)
        }

        val last24HoursWindow = if (allPointsWithDelta.size > MAX_HOURS_WINDOW) {
            allPointsWithDelta.takeLast(MAX_HOURS_WINDOW)
        } else {
            allPointsWithDelta
        }

        val entities = last24HoursWindow.mapIndexed { idx, (label, level, delta) ->
            WaterPointEntity(
                stationId = effectiveStationId,
                pointIndex = idx,
                timeLabel = label,
                waterLevel = level,
                delta = delta,
                alarmLevel1 = defaultAlarm1,
                alarmLevel2 = defaultAlarm2,
                alarmLevel3 = defaultAlarm3,
                historicalFloodLevel = defaultHist,
                updatedAtMillis = now
            )
        }

        val metadata = StationMetadataEntity(
            stationId = effectiveStationId,
            stationName = dto.nameVn?.trim()?.takeIf { it.isNotEmpty() } ?: "Trà Khúc",
            riverName = dto.riverName?.trim()?.takeIf { it.isNotEmpty() } ?: "Trà Khúc",
            provinceName = dto.provinceName?.trim()?.takeIf { it.isNotEmpty() } ?: "Quảng Ngãi",
            source = dto.source?.trim()?.takeIf { it.isNotEmpty() } ?: "KTTV",
            sourceSet = dto.sourceSet?.trim()?.takeIf { it.isNotEmpty() } ?: "Water",
            historicalFloodYear = dto.historicalFloodYear?.trim()?.takeIf { it.isNotEmpty() } ?: "2013",
            alarmLevel1 = defaultAlarm1,
            alarmLevel2 = defaultAlarm2,
            alarmLevel3 = defaultAlarm3,
            historicalFloodLevel = defaultHist,
            lastUpdatedMillis = now,
            isFallbackData = isFallback
        )

        return metadata to entities
    }

    companion object {
        const val DEFAULT_STATION_ID = "71540"
        const val DEFAULT_SOURCE = "Water"
        const val DEFAULT_TIME_SELECT = "1"
        const val MAX_HOURS_WINDOW = 24

        fun createRealFallbackDto(stationId: String = DEFAULT_STATION_ID): WaterLevelDto {
            return WaterLevelDto(
                stationId = stationId,
                nameVn = "Trà Khúc",
                riverName = "Trà Khúc",
                provinceName = "Quảng Ngãi",
                districtName = null,
                source = "KTTV",
                sourceSet = "Water",
                historicalFloodYear = "2013",
                labels = "0h 10/11,1h 10/11,2h 10/11,3h 10/11,4h 10/11,5h 10/11,6h 10/11,7h 10/11,8h 10/11,9h 10/11,10h 10/11,11h 10/11,12h 10/11,13h 10/11,15h 10/11,16h 10/11,17h 10/11,20h 10/11,21h 10/11,22h 10/11,23h 10/11,0h 11/11,1h 11/11,2h 11/11,5h 11/11,6h 11/11,7h 11/11,8h 11/11,9h 11/11,10h 11/11,11h 11/11",
                value = "0.78,0.66,0.53,0.42,0.33,0.28,0.26,0.38,0.55,0.74,0.84,0.99,1.12,1.3,1.65,1.83,2.15,3.18,3.63,4.04,4.36,4.56,4.75,4.96,5.65,5.74,5.81,5.99,6.17,6.29,6.3",
                baoDong1 = "3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5,3.5",
                baoDong2 = "5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5",
                baoDong3 = "6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5,6.5",
                historicalFloodValue = "8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76,8.76"
            )
        }

        fun createFallbackNewsArticles(): List<NewsArticleEntity> {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", NchmfScraper.LOCALE_VI)
            val dateFmt = SimpleDateFormat("dd/MM/yyyy", NchmfScraper.LOCALE_VI)
            val nowCal = Calendar.getInstance()
            val todayStr = dateFmt.format(nowCal.time)
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayStr = dateFmt.format(yesterdayCal.time)

            val pub1 = "$todayStr 15:30"
            val pub2 = "$todayStr 09:15"
            val pub3 = "$yesterdayStr 21:00"
            val pub4 = "$yesterdayStr 14:30"

            val ts1 = runCatching { sdf.parse(pub1)?.time }.getOrNull() ?: System.currentTimeMillis()
            val ts2 = runCatching { sdf.parse(pub2)?.time }.getOrNull() ?: (ts1 - 6 * 3600_000L)
            val ts3 = runCatching { sdf.parse(pub3)?.time }.getOrNull() ?: (ts1 - 18 * 3600_000L)
            val ts4 = runCatching { sdf.parse(pub4)?.time }.getOrNull() ?: (ts1 - 25 * 3600_000L)

            return listOf(
                NewsArticleEntity(
                    url = "https://www.nchmf.gov.vn/kttv/vi-VN/1/tin-lu-tren-song-tra-khuc-va-cac-song-quang-ngai-post55154.html",
                    title = "TIN LŨ TRÊN SÔNG TRÀ KHÚC VÀ CÁC SÔNG Ở QUẢNG NGÃI, TIN CẢNH BÁO LŨ TRÊN CÁC SÔNG TỪ THỪA THIÊN HUẾ ĐẾN BÌNH ĐỊNH",
                    pubDateStr = pub1,
                    timestampMillis = ts1,
                    isHighlight = true,
                    cachedContent = """
                        1. HIỆN TRẠNG DIỄN BIẾN LŨ
                        Hiện nay, mực nước lũ trên sông Trà Khúc (Quảng Ngãi) đang lên nhanh. Mực nước lúc 15h00 tại trạm Trà Khúc (Mã trạm: 71540) là 6,30m, trên mức Báo động 2 (BĐ2: 5,00m) là 1,30m và dưới mức Báo động 3 (BĐ3: 6,50m) là 0,20m.

                        2. DỰ BÁO
                        ✅ Trong 6 đến 12 giờ tới: Lũ trên sông Trà Khúc tiếp tục lên và có khả năng đạt đỉnh ở mức xấp xỉ hoặc trên mức Báo động 3 (6,50m), sau đó biến đổi chậm theo điều tiết hồ chứa thượng nguồn.
                        ✅ Trong 12 đến 24 giờ tiếp theo: Mực nước trên sông Trà Khúc tại trạm Trà Khúc dao động ở mức trên BĐ2 đến BĐ3.

                        3. CẢNH BÁO NGUY CƠ NGẬP LỤT VÀ CẤP ĐỘ RỦI RO THIÊN TAI
                        ⚠️ Nguy cơ cao xảy ra ngập lụt tại các vùng trũng thấp ven sông Trà Khúc thuộc thành phố Quảng Ngãi, huyện Sơn Tịnh, Tư Nghĩa; lũ quét trên các sông, suối nhỏ và sạt lở đất trên sườn dốc ở khu vực vùng núi tỉnh Quảng Ngãi (Trà Bồng, Sơn Hà, Sơn Tây, Ba Tơ, Minh Long).
                        ⚠️ Cảnh báo cấp độ rủi ro thiên tai do lũ: Cấp 2 - Cấp 3.
                    """.trimIndent()
                ),
                NewsArticleEntity(
                    url = "https://www.nchmf.gov.vn/kttv/vi-VN/1/tin-canh-bao-lu-quet-sat-lo-dat-khu-vuc-trung-bo-post55148.html",
                    title = "TIN CẢNH BÁO LŨ QUÉT, SẠT LỞ ĐẤT, SỤT LÚN ĐẤT DO MƯA LŨ HOẶC DÒNG CHẢY TRÊN KHU VỰC CÁC TỈNH TỪ ĐÀ NẴNG ĐẾN QUẢNG NGÃI",
                    pubDateStr = pub2,
                    timestampMillis = ts2,
                    isHighlight = true,
                    cachedContent = """
                        1. HIỆN TRẠNG MƯA LỚN
                        Trong 12 giờ qua, khu vực các tỉnh từ Đà Nẵng đến Quảng Ngãi đã có mưa vừa, mưa to, có nơi mưa rất to như: Trà Phú (Quảng Ngãi) 168mm, Giá Vực (Quảng Ngãi) 154mm, Sơn Giang 142mm.

                        2. CẢNH BÁO MƯA TRONG THỜI GIAN TỚI
                        ✅ Trong 6 giờ tới, khu vực tỉnh Quảng Ngãi và Quảng Nam tiếp tục có mưa với lượng mưa tích lũy phổ biến từ 40-80mm, có nơi trên 120mm.

                        3. CẢNH BÁO NGUY CƠ
                        ⚠️ Nguy cơ xảy ra lũ quét trên các sông, suối nhỏ vùng núi; sạt lở đất trên sườn dốc tại các huyện vùng núi tỉnh Quảng Ngãi (Trà Bồng, Sơn Hà, Sơn Tây, Minh Long, Ba Tơ) và mực nước sông Trà Khúc tiếp tục duy trì ở mức cao trên Báo động 2.
                    """.trimIndent()
                ),
                NewsArticleEntity(
                    url = "https://www.nchmf.gov.vn/kttv/vi-VN/1/tin-lu-tren-cac-song-tu-quang-binh-den-phu-yen-post55139.html",
                    title = "TIN CẢNH BÁO LŨ TRÊN CÁC SÔNG TỪ QUẢNG TRỊ ĐẾN KHÁNH HÒA VÀ KHU VỰC BẮC TÂY NGUYÊN",
                    pubDateStr = pub3,
                    timestampMillis = ts3,
                    isHighlight = false,
                    cachedContent = """
                        1. HIỆN TRẠNG
                        Mực nước trên các sông từ Quảng Trị đến Khánh Hòa có dao động và đang có xu thế lên do ảnh hưởng của đợt mưa lớn diện rộng ở Trung Bộ.

                        2. DỰ BÁO
                        ✅ Từ đêm nay đến ngày mai, trên các sông từ Thừa Thiên Huế đến Bình Định (bao gồm sông Trà Khúc, sông Vệ tại Quảng Ngãi) xuất hiện một đợt lũ. Đỉnh lũ trên các sông chính khả năng lên mức BĐ2-BĐ3.

                        3. CẢNH BÁO
                        ⚠️ Đề phòng nguy cơ ngập úng tại các vùng trũng thấp ven sông và khu đô thị tại các tỉnh Trung Trung Bộ.
                    """.trimIndent()
                ),
                NewsArticleEntity(
                    url = "https://www.nchmf.gov.vn/kttv/vi-VN/1/ban-tin-du-bao-thuy-van-nguy-hiem-trung-bo-post55130.html",
                    title = "TIN LŨ KHẨN CẤP TRÊN SÔNG TRÀ KHÚC, SÔNG VỆ VÀ CẢNH BÁO NGẬP LỤT TẠI TỈNH QUẢNG NGÃI",
                    pubDateStr = pub4,
                    timestampMillis = ts4,
                    isHighlight = true,
                    cachedContent = """
                        1. HIỆN TRẠNG
                        Do mưa lớn tập trung ở thượng nguồn, mực nước sông Trà Khúc tại trạm Trà Khúc (Mã: 71540) đang lên nhanh từng giờ, vượt mức Báo động 1 và tiến sát Báo động 2.

                        2. DỰ BÁO
                        ✅ Trong 12 giờ tới, mực nước sông Trà Khúc tại trạm Trà Khúc tiếp tục tăng lên mức 5,60m - 6,30m (trên mức BĐ2 từ 0,60m đến 1,30m).

                        3. CẢNH BÁO
                        ⚠️ Các địa phương ven sông Trà Khúc chủ động phương án đảm bảo an toàn công trình đê điều và dân cư vùng bãi bồi ven sông.
                    """.trimIndent()
                )
            )
        }
    }
}
