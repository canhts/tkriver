package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.forecast.WaterLevelPrediction
import com.example.data.local.NewsArticleEntity
import com.example.data.local.StationMetadataEntity
import com.example.data.local.WaterPointEntity
import com.example.data.remote.NchmfScraper
import com.example.ui.DisplayMode
import com.example.ui.MainNavSection
import com.example.ui.ThemeMode
import com.example.ui.WaterLevelForecastCard
import com.example.ui.WaterLevelUiState
import com.example.ui.WaterLevelViewModel
import com.example.ui.WeatherSection
import com.example.ui.theme.AlarmLevel1Green
import com.example.ui.theme.AlarmLevel2Orange
import com.example.ui.theme.AlarmLevel3Red
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.DeltaFallingBlue
import com.example.ui.theme.DeltaRisingRed
import com.example.ui.theme.DeltaStableGray
import com.example.ui.theme.HistoricalFloodPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WaterLineBlue
import com.example.worker.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

private val WHITESPACE_REGEX = Regex("\\s+")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TraKhucWaterApp
        val appContainer = app.container
        // Khi người dùng chủ động mở ứng dụng, khôi phục trạng thái chạy ngầm (nếu trước đó đã tắt hoàn toàn)
        if (appContainer.settingsPreferences.isAppForceStopped()) {
            appContainer.settingsPreferences.setAppForceStopped(false)
        }
        app.ensureBackgroundSyncRunning()

        val openBulletinFromNotification = intent?.getBooleanExtra(
            NotificationHelper.EXTRA_OPEN_BULLETIN_TAB,
            false
        ) == true

        setContent {
            val viewModel: WaterLevelViewModel = viewModel(
                factory = WaterLevelViewModel.provideFactory(
                    repository = appContainer.repository,
                    weatherRepository = appContainer.weatherRepository,
                    notificationHelper = appContainer.notificationHelper,
                    settingsPreferences = appContainer.settingsPreferences
                )
            )

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDarkTheme = when (uiState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            MyApplicationTheme(
                darkTheme = effectiveDarkTheme,
                colorPalette = uiState.colorPalette
            ) {
                val context = LocalContext.current
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        viewModel.refreshWeather(showUserFeedback = true)
                    }
                }

                LaunchedEffect(Unit) {
                    if (openBulletinFromNotification) {
                        viewModel.selectNavSection(MainNavSection.FORECAST)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && uiState.isPushNotificationEnabled) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                BackHandler(enabled = uiState.selectedNavSection != MainNavSection.WATER_LEVEL) {
                    viewModel.selectNavSection(MainNavSection.WATER_LEVEL)
                }

                TraKhucWaterScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshData(showUserFeedback = true) },
                    onRefreshNews = { viewModel.refreshNewsArticles(showUserFeedback = true) },
                    onRefreshWeather = { viewModel.refreshWeather(showUserFeedback = true) },
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onSelectMode = { viewModel.selectDisplayMode(it) },
                    onSelectNavSection = { viewModel.selectNavSection(it) },
                    onOpenSettings = { viewModel.setSettingsSheetOpen(true) },
                    onDismissSettings = { viewModel.setSettingsSheetOpen(false) },
                    onSelectThemeMode = { viewModel.setThemeMode(it) },
                    onSelectColorPalette = { viewModel.selectColorPalette(it) },
                    onTogglePushNotification = { enabled ->
                        viewModel.setPushNotificationEnabled(enabled)
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    onConfirmForceStopApp = {
                        viewModel.setSettingsSheetOpen(false)
                        app.forceStopApplication()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            finishAndRemoveTask()
                        } else {
                            finishAffinity()
                        }
                    },
                    onSelectChartPoint = { viewModel.selectChartPoint(it) },
                    onSelectArticle = { viewModel.openArticleReader(it) },
                    onDismissArticle = { viewModel.closeArticleReader() },
                    onDismissBanner = { viewModel.dismissBannerMessage() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraKhucWaterScreen(
    uiState: WaterLevelUiState,
    onRefresh: () -> Unit,
    onRefreshNews: () -> Unit,
    onRefreshWeather: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onSelectMode: (DisplayMode) -> Unit,
    onSelectNavSection: (MainNavSection) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectColorPalette: (AppColorPalette) -> Unit,
    onTogglePushNotification: (Boolean) -> Unit,
    onConfirmForceStopApp: () -> Unit,
    onSelectChartPoint: (Int?) -> Unit,
    onSelectArticle: (NewsArticleEntity) -> Unit,
    onDismissArticle: () -> Unit,
    onDismissBanner: () -> Unit
) {
    LaunchedEffect(uiState.statusBannerMessage, uiState.isRefreshing) {
        if (!uiState.statusBannerMessage.isNullOrBlank() && !uiState.isRefreshing) {
            kotlinx.coroutines.delay(2500L)
            onDismissBanner()
        }
    }

    var forecastHeaderBottomPx by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Nội dung cuộn xuyên mượt mà phía dưới thanh menu viên nhộng nổi
            // kèm padding-bottom 108.dp để nội dung cuối trang luôn nằm hoàn toàn phía trên thanh menu
            AnimatedContent(
                targetState = uiState.selectedNavSection,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) { fullWidth -> direction * (fullWidth / 5) } + fadeIn(
                        animationSpec = tween(durationMillis = 260)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    ) { fullWidth -> -direction * (fullWidth / 5) } + fadeOut(
                        animationSpec = tween(durationMillis = 180)
                    ))
                },
                label = "main_nav_section_transition",
                modifier = Modifier.fillMaxSize()
            ) { currentNavSection ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (currentNavSection) {
                        MainNavSection.WATER_LEVEL -> {
                            PullToRefreshBox(
                                isRefreshing = uiState.isRefreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 760.dp)
                                    .padding(horizontal = 14.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("water_level_page_scroll"),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 108.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item(key = "overview_card_item") {
                                        OverviewCard(
                                            metadata = uiState.stationMetadata,
                                            latestPoint = uiState.latestPoint,
                                            pointsChronological = uiState.pointsChronological,
                                            waterLevelPrediction = uiState.waterLevelPrediction
                                        )
                                    }

                                    item(key = "display_mode_tabs_item") {
                                        BorderlessTabBar(
                                            selectedMode = uiState.selectedDisplayMode,
                                            onSelectMode = onSelectMode
                                        )
                                    }

                                    item(key = "main_content_section_item") {
                                        AnimatedContent(
                                            targetState = uiState.selectedDisplayMode,
                                            transitionSpec = {
                                                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                                (slideInHorizontally(
                                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                                ) { fullWidth -> direction * (fullWidth / 4) } + fadeIn(
                                                    animationSpec = tween(durationMillis = 240)
                                                )) togetherWith (slideOutHorizontally(
                                                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                                                ) { fullWidth -> -direction * (fullWidth / 4) } + fadeOut(
                                                    animationSpec = tween(durationMillis = 160)
                                                ))
                                            },
                                            label = "water_level_display_mode_transition"
                                        ) { currentDisplayMode ->
                                            when (currentDisplayMode) {
                                                DisplayMode.CHART -> {
                                                    WaterLevelChartSection(
                                                        points = uiState.pointsChronological,
                                                        metadata = uiState.stationMetadata,
                                                        selectedIndex = uiState.selectedChartIndex,
                                                        onSelectIndex = onSelectChartPoint
                                                    )
                                                }

                                                DisplayMode.TABLE -> {
                                                    WaterLevelTableSection(
                                                        pointsDescending = uiState.pointsReverseChronological,
                                                        metadata = uiState.stationMetadata
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Yêu cầu 3: Tại trang Mực nước, ngay bên dưới khu vực biểu đồ, thêm thẻ Dự báo mực nước 1 giờ tới
                                    if (uiState.selectedDisplayMode == DisplayMode.CHART && uiState.waterLevelPrediction != null) {
                                        item(key = "water_level_forecast_card_item") {
                                            WaterLevelForecastCard(
                                                prediction = uiState.waterLevelPrediction
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        MainNavSection.FORECAST -> {
                            ForecastNewsSection(
                                articles = uiState.newsArticles,
                                isNewsLoading = uiState.isNewsLoading,
                                onRefreshNews = onRefreshNews,
                                onSelectArticle = onSelectArticle,
                                onHeaderBottomMeasured = { bottomPx ->
                                    forecastHeaderBottomPx = bottomPx
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 760.dp)
                                    .padding(horizontal = 14.dp)
                            )
                        }

                        MainNavSection.WEATHER -> {
                            WeatherSection(
                                uiState = uiState,
                                onRefreshWeather = onRefreshWeather,
                                onRequestLocationPermission = onRequestLocationPermission,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 760.dp)
                                    .padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }

            // Borderless capsule popup (Popup dạng viên nhộng không viền) nằm phía trên thanh điều hướng nổi
            AnimatedVisibility(
                visible = !uiState.statusBannerMessage.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 94.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.testTag("capsule_status_popup")
                ) {
                    Text(
                        text = uiState.statusBannerMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                    )
                }
            }

            // Thanh điều hướng nổi 2 khối tách biệt căn giữa chuẩn MD3
            FloatingPillNavigationBar(
                selectedSection = uiState.selectedNavSection,
                onSelectSection = onSelectNavSection,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
        }

        // Khung đọc bài viết chi tiết (ModalBottomSheet chuẩn Material Design 3)
        if (uiState.selectedArticle != null) {
            ArticleReaderModalBottomSheet(
                article = uiState.selectedArticle,
                content = uiState.selectedArticleContent,
                isLoading = uiState.isArticleContentLoading,
                errorMessage = uiState.selectedArticleError,
                headerBottomInWindowPx = forecastHeaderBottomPx,
                onDismiss = onDismissArticle
            )
        }

        // Bottom Sheet cho mục "Cài đặt" chuẩn MD3
        if (uiState.isSettingsSheetOpen) {
            SettingsModalBottomSheet(
                themeMode = uiState.themeMode,
                colorPalette = uiState.colorPalette,
                isPushNotificationEnabled = uiState.isPushNotificationEnabled,
                metadata = uiState.stationMetadata,
                onSelectThemeMode = onSelectThemeMode,
                onSelectColorPalette = onSelectColorPalette,
                onTogglePushNotification = onTogglePushNotification,
                onConfirmForceStopApp = onConfirmForceStopApp,
                onDismiss = onDismissSettings
            )
        }
    }
}

@Composable
fun FloatingPillNavigationBar(
    selectedSection: MainNavSection,
    onSelectSection: (MainNavSection) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navClusterHeight = 58.dp
    val borderStrokeColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    val settingsInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.testTag("floating_pill_nav_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Khối bên trái (Hình viên nhộng - Pill shape): Chứa "Mực nước", "Bản tin", và "Thời tiết"
        // Luôn duy trì nền đang chọn của tab hiện tại kể cả khi mở Cài đặt
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .height(navClusterHeight)
                .border(
                    width = 0.8.dp,
                    color = borderStrokeColor,
                    shape = CircleShape
                )
                .testTag("floating_pill_left_block")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillNavItem(
                    label = "Mực nước",
                    icon = Icons.Default.WaterDrop,
                    isSelected = selectedSection == MainNavSection.WATER_LEVEL,
                    onClick = { onSelectSection(MainNavSection.WATER_LEVEL) },
                    testTag = "nav_item_water_level"
                )

                PillNavItem(
                    label = "Bản tin",
                    icon = Icons.Default.Newspaper,
                    isSelected = selectedSection == MainNavSection.FORECAST,
                    onClick = { onSelectSection(MainNavSection.FORECAST) },
                    testTag = "nav_item_forecast"
                )

                PillNavItem(
                    label = "Thời tiết",
                    icon = Icons.Default.WbSunny,
                    isSelected = selectedSection == MainNavSection.WEATHER,
                    onClick = { onSelectSection(MainNavSection.WEATHER) },
                    testTag = "nav_item_weather"
                )
            }
        }

        // 2. Khối bên phải (Nút "Cài đặt" độc lập hình tròn hoàn hảo, không có nền tròn kích hoạt bên trong)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .size(navClusterHeight)
                .border(
                    width = 0.8.dp,
                    color = borderStrokeColor,
                    shape = CircleShape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = settingsInteractionSource,
                        indication = ripple(),
                        onClick = onOpenSettings
                    )
                    .testTag("nav_item_settings"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PillNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeIndicatorColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "pill_nav_indicator"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "pill_nav_content"
    )
    val itemScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_nav_scale"
    )
    val horizontalPad by animateDpAsState(
        targetValue = if (isSelected) 15.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pill_nav_pad"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clip(CircleShape)
            .background(activeIndicatorColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .padding(horizontal = horizontalPad)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(220)) +
                    expandHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                exit = fadeOut(animationSpec = tween(150)) +
                    shrinkHorizontally(animationSpec = tween(180)) +
                    scaleOut(targetScale = 0.7f, animationSpec = tween(150))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastNewsSection(
    articles: List<NewsArticleEntity>,
    isNewsLoading: Boolean,
    onRefreshNews: () -> Unit,
    onSelectArticle: (NewsArticleEntity) -> Unit,
    onHeaderBottomMeasured: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val newestDayEpoch = remember(articles) {
        articles.maxOfOrNull { NchmfScraper.normalizeToDayStartMillis(it.timestampMillis) }
            ?: NchmfScraper.normalizeToDayStartMillis(System.currentTimeMillis())
    }

    PullToRefreshBox(
        isRefreshing = isNewsLoading,
        onRefresh = onRefreshNews,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("forecast_news_scroll"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tiêu đề trang bản tin bo góc 20.dp như các thẻ tin tức, cuộn cùng danh sách (đã bỏ nút refresh bấm)
            item(key = "forecast_header_item") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            val bottomPx = (coordinates.positionInWindow().y + coordinates.size.height).toInt()
                            if (bottomPx > 0) {
                                onHeaderBottomMeasured(bottomPx)
                            }
                        }
                        .testTag("forecast_header_card"),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Bản tin Thủy văn nguy hiểm",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Trung tâm Dự báo KTTV Quốc gia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (articles.isEmpty() && isNewsLoading) {
                item(key = "forecast_loading_item") {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Đang tải bản tin Thủy văn nguy hiểm...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (articles.isEmpty()) {
                item(key = "forecast_empty_item") {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbCloudy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Chưa có bản tin Thủy văn nguy hiểm.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = articles,
                    key = { it.url }
                ) { article ->
                    NewsCard(
                        article = article,
                        referenceLatestDayEpoch = newestDayEpoch,
                        onClick = { onSelectArticle(article) }
                    )
                }
            }
        }
    }
}

private fun formatRelativeArticleDate(
    pubDateStr: String,
    timestampMillis: Long,
    referenceLatestDayEpoch: Long
): String {
    val parts = pubDateStr.trim().split(WHITESPACE_REGEX)
    val datePart = parts.firstOrNull().orEmpty()
    val timePart = if (parts.size >= 2) parts.last() else ""
    val articleDayEpoch = NchmfScraper.normalizeToDayStartMillis(timestampMillis)
    val yesterdayDayEpoch = NchmfScraper.subtractOneDayMillis(referenceLatestDayEpoch)

    return when {
        articleDayEpoch >= referenceLatestDayEpoch && timePart.isNotEmpty() ->
            "Hôm nay - $timePart ($datePart)"
        articleDayEpoch == yesterdayDayEpoch && timePart.isNotEmpty() ->
            "Hôm qua - $timePart ($datePart)"
        timePart.isNotEmpty() && datePart.isNotEmpty() ->
            "$timePart ($datePart)"
        else -> pubDateStr
    }
}

@Composable
fun NewsCard(
    article: NewsArticleEntity,
    referenceLatestDayEpoch: Long,
    onClick: () -> Unit
) {
    val formattedDateLabel = remember(article.pubDateStr, article.timestampMillis, referenceLatestDayEpoch) {
        formatRelativeArticleDate(
            pubDateStr = article.pubDateStr,
            timestampMillis = article.timestampMillis,
            referenceLatestDayEpoch = referenceLatestDayEpoch
        )
    }

    // Sử dụng màu nền đặc (100% opaque) chuẩn MD3 bằng lerp thay vì alpha trong suốt để không bị bóng đổ tạo khối trắng bên trong
    val cardBgColor = if (article.isHighlight) {
        androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.primaryContainer,
            0.55f
        )
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (article.isHighlight) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBgColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (article.isHighlight) 1.2.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag("news_article_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Phần 1 (Tiêu đề): Chữ đậm rõ ràng + nhãn nổi bật "Quảng Ngãi • Trà Khúc" nếu có từ khóa Trà Khúc / Quảng Ngãi
            if (article.isHighlight) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Quảng Ngãi • Trà Khúc",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Phần 2 (Ngày giờ bài đăng): Nằm ngay bên dưới kèm icon đồng hồ nhỏ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = formattedDateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderModalBottomSheet(
    article: NewsArticleEntity,
    content: String?,
    isLoading: Boolean,
    errorMessage: String?,
    headerBottomInWindowPx: Int,
    onDismiss: () -> Unit
) {
    // skipPartiallyExpanded = false để người dùng có thể kéo BottomSheet lên nhưng dừng lại ngay dưới tiêu đề Bản tin
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val safeDrawingTopPx = WindowInsets.safeDrawing.getTop(density)
    val sheetWindowInsetTopPx = BottomSheetDefaults.windowInsets.getTop(density)
    val extraGapPx = with(density) { 8.dp.roundToPx() }
    val fallbackHeaderBottomPx = safeDrawingTopPx + with(density) { 108.dp.roundToPx() }
    val targetTopYInWindowPx = if (headerBottomInWindowPx > 0) {
        headerBottomInWindowPx + extraGapPx
    } else {
        fallbackHeaderBottomPx + extraGapPx
    }
    val reservedTopClearancePx = (targetTopYInWindowPx - sheetWindowInsetTopPx).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        scrimColor = BottomSheetDefaults.ScrimColor,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("article_reader_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val maxAllowedHeight = (constraints.maxHeight - reservedTopClearancePx).coerceAtLeast(0)
                    val childConstraints = constraints.copy(
                        minHeight = maxAllowedHeight,
                        maxHeight = maxAllowedHeight
                    )
                    val placeable = measurable.measure(childConstraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (article.isHighlight) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Trọng điểm: Quảng Ngãi • Sông Trà Khúc",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Tiêu đề bài viết (headlineSmall, in đậm)
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Ngày giờ đăng bài viết
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Cập nhật: ${article.pubDateStr} • Nguồn: NCHMF",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when {
                isLoading && content.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Đang tải và làm sạch nội dung bản tin...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                !content.isNullOrBlank() -> {
                    val paragraphs = remember(content) {
                        val rawLines = content.split("\n\n")
                            .flatMap { it.lines() }
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        NchmfScraper.stripTrailingChiTietSection(rawLines)
                    }

                    val highlightBoxBgColor = androidx.compose.ui.graphics.lerp(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer,
                        0.5f
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        paragraphs.forEach { line ->
                            val upper = line.uppercase(NchmfScraper.LOCALE_VI)
                            val isSectionHeader = upper.startsWith("1.") ||
                                upper.startsWith("2.") ||
                                upper.startsWith("3.") ||
                                upper.startsWith("4.") ||
                                upper.startsWith("I.") ||
                                upper.startsWith("II.") ||
                                upper.startsWith("III.") ||
                                upper.contains("HIỆN TRẠNG") ||
                                upper.contains("DỰ BÁO") ||
                                upper.contains("CẢNH BÁO")

                            val mentionsTraKhuc = NchmfScraper.isTraKhucOrQuangNgai(line)

                            when {
                                isSectionHeader && line.length <= 95 -> {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 24.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                mentionsTraKhuc -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = highlightBoxBgColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 24.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }

                                else -> {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 24.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = errorMessage ?: "Không có nội dung chi tiết cho bản tin này.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsModalBottomSheet(
    themeMode: ThemeMode,
    colorPalette: AppColorPalette,
    isPushNotificationEnabled: Boolean,
    metadata: StationMetadataEntity,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectColorPalette: (AppColorPalette) -> Unit,
    onTogglePushNotification: (Boolean) -> Unit,
    onConfirmForceStopApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var showForceStopDialog by remember { mutableStateOf(false) }

    if (showForceStopDialog) {
        AlertDialog(
            onDismissRequest = { showForceStopDialog = false },
            title = {
                Text(
                    text = "Thoát ứng dụng?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Tắt hoàn toàn ứng dụng và dừng cập nhật, thông báo ngầm?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForceStopDialog = false
                        onConfirmForceStopApp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("confirm_force_stop_button")
                ) {
                    Text(
                        text = "Thoát",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForceStopDialog = false },
                    modifier = Modifier.testTag("cancel_force_stop_button")
                ) {
                    Text(
                        text = "Hủy",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            modifier = Modifier.testTag("force_stop_confirm_dialog")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        scrimColor = BottomSheetDefaults.ScrimColor,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("settings_modal_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    // Giới hạn chiều cao của Bottom Sheet Cài đặt bằng đúng 1/2 chiều cao ứng dụng
                    val halfAppHeightPx = (constraints.maxHeight / 2).coerceAtLeast(220)
                    val childConstraints = constraints.copy(
                        minHeight = halfAppHeightPx,
                        maxHeight = halfAppHeightPx
                    )
                    val placeable = measurable.measure(childConstraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cài đặt ứng dụng",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Nút Thoát gọn gàng (chỉ ghi "Thoát")
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
                        .clickable { showForceStopDialog = true }
                        .testTag("settings_force_stop_app_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Thoát",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Thoát",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1
                        )
                    }
                }
            }

            // 1. Chuyển chế độ giao diện (Theme Mode)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessAuto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Chế độ giao diện",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val themeOptions = listOf(
                            Triple(ThemeMode.LIGHT, "Sáng", Icons.Default.LightMode),
                            Triple(ThemeMode.DARK, "Tối", Icons.Default.DarkMode),
                            Triple(ThemeMode.SYSTEM, "Theo hệ thống", Icons.Default.BrightnessAuto)
                        )
                        themeOptions.forEach { (mode, label, icon) ->
                            val isSelected = themeMode == mode
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                label = "theme_mode_bg"
                            )
                            val fg by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                label = "theme_mode_fg"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable { onSelectThemeMode(mode) }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                                    .testTag("theme_option_${mode.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = fg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = fg,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Đổi màu chủ đạo (Color Scheme - 5 bảng màu chuẩn MD3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Màu chủ đạo (Material Design 3)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppColorPalette.entries.forEach { palette ->
                        val isSelected = colorPalette == palette
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 1.6.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable { onSelectColorPalette(palette) }
                                .testTag("palette_option_${palette.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(palette.seedColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = palette.displayName,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Bật / Tắt thông báo đẩy (Chỉ tắt thông báo đẩy, hệ thống cập nhật dữ liệu ngầm 4 mốc/giờ vẫn hoạt động)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_push_notification_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTogglePushNotification(!isPushNotificationEnabled) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPushNotificationEnabled) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = null,
                            tint = if (isPushNotificationEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Thông báo đẩy",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPushNotificationEnabled) {
                                    "Đang bật cảnh báo • Tự cập nhật lúc hh:00, hh:15, hh:30, hh:45"
                                } else {
                                    "Đã tắt thông báo đẩy • Vẫn tự cập nhật dữ liệu ngầm"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = isPushNotificationEnabled,
                        onCheckedChange = onTogglePushNotification,
                        modifier = Modifier.testTag("settings_push_notification_switch")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 4. Thông tin nguồn dữ liệu (Đầy đủ các nguồn ứng dụng đang sử dụng)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_data_source_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Nguồn dữ liệu",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Mực nước quan trắc: Hệ thống Giám sát Thiên tai Việt Nam (vndms.gov.vn) - Trạm ${metadata.stationName} (Mã: ${metadata.stationId}, ${metadata.provinceName}, Nguồn: ${metadata.source})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Bản tin thủy văn: Trung tâm Dự báo Khí tượng Thủy văn Quốc gia (nchmf.gov.vn)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Dữ liệu thời tiết: Hệ thống khí tượng toàn cầu Open-Meteo API (open-meteo.com)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Phiên bản ứng dụng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .testTag("settings_version_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Phiên bản ứng dụng",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "v1.0.1",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OverviewCard(
    metadata: StationMetadataEntity,
    latestPoint: WaterPointEntity?,
    pointsChronological: List<WaterPointEntity>,
    waterLevelPrediction: WaterLevelPrediction?
) {
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm dd/MM/yyyy", NchmfScraper.LOCALE_VI)
    }
    val formattedLastSync = remember(metadata.lastUpdatedMillis) {
        timeFormatter.format(Date(metadata.lastUpdatedMillis))
    }

    val currentLevel = latestPoint?.waterLevel ?: 0f
    val currentDelta = latestPoint?.delta ?: 0f
    val currentTimeLabel = latestPoint?.timeLabel ?: "--"
    val predicted1hLevel = waterLevelPrediction?.predictedLevel ?: currentLevel
    val predicted1hDelta = waterLevelPrediction?.predictedDelta ?: (predicted1hLevel - currentLevel)

    // Mực nước cao nhất trong 24h qua (lấy từ tối đa 24 mốc giờ gần nhất)
    val maxLevel24h = remember(pointsChronological, currentLevel) {
        val startIdx = (pointsChronological.size - 24).coerceAtLeast(0)
        var maxVal = Float.NEGATIVE_INFINITY
        for (i in startIdx until pointsChronological.size) {
            val v = pointsChronological[i].waterLevel
            if (v > maxVal) maxVal = v
        }
        if (maxVal == Float.NEGATIVE_INFINITY) currentLevel else maxVal
    }

    // Rising: soft bright red (đỏ tươi nhạt); Falling: blue (xanh dương)
    val deltaBgColor = when {
        currentDelta > 0.0001f -> Color(0xFFFF3B30).copy(alpha = 0.16f)
        currentDelta < -0.0001f -> WaterLineBlue.copy(alpha = 0.16f)
        else -> DeltaStableGray.copy(alpha = 0.14f)
    }

    val deltaTextColor = when {
        currentDelta > 0.0001f -> Color(0xFFFF2D20)
        currentDelta < -0.0001f -> WaterLineBlue
        else -> DeltaStableGray
    }

    val deltaText = remember(currentDelta) {
        when {
            currentDelta > 0.0001f -> String.format(Locale.US, "▲ %.2f m", abs(currentDelta))
            currentDelta < -0.0001f -> String.format(Locale.US, "▼ %.2f m", abs(currentDelta))
            else -> "▬ 0.00 m"
        }
    }
    val currentLevelFormatted = remember(currentLevel) {
        String.format(Locale.US, "%.2f", currentLevel)
    }
    val predicted1hFormatted = remember(predicted1hLevel) {
        String.format(Locale.US, "%.2f m", predicted1hLevel)
    }
    val max24hFormatted = remember(maxLevel24h) {
        String.format(Locale.US, "%.2f m", maxLevel24h)
    }

    // Màu sắc và icon của viên nhộng dự báo mực nước 1h tới thay đổi theo trạng thái dự đoán:
    // Dự đoán tăng -> Đỏ cảnh báo & icon hướng lên; Dự đoán giảm -> Xanh dương & icon lao dốc; Ổn định -> Xám & icon ngang
    val forecastBadgeBgColor = when {
        predicted1hDelta > 0.0001f -> Color(0xFFFF3B30).copy(alpha = 0.16f)
        predicted1hDelta < -0.0001f -> WaterLineBlue.copy(alpha = 0.16f)
        else -> DeltaStableGray.copy(alpha = 0.14f)
    }

    val forecastBadgeColor = when {
        predicted1hDelta > 0.0001f -> Color(0xFFFF2D20)
        predicted1hDelta < -0.0001f -> WaterLineBlue
        else -> DeltaStableGray
    }

    val forecastBadgeIcon = when {
        predicted1hDelta > 0.0001f -> Icons.Default.TrendingUp
        predicted1hDelta < -0.0001f -> Icons.Default.TrendingDown
        else -> Icons.Default.TrendingFlat
    }

    val alarmComparison = remember(currentLevel, metadata) {
        buildAlarmComparisonInfo(
            level = currentLevel,
            alarm1 = metadata.alarmLevel1,
            alarm2 = metadata.alarmLevel2,
            alarm3 = metadata.alarmLevel3
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overview_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Station header line (căn giữa thẻ lớn)
            Text(
                text = "Trạm ${metadata.stationName} (Mã: ${metadata.stationId}) - ${metadata.provinceName} | Nguồn: ${metadata.source}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Current water level with 3 inline badges on the same horizontal row:
            // 1. Left badge = delta (tăng/giảm trong giờ qua)
            // 2. Middle badge = predicted water level in next 1h (dự báo 1h tới)
            // 3. Right badge = max water level in last 24h (cao nhất trong 24h qua)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = currentLevelFormatted,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 44.sp,
                            lineHeight = 46.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.2).sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1,
                        modifier = Modifier.testTag("current_water_level_text")
                    )
                    Text(
                        text = "m",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // 1. Thẻ số mực nước giảm/tăng trong giờ qua (dạng viên nhộng)
                    Surface(
                        color = deltaBgColor,
                        shape = CircleShape
                    ) {
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = deltaTextColor
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("current_delta_badge")
                        )
                    }

                    // 2. Thẻ dự báo mực nước 1h tới (dạng viên nhộng, đổi icon và màu theo trạng thái dự đoán)
                    Surface(
                        color = forecastBadgeBgColor,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("predicted_1h_water_level_badge"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = forecastBadgeIcon,
                                contentDescription = "Dự báo mực nước 1 giờ tới",
                                tint = forecastBadgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = predicted1hFormatted,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = forecastBadgeColor
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // 3. Thẻ số mực nước cao nhất trong 24h qua (dạng viên nhộng)
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("max_24h_water_level_badge"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerticalAlignTop,
                                contentDescription = "Mực nước cao nhất trong 24h qua",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = max24hFormatted,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Dòng thông báo cảnh báo căn giữa tuyệt đối so với nền viên nhộng dài
            Surface(
                shape = CircleShape,
                color = alarmComparison.color.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = alarmComparison.color.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .testTag("alarm_status_chip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = alarmComparison.color,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = alarmComparison.description,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = alarmComparison.color,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Footer: "Mực nước lúc... - Cập nhật:..." căn giữa thẻ lớn, nằm ngang hàng trên 1 dòng gọn gàng
            Text(
                text = "Mực nước lúc: $currentTimeLabel - Cập nhật: $formattedLastSync",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BorderlessTabBar(
    selectedMode: DisplayMode,
    onSelectMode: (DisplayMode) -> Unit
) {
    val modes = DisplayMode.entries
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mode_segmented_row")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val spacing = 6.dp
            val tabWidth = (maxWidth - spacing) / modes.size
            val indicatorOffset by animateDpAsState(
                targetValue = (tabWidth + spacing) * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "tab_sliding_indicator_offset"
            )

            // Nền viên nhộng trượt mượt mà giữa 2 tab Biểu đồ và Số liệu (State Deferral xuống pha Layout)
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                    .width(tabWidth)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                modes.forEach { mode ->
                    val isSelected = selectedMode == mode
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                        label = "tab_content_color"
                    )
                    val tabScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tab_item_scale"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .graphicsLayer {
                                scaleX = tabScale
                                scaleY = tabScale
                            }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                                onClick = { onSelectMode(mode) }
                            )
                            .padding(horizontal = 12.dp)
                            .testTag("segmented_tab_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (mode == DisplayMode.CHART) {
                                    Icons.Default.Analytics
                                } else {
                                    Icons.Default.TableChart
                                },
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterLevelChartSection(
    points: List<WaterPointEntity>,
    metadata: StationMetadataEntity,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_section_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bảng thông tin mực nước khi chấm đỏ đang ở: luôn luôn hiển thị (kể cả khi mở app hay sau khi popup 5s biến mất)
            val activePoint = remember(points, selectedIndex) {
                if (points.isEmpty()) null
                else {
                    val idx = (selectedIndex ?: points.lastIndex).coerceIn(points.indices)
                    points[idx]
                }
            }

            if (activePoint != null) {
                Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                    ChartInteractiveTooltipBanner(
                        point = activePoint,
                        metadata = metadata,
                        isSelectedPoint = selectedIndex != null
                    )
                }
            }

            // Pure Compose Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .padding(horizontal = 4.dp)
            ) {
                if (points.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đang tải dữ liệu biểu đồ mực nước...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    WaterLevelLineChart(
                        points = points,
                        alarm1 = metadata.alarmLevel1,
                        alarm2 = metadata.alarmLevel2,
                        alarm3 = metadata.alarmLevel3,
                        historicalFloodLevel = metadata.historicalFloodLevel,
                        selectedIndex = selectedIndex,
                        onSelectPoint = onSelectIndex,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("water_level_canvas_chart")
                    )
                }
            }

            // Legend below chart (căn giữa, đã bỏ chú thích Mực nước thực tế)
            val bd1Label = remember(metadata.alarmLevel1) {
                String.format(Locale.US, "BĐ1 (%.2fm)", metadata.alarmLevel1)
            }
            val bd2Label = remember(metadata.alarmLevel2) {
                String.format(Locale.US, "BĐ2 (%.2fm)", metadata.alarmLevel2)
            }
            val bd3Label = remember(metadata.alarmLevel3) {
                String.format(Locale.US, "BĐ3 (%.2fm)", metadata.alarmLevel3)
            }
            val histFloodLabel = remember(metadata.historicalFloodYear, metadata.historicalFloodLevel) {
                String.format(
                    Locale.US,
                    "Lũ lịch sử %s (%.2fm)",
                    metadata.historicalFloodYear,
                    metadata.historicalFloodLevel
                )
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartLegendItem(
                    color = AlarmLevel1Green,
                    label = bd1Label,
                    isDashed = true
                )
                ChartLegendItem(
                    color = AlarmLevel2Orange,
                    label = bd2Label,
                    isDashed = true
                )
                ChartLegendItem(
                    color = AlarmLevel3Red,
                    label = bd3Label,
                    isDashed = true
                )
                ChartLegendItem(
                    color = HistoricalFloodPurple,
                    label = histFloodLabel,
                    isDashed = true
                )
            }
        }
    }
}

@Composable
private fun ChartInteractiveTooltipBanner(
    point: WaterPointEntity,
    metadata: StationMetadataEntity,
    isSelectedPoint: Boolean = false
) {
    val deltaColor = when {
        point.delta > 0.0001f -> DeltaRisingRed
        point.delta < -0.0001f -> DeltaFallingBlue
        else -> DeltaStableGray
    }
    val deltaString = remember(point.delta) {
        when {
            point.delta > 0.0001f -> String.format(Locale.US, "▲ %.2f m", abs(point.delta))
            point.delta < -0.0001f -> String.format(Locale.US, "▼ %.2f m", abs(point.delta))
            else -> "▬ 0.00 m"
        }
    }
    val levelString = remember(point.waterLevel) {
        String.format(Locale.US, "Mực nước: %.2f m", point.waterLevel)
    }
    val badgeInfo = remember(point.waterLevel, metadata) {
        getPointStatusBadge(
            level = point.waterLevel,
            alarm1 = metadata.alarmLevel1,
            alarm2 = metadata.alarmLevel2,
            alarm3 = metadata.alarmLevel3
        )
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_tooltip_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val pointHeader = if (isSelectedPoint) "Điểm đang chọn: ${point.timeLabel}" else "Mốc mới nhất: ${point.timeLabel}"
                Text(
                    text = pointHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = levelString,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "($deltaString)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = deltaColor
                    )
                }
            }

            StatusBadgePill(label = badgeInfo.first, color = badgeInfo.second)
        }
    }
}

private fun extractHourOnlyLabel(timeLabel: String): String {
    val rawHour = timeLabel.trim().substringBefore(" ").trim()
    return if (rawHour.isNotEmpty()) rawHour else timeLabel
}

private fun extractDatePart(timeLabel: String): String {
    val parts = timeLabel.trim().split(WHITESPACE_REGEX)
    return if (parts.size >= 2) parts.last() else ""
}

private fun isMidnightTransition(point: WaterPointEntity, prevPoint: WaterPointEntity?): Boolean {
    val hourPart = extractHourOnlyLabel(point.timeLabel).lowercase(Locale.ROOT)
    if (hourPart == "0h" || hourPart == "00h" || hourPart == "00:00" || hourPart == "0:00") {
        return true
    }
    if (prevPoint != null) {
        val prevDate = extractDatePart(prevPoint.timeLabel)
        val currDate = extractDatePart(point.timeLabel)
        if (prevDate.isNotEmpty() && currDate.isNotEmpty() && prevDate != currDate) {
            return true
        }
    }
    return false
}

@Composable
fun WaterLevelLineChart(
    points: List<WaterPointEntity>,
    alarm1: Float,
    alarm2: Float,
    alarm3: Float,
    historicalFloodLevel: Float,
    selectedIndex: Int?,
    onSelectPoint: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val dayBoundaryLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val minY = 0.0f
    val maxY = 10.0f

    val refDashEffect = remember {
        PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    }
    val cursorDashEffect = remember {
        PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    // Animated wave phase for the lively water ripple line cutting across latest water level
    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing)
        ),
        label = "wave_phase"
    )

    // Compact horizontal padding so the chart area takes maximum horizontal width
    val compactLeftPadDp = 24.dp
    val compactRightPadDp = 8.dp
    val topPadDp = 14.dp
    val bottomPadDp = 26.dp

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                // Chỉ kích hoạt chọn mốc khi bấm trúng trực tiếp vào các chấm tròn (data points / nodes).
                // Vô hiệu hóa hoàn toàn việc bấm vào vùng trống, lưới biểu đồ hoặc đường nối.
                detectTapGestures { tapOffset ->
                    if (points.isNotEmpty()) {
                        val leftPad = compactLeftPadDp.toPx()
                        val rightPad = compactRightPadDp.toPx()
                        val topPad = topPadDp.toPx()
                        val bottomPad = bottomPadDp.toPx()

                        val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                        // Bán kính vùng chạm trực tiếp trên từng chấm tròn (node hit-testing)
                        val maxNodeHitRadiusPx = 14.dp.toPx()

                        var closestIndex = -1
                        var closestDistance = Float.MAX_VALUE

                        points.forEachIndexed { idx, pt ->
                            val nodeX = if (points.size <= 1) {
                                leftPad + chartW / 2f
                            } else {
                                leftPad + (idx.toFloat() / (points.size - 1).toFloat()) * chartW
                            }
                            val normalizedY = ((pt.waterLevel - minY) / (maxY - minY)).coerceIn(0f, 1f)
                            val nodeY = topPad + chartH * (1f - normalizedY)

                            val dist = hypot(tapOffset.x - nodeX, tapOffset.y - nodeY)
                            if (dist <= maxNodeHitRadiusPx && dist < closestDistance) {
                                closestDistance = dist
                                closestIndex = idx
                            }
                        }

                        if (closestIndex in points.indices) {
                            onSelectPoint(closestIndex)
                        }
                    }
                }
            }
            .drawWithCache {
                val leftPad = compactLeftPadDp.toPx()
                val rightPad = compactRightPadDp.toPx()
                val topPad = topPadDp.toPx()
                val bottomPad = bottomPadDp.toPx()

                val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                fun levelToY(level: Float): Float {
                    val normalized = ((level - minY) / (maxY - minY)).coerceIn(0f, 1f)
                    return topPad + chartHeight * (1f - normalized)
                }

                fun indexToX(idx: Int): Float {
                    return if (points.size <= 1) {
                        leftPad + chartWidth / 2f
                    } else {
                        leftPad + (idx.toFloat() / (points.size - 1).toFloat()) * chartWidth
                    }
                }

                val plottedOffsets = points.mapIndexed { idx, pt ->
                    Offset(x = indexToX(idx), y = levelToY(pt.waterLevel))
                }

                val linePath = Path().apply {
                    plottedOffsets.forEachIndexed { idx, offset ->
                        if (idx == 0) moveTo(offset.x, offset.y)
                        else lineTo(offset.x, offset.y)
                    }
                }

                val yAxisPaint = Paint().apply {
                    isAntiAlias = true
                    color = axisLabelColor
                    textSize = 9.5.sp.toPx()
                    textAlign = Paint.Align.RIGHT
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                val xAxisPaint = Paint().apply {
                    isAntiAlias = true
                    color = axisLabelColor
                    textSize = 9.5.sp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                // Precompute day zones and midnight boundary X positions in cache scope
                val boundaryIndices = mutableListOf<Int>()
                points.forEachIndexed { idx, pt ->
                    if (idx > 0 && isMidnightTransition(pt, points[idx - 1])) {
                        boundaryIndices.add(idx)
                    }
                }

                val yStepsWithLabels = listOf(
                    0f to "0m",
                    2f to "2m",
                    4f to "4m",
                    6f to "6m",
                    8f to "8m",
                    10f to "10m"
                )
                val refLines = listOf(
                    alarm1 to AlarmLevel1Green,
                    alarm2 to AlarmLevel2Orange,
                    alarm3 to AlarmLevel3Red,
                    historicalFloodLevel to HistoricalFloodPurple
                )

                val sortedLabelIndices = if (points.isNotEmpty()) {
                    val step = (points.size / 6).coerceAtLeast(1)
                    val labelSet = LinkedHashSet<Int>()
                    points.indices.forEach { idx ->
                        if (idx % step == 0 || boundaryIndices.contains(idx) || idx == points.lastIndex) {
                            labelSet.add(idx)
                        }
                    }
                    labelSet.sorted()
                } else {
                    emptyList()
                }

                // Precompute X-axis label positions and strings in cache scope (zero allocation in onDrawBehind)
                data class CachedXLabel(val x: Float, val clampedX: Float, val label: String)
                val cachedXLabels = ArrayList<CachedXLabel>(sortedLabelIndices.size)
                if (sortedLabelIndices.isNotEmpty()) {
                    var lastDrawnRightEdge = -100f
                    val minGapPx = 6.dp.toPx()
                    sortedLabelIndices.forEach { idx ->
                        val x = indexToX(idx)
                        val hourOnlyLabel = extractHourOnlyLabel(points[idx].timeLabel)
                        val textWidth = xAxisPaint.measureText(hourOnlyLabel)
                        val clampedX = (x - textWidth / 2f).coerceIn(
                            leftPad,
                            (leftPad + chartWidth - textWidth).coerceAtLeast(leftPad)
                        )
                        if (clampedX >= lastDrawnRightEdge + minGapPx) {
                            cachedXLabels.add(CachedXLabel(x = x, clampedX = clampedX, label = hourOnlyLabel))
                            lastDrawnRightEdge = clampedX + textWidth
                        }
                    }
                }

                val startIdx24h = (points.size - 24).coerceAtLeast(0)
                val peak24hIndex = if (points.isNotEmpty()) {
                    (startIdx24h..points.lastIndex).maxByOrNull { points[it].waterLevel } ?: -1
                } else {
                    -1
                }
                val peakYellowColor = Color(0xFFFFC107)

                // Reusable Path and Stroke objects for the 60fps wave animation (zero per-frame heap allocation)
                val waveLinePath = Path()
                val waveFillPath = Path()
                val waveStroke = Stroke(
                    width = 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val waterLineStroke = Stroke(
                    width = 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val subGridColor = gridColor.copy(alpha = 0.25f)
                val latestWaterLevel = points.lastOrNull()?.waterLevel
                val waveBaseY = if (latestWaterLevel != null) levelToY(latestWaterLevel) else 0f
                val waveAmplitude = 2.6.dp.toPx()
                val waveStep = 5.dp.toPx()
                val waveFreq = (2f * Math.PI.toFloat()) / (chartWidth * 0.42f)
                val waveWaterTint = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x1F00B0FF),
                        Color(0x06007BFF)
                    ),
                    startY = waveBaseY - waveAmplitude,
                    endY = topPad + chartHeight
                )

                onDrawBehind {
                    // 0. Subtle vertical divider line at each day transition (00:00 / 0h)
                    if (points.isNotEmpty()) {
                        boundaryIndices.forEach { bIdx ->
                            val boundaryX = indexToX(bIdx)
                            drawLine(
                                color = dayBoundaryLineColor,
                                start = Offset(boundaryX, topPad),
                                end = Offset(boundaryX, topPad + chartHeight),
                                strokeWidth = 1.1.dp.toPx()
                            )
                        }
                    }

                    // 1. Horizontal Y-axis grid lines (0m to 10m step 2m) with compact Y labels
                    yStepsWithLabels.forEach { (stepValue, stepLabel) ->
                        val y = levelToY(stepValue)
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPad, y),
                            end = Offset(leftPad + chartWidth, y),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            stepLabel,
                            leftPad - 4.dp.toPx(),
                            y + 3.5.dp.toPx(),
                            yAxisPaint
                        )
                    }

                    // 2. 4 Slim Dashed Reference Lines WITHOUT text labels on the chart
                    refLines.forEach { (level, refColor) ->
                        val y = levelToY(level)
                        drawLine(
                            color = refColor,
                            start = Offset(leftPad, y),
                            end = Offset(leftPad + chartWidth, y),
                            strokeWidth = 0.9.dp.toPx(),
                            pathEffect = refDashEffect
                        )
                    }

                    // 2.5. Animated horizontal wave ripple line cutting across the latest water level point
                    if (latestWaterLevel != null) {
                        waveLinePath.reset()
                        var currX = leftPad
                        var first = true
                        while (currX <= leftPad + chartWidth + waveStep) {
                            val clampedX = currX.coerceAtMost(leftPad + chartWidth)
                            val currentSine = kotlin.math.sin((clampedX - leftPad) * waveFreq + wavePhase).toFloat()
                            val waveY = waveBaseY + currentSine * waveAmplitude
                            if (first) {
                                waveLinePath.moveTo(clampedX, waveY)
                                first = false
                            } else {
                                waveLinePath.lineTo(clampedX, waveY)
                            }
                            currX += waveStep
                        }

                        waveFillPath.reset()
                        waveFillPath.addPath(waveLinePath)
                        waveFillPath.lineTo(leftPad + chartWidth, topPad + chartHeight)
                        waveFillPath.lineTo(leftPad, topPad + chartHeight)
                        waveFillPath.close()

                        drawPath(
                            path = waveFillPath,
                            brush = waveWaterTint
                        )

                        drawPath(
                            path = waveLinePath,
                            color = Color(0x8029B6F6),
                            style = waveStroke
                        )
                    }

                    // 3. Water Level Line (#007BFF) without background fill (thinner connecting lines, dots keep same size)
                    if (plottedOffsets.isNotEmpty()) {
                        drawPath(
                            path = linePath,
                            color = WaterLineBlue,
                            style = waterLineStroke
                        )
                    }

                    // 5. X-axis 24h hour-only labels (e.g., 0h, 6h, 23h - date part hidden)
                    cachedXLabels.forEach { item ->
                        drawLine(
                            color = subGridColor,
                            start = Offset(item.x, topPad),
                            end = Offset(item.x, topPad + chartHeight),
                            strokeWidth = 0.6.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            item.label,
                            item.clampedX,
                            topPad + chartHeight + 16.dp.toPx(),
                            xAxisPaint
                        )
                    }

                    // 6. Circular markers at each hour & active vertical cursor + callout
                    val activeRedDotIndex = selectedIndex ?: points.lastIndex

                    plottedOffsets.forEachIndexed { idx, offset ->
                        val isTapped = selectedIndex != null && idx == selectedIndex
                        val isRedDot = idx == activeRedDotIndex
                        val isPeak24hDot = idx == peak24hIndex && !isRedDot

                        if (isTapped) {
                            drawLine(
                                color = WaterLineBlue.copy(alpha = 0.55f),
                                start = Offset(offset.x, topPad),
                                end = Offset(offset.x, topPad + chartHeight),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = cursorDashEffect
                            )
                        }

                        drawCircle(
                            color = surfaceColor,
                            radius = when {
                                isRedDot -> 6.dp.toPx()
                                isPeak24hDot -> 5.2.dp.toPx()
                                else -> 3.5.dp.toPx()
                            },
                            center = offset
                        )
                        drawCircle(
                            color = when {
                                isRedDot -> AlarmLevel3Red
                                isPeak24hDot -> peakYellowColor
                                else -> WaterLineBlue
                            },
                            radius = when {
                                isRedDot -> 4.5.dp.toPx()
                                isPeak24hDot -> 3.8.dp.toPx()
                                else -> 2.4.dp.toPx()
                            },
                            center = offset
                        )
                    }
                }
            }
    ) {}
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    isDashed: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (isDashed) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(2.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(2.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WaterLevelTableSection(
    pointsDescending: List<WaterPointEntity>,
    metadata: StationMetadataEntity
) {
    val verticalDividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("table_section_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header with equal column spacing and vertical dividers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Thời gian",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 0.8.dp,
                    color = verticalDividerColor
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mực nước",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 0.8.dp,
                    color = verticalDividerColor
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chênh lệch",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 0.8.dp,
                    color = verticalDividerColor
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Trạng thái",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("water_level_table_list")
            ) {
                pointsDescending.forEachIndexed { idx, point ->
                    key(point.pointIndex) {
                        val isNewest = idx == 0
                        WaterLevelTableRow(
                            point = point,
                            metadata = metadata,
                            isNewest = isNewest,
                            verticalDividerColor = verticalDividerColor
                        )
                        if (idx < pointsDescending.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterLevelTableRow(
    point: WaterPointEntity,
    metadata: StationMetadataEntity,
    isNewest: Boolean,
    verticalDividerColor: Color
) {
    val deltaColor = when {
        point.delta > 0.0001f -> DeltaRisingRed
        point.delta < -0.0001f -> DeltaFallingBlue
        else -> DeltaStableGray
    }

    val deltaFormatted = remember(point.delta) {
        when {
            point.delta > 0.0001f -> String.format(Locale.US, "▲ %.2f m", abs(point.delta))
            point.delta < -0.0001f -> String.format(Locale.US, "▼ %.2f m", abs(point.delta))
            else -> "▬ 0.00 m"
        }
    }
    val levelFormatted = remember(point.waterLevel) {
        String.format(Locale.US, "%.2f m", point.waterLevel)
    }

    val (statusLabel, statusColor) = remember(point.waterLevel, metadata) {
        getPointStatusBadge(
            level = point.waterLevel,
            alarm1 = metadata.alarmLevel1,
            alarm2 = metadata.alarmLevel2,
            alarm3 = metadata.alarmLevel3
        )
    }

    val rowBg = if (isNewest) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(rowBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Thời gian
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = point.timeLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isNewest) FontWeight.Bold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (isNewest) {
                Text(
                    text = "Mới nhất",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 0.8.dp,
            color = verticalDividerColor
        )

        // Column 2: Mực nước
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = levelFormatted,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 0.8.dp,
            color = verticalDividerColor
        )

        // Column 3: Chênh lệch
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = deltaFormatted,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = deltaColor,
                textAlign = TextAlign.Center
            )
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 0.8.dp,
            color = verticalDividerColor
        )

        // Column 4: Trạng thái
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            StatusBadgePill(label = statusLabel, color = statusColor)
        }
    }
}

@Composable
private fun StatusBadgePill(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = CircleShape,
        modifier = Modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.4f),
            shape = CircleShape
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Immutable
data class AlarmComparisonInfo(
    val description: String,
    val color: Color
)

private fun buildAlarmComparisonInfo(
    level: Float,
    alarm1: Float,
    alarm2: Float,
    alarm3: Float
): AlarmComparisonInfo {
    return when {
        level >= alarm3 -> {
            val diff3 = level - alarm3
            AlarmComparisonInfo(
                description = String.format(
                    Locale.US,
                    "Vượt Báo động 3 (%.2fm) • Nguy hiểm lũ lớn!",
                    diff3
                ),
                color = AlarmLevel3Red
            )
        }

        level >= alarm2 -> {
            val above2 = level - alarm2
            val below3 = alarm3 - level
            AlarmComparisonInfo(
                description = String.format(
                    Locale.US,
                    "Vượt Báo động 2 (%.2fm) • Dưới BĐ3 (%.2fm)",
                    above2,
                    below3
                ),
                color = AlarmLevel2Orange
            )
        }

        level >= alarm1 -> {
            val above1 = level - alarm1
            val below2 = alarm2 - level
            AlarmComparisonInfo(
                description = String.format(
                    Locale.US,
                    "Vượt Báo động 1 (%.2fm) • Dưới BĐ2 (%.2fm)",
                    above1,
                    below2
                ),
                color = AlarmLevel1Green
            )
        }

        else -> {
            val below1 = abs(alarm1 - level)
            AlarmComparisonInfo(
                description = String.format(
                    Locale.US,
                    "Dưới Báo động 1 (%.2fm) • Mực nước an toàn",
                    below1
                ),
                color = WaterLineBlue
            )
        }
    }
}

private fun getPointStatusBadge(
    level: Float,
    alarm1: Float,
    alarm2: Float,
    alarm3: Float
): Pair<String, Color> {
    return when {
        level >= alarm3 -> "Trên BĐ3" to AlarmLevel3Red
        level >= alarm2 -> "Trên BĐ2" to AlarmLevel2Orange
        level >= alarm1 -> "Trên BĐ1" to AlarmLevel1Green
        else -> "< BĐ1" to WaterLineBlue
    }
}
