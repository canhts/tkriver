package com.example.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.forecast.WaterLevelForecastEngine
import com.example.data.forecast.WaterLevelPrediction
import com.example.data.local.NewsArticleEntity
import com.example.data.local.StationMetadataEntity
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.WaterPointEntity
import com.example.data.repository.WaterLevelRepository
import com.example.data.repository.WeatherData
import com.example.data.repository.WeatherRepository
import com.example.ui.theme.AppColorPalette
import com.example.worker.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainNavSection(val title: String) {
    WATER_LEVEL("Mực nước"),
    FORECAST("Bản tin"),
    WEATHER("Thời tiết")
}

enum class DisplayMode(val title: String) {
    CHART("Biểu đồ"),
    TABLE("Số liệu")
}

enum class ThemeMode(val label: String) {
    SYSTEM("Theo hệ thống"),
    LIGHT("Sáng"),
    DARK("Tối")
}

@Immutable
data class WeatherUiState(
    val weatherData: WeatherData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class WaterLevelUiState(
    val stationMetadata: StationMetadataEntity = StationMetadataEntity(),
    val pointsChronological: List<WaterPointEntity> = emptyList(),
    val pointsReverseChronological: List<WaterPointEntity> = emptyList(),
    val latestPoint: WaterPointEntity? = null,
    val waterLevelPrediction: WaterLevelPrediction? = null,
    val weatherState: WeatherUiState = WeatherUiState(),
    val newsArticles: List<NewsArticleEntity> = emptyList(),
    val isNewsLoading: Boolean = false,
    val selectedArticle: NewsArticleEntity? = null,
    val selectedArticleContent: String? = null,
    val isArticleContentLoading: Boolean = false,
    val selectedArticleError: String? = null,
    val selectedNavSection: MainNavSection = MainNavSection.WATER_LEVEL,
    val selectedDisplayMode: DisplayMode = DisplayMode.CHART,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.OCEAN_BLUE,
    val isPushNotificationEnabled: Boolean = true,
    val isSettingsSheetOpen: Boolean = false,
    val selectedChartIndex: Int? = null,
    val isRefreshing: Boolean = false,
    val statusBannerMessage: String? = null,
    val isOfflineOrFallback: Boolean = false
)

@Immutable
private data class PrecomputedDomainSnapshot(
    val metadata: StationMetadataEntity,
    val pointsChronological: List<WaterPointEntity>,
    val pointsReverseChronological: List<WaterPointEntity>,
    val latestPoint: WaterPointEntity?,
    val sortedArticles: List<NewsArticleEntity>,
    val articleByUrl: Map<String, NewsArticleEntity>,
    val prediction: WaterLevelPrediction
)

private data class LocalControlState(
    val selectedNavSection: MainNavSection = MainNavSection.WATER_LEVEL,
    val selectedDisplayMode: DisplayMode = DisplayMode.CHART,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.OCEAN_BLUE,
    val isPushNotificationEnabled: Boolean = true,
    val isSettingsSheetOpen: Boolean = false,
    val selectedChartIndex: Int? = null,
    val isRefreshing: Boolean = false,
    val isNewsLoading: Boolean = false,
    val selectedArticle: NewsArticleEntity? = null,
    val selectedArticleContent: String? = null,
    val isArticleContentLoading: Boolean = false,
    val selectedArticleError: String? = null,
    val weatherState: WeatherUiState = WeatherUiState(),
    val statusBannerMessage: String? = null
)

class WaterLevelViewModel(
    private val repository: WaterLevelRepository,
    private val weatherRepository: WeatherRepository,
    private val notificationHelper: NotificationHelper,
    private val settingsPreferences: UserSettingsPreferences,
    private val stationId: String = WaterLevelRepository.DEFAULT_STATION_ID
) : ViewModel() {

    private val initialThemeMode = settingsPreferences.getThemeMode()
    private val initialColorPalette = settingsPreferences.getColorPalette()
    private val initialPushNotificationEnabled = settingsPreferences.isPushNotificationEnabled()

    private val controlState = MutableStateFlow(
        LocalControlState(
            themeMode = initialThemeMode,
            colorPalette = initialColorPalette,
            isPushNotificationEnabled = initialPushNotificationEnabled
        )
    )

    // Tách riêng luồng tính toán dữ liệu cơ sở (sorting + mô hình dự báo thủy văn + HashMap O(1))
    // để khi người dùng bấm chuyển Tab, chọn mốc trên biểu đồ hay mở Cài đặt (controlState đổi),
    // hệ thống KHÔNG bao giờ phải sắp xếp lại danh sách hay tính lại dự báo 1 giờ tới.
    private val domainDataFlow = combine(
        repository.observeStationMetadata(stationId).distinctUntilChanged(),
        repository.observeWaterPoints(stationId).distinctUntilChanged(),
        repository.observeNewsArticles().distinctUntilChanged()
    ) { metadata, points, articles ->
        val effectiveMeta = metadata ?: StationMetadataEntity(stationId = stationId)
        val sortedAscending = points.sortedBy { it.pointIndex }
        val sortedDescending = sortedAscending.asReversed()
        val latest = sortedAscending.lastOrNull()
        val sortedArticles = articles.sortedByDescending { it.timestampMillis }
        val articleByUrl = sortedArticles.associateBy { it.url }

        val prediction = WaterLevelForecastEngine.computeNextHourForecast(
            points = sortedAscending,
            articles = sortedArticles,
            metadata = effectiveMeta
        )

        PrecomputedDomainSnapshot(
            metadata = effectiveMeta,
            pointsChronological = sortedAscending,
            pointsReverseChronological = sortedDescending,
            latestPoint = latest,
            sortedArticles = sortedArticles,
            articleByUrl = articleByUrl,
            prediction = prediction
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<WaterLevelUiState> = combine(
        domainDataFlow,
        controlState
    ) { domain, controls ->
        val validSelectedIdx = when {
            domain.pointsChronological.isEmpty() -> null
            controls.selectedChartIndex != null && controls.selectedChartIndex in domain.pointsChronological.indices ->
                controls.selectedChartIndex
            else -> null
        }

        // Tìm bài viết đang mở với độ phức tạp O(1) qua Map thay vì O(n)
        val updatedSelectedArticle = controls.selectedArticle?.let { currentSelected ->
            domain.articleByUrl[currentSelected.url] ?: currentSelected
        }

        WaterLevelUiState(
            stationMetadata = domain.metadata,
            pointsChronological = domain.pointsChronological,
            pointsReverseChronological = domain.pointsReverseChronological,
            latestPoint = domain.latestPoint,
            waterLevelPrediction = domain.prediction,
            weatherState = controls.weatherState,
            newsArticles = domain.sortedArticles,
            isNewsLoading = controls.isNewsLoading,
            selectedArticle = updatedSelectedArticle,
            selectedArticleContent = controls.selectedArticleContent ?: updatedSelectedArticle?.cachedContent,
            isArticleContentLoading = controls.isArticleContentLoading,
            selectedArticleError = controls.selectedArticleError,
            selectedNavSection = controls.selectedNavSection,
            selectedDisplayMode = controls.selectedDisplayMode,
            themeMode = controls.themeMode,
            colorPalette = controls.colorPalette,
            isPushNotificationEnabled = controls.isPushNotificationEnabled,
            isSettingsSheetOpen = controls.isSettingsSheetOpen,
            selectedChartIndex = validSelectedIdx,
            isRefreshing = controls.isRefreshing,
            statusBannerMessage = controls.statusBannerMessage,
            isOfflineOrFallback = domain.metadata.isFallbackData
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WaterLevelUiState(
            themeMode = initialThemeMode,
            colorPalette = initialColorPalette,
            isPushNotificationEnabled = initialPushNotificationEnabled,
            isRefreshing = true
        )
    )

    private var waterRefreshJob: Job? = null
    private var newsRefreshJob: Job? = null
    private var articleFetchJob: Job? = null
    private var weatherFetchJob: Job? = null

    init {
        initializeOnAppLaunch()
    }

    private fun initializeOnAppLaunch() {
        viewModelScope.launch {
            repository.ensureSeededIfEmpty(stationId)
            // Tự động làm mới cả trang Mực nước, Bản tin và Thời tiết khi mở app
            refreshData(showUserFeedback = false)
            refreshNewsArticles(showUserFeedback = false)
            refreshWeather(showUserFeedback = false)
        }
    }

    fun selectNavSection(section: MainNavSection) {
        if (controlState.value.selectedNavSection == section) return
        controlState.update { it.copy(selectedNavSection = section) }
        if (section == MainNavSection.WEATHER && controlState.value.weatherState.weatherData == null) {
            refreshWeather(showUserFeedback = false)
        }
    }

    fun refreshWeather(showUserFeedback: Boolean = true) {
        if (weatherFetchJob?.isActive == true && !showUserFeedback) return
        weatherFetchJob?.cancel()
        controlState.update {
            it.copy(
                weatherState = it.weatherState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            )
        }

        weatherFetchJob = viewModelScope.launch {
            val result = weatherRepository.fetchWeather(forceRefresh = showUserFeedback)
            result.onSuccess { weatherData ->
                controlState.update {
                    it.copy(
                        weatherState = it.weatherState.copy(
                            weatherData = weatherData,
                            isLoading = false,
                            errorMessage = null
                        ),
                        statusBannerMessage = if (showUserFeedback) "Đã cập nhật thời tiết tại ${weatherData.locationName}" else it.statusBannerMessage
                    )
                }
            }.onFailure { err ->
                controlState.update {
                    it.copy(
                        weatherState = it.weatherState.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Không thể kết nối dịch vụ thời tiết Open-Meteo."
                        )
                    )
                }
            }
        }
    }

    fun selectDisplayMode(mode: DisplayMode) {
        if (controlState.value.selectedDisplayMode == mode) return
        controlState.update { it.copy(selectedDisplayMode = mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (controlState.value.themeMode == mode) return
        settingsPreferences.saveThemeMode(mode)
        controlState.update { it.copy(themeMode = mode) }
    }

    fun selectColorPalette(palette: AppColorPalette) {
        if (controlState.value.colorPalette == palette) return
        settingsPreferences.saveColorPalette(palette)
        controlState.update { it.copy(colorPalette = palette) }
    }

    fun setPushNotificationEnabled(enabled: Boolean) {
        if (controlState.value.isPushNotificationEnabled == enabled) return
        settingsPreferences.setPushNotificationEnabled(enabled)
        if (!enabled) {
            notificationHelper.cancelAllNotifications()
        }
        controlState.update {
            it.copy(
                isPushNotificationEnabled = enabled,
                statusBannerMessage = if (enabled) {
                    "Đã bật thông báo đẩy"
                } else {
                    "Đã tắt thông báo đẩy (vẫn tự cập nhật dữ liệu ngầm)"
                }
            )
        }
    }

    fun setSettingsSheetOpen(open: Boolean) {
        if (controlState.value.isSettingsSheetOpen == open) return
        controlState.update { it.copy(isSettingsSheetOpen = open) }
    }

    fun selectChartPoint(index: Int?) {
        if (controlState.value.selectedChartIndex == index) return
        controlState.update {
            it.copy(selectedChartIndex = index)
        }
    }

    fun openArticleReader(article: NewsArticleEntity) {
        if (controlState.value.selectedArticle?.url == article.url) return
        articleFetchJob?.cancel()
        val cached = article.cachedContent
        if (!cached.isNullOrBlank()) {
            controlState.update {
                it.copy(
                    selectedArticle = article,
                    selectedArticleContent = cached,
                    isArticleContentLoading = false,
                    selectedArticleError = null
                )
            }
            return
        }

        controlState.update {
            it.copy(
                selectedArticle = article,
                selectedArticleContent = null,
                isArticleContentLoading = true,
                selectedArticleError = null
            )
        }

        articleFetchJob = viewModelScope.launch {
            val result = repository.getOrFetchArticleContent(article)
            result.fold(
                onSuccess = { cleanText ->
                    controlState.update {
                        it.copy(
                            selectedArticleContent = cleanText,
                            isArticleContentLoading = false,
                            selectedArticleError = null
                        )
                    }
                },
                onFailure = { err ->
                    controlState.update {
                        it.copy(
                            isArticleContentLoading = false,
                            selectedArticleError = "Không thể tải chi tiết bản tin từ nchmf.gov.vn (${err.localizedMessage ?: "Lỗi kết nối"})."
                        )
                    }
                }
            )
        }
    }

    fun closeArticleReader() {
        articleFetchJob?.cancel()
        controlState.update {
            it.copy(
                selectedArticle = null,
                selectedArticleContent = null,
                isArticleContentLoading = false,
                selectedArticleError = null
            )
        }
    }

    fun refreshNewsArticles(showUserFeedback: Boolean = false) {
        if (newsRefreshJob?.isActive == true) return

        newsRefreshJob = viewModelScope.launch {
            controlState.update {
                it.copy(
                    isNewsLoading = true,
                    statusBannerMessage = if (showUserFeedback) null else it.statusBannerMessage
                )
            }
            val bulletinResult = repository.syncNewsArticles()
            if (bulletinResult.newHighlightArticlesToNotify.isNotEmpty()) {
                bulletinResult.newHighlightArticlesToNotify
                    .sortedByDescending { it.timestampMillis }
                    .take(2)
                    .forEach { newBulletin ->
                        notificationHelper.showNewBulletinNotification(newBulletin)
                    }
            }
            controlState.update {
                it.copy(
                    isNewsLoading = false,
                    statusBannerMessage = if (showUserFeedback) "Đã cập nhật bản tin mới nhất" else it.statusBannerMessage
                )
            }
        }
    }

    fun dismissBannerMessage() {
        controlState.update { it.copy(statusBannerMessage = null) }
    }

    fun refreshData(showUserFeedback: Boolean = true) {
        if (waterRefreshJob?.isActive == true) return

        waterRefreshJob = viewModelScope.launch {
            controlState.update {
                it.copy(
                    isRefreshing = true,
                    statusBannerMessage = null
                )
            }

            val result = repository.syncWaterLevel(stationId = stationId)
            val latest = result.latestPoint
            val meta = result.metadata

            if (result.shouldTriggerRisingAlert && latest != null && meta != null) {
                notificationHelper.showRisingWaterNotification(
                    riverName = meta.riverName,
                    timeLabel = latest.timeLabel,
                    currentLevel = latest.waterLevel,
                    delta = result.risingDelta,
                    alarm1 = meta.alarmLevel1,
                    alarm2 = meta.alarmLevel2,
                    alarm3 = meta.alarmLevel3
                )
            }

            val feedbackMsg = if (showUserFeedback) {
                "Đã cập nhật dữ liệu mới nhất"
            } else {
                null
            }

            controlState.update {
                it.copy(
                    isRefreshing = false,
                    selectedChartIndex = null,
                    statusBannerMessage = feedbackMsg
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: WaterLevelRepository,
            weatherRepository: WeatherRepository,
            notificationHelper: NotificationHelper,
            settingsPreferences: UserSettingsPreferences,
            stationId: String = WaterLevelRepository.DEFAULT_STATION_ID
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(WaterLevelViewModel::class.java)) {
                    return WaterLevelViewModel(
                        repository = repository,
                        weatherRepository = weatherRepository,
                        notificationHelper = notificationHelper,
                        settingsPreferences = settingsPreferences,
                        stationId = stationId
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
