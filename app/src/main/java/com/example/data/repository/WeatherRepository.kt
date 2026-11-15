package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import com.example.data.remote.NchmfScraper
import com.example.data.remote.OpenMeteoApi
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

@Immutable
data class CurrentWeather(
    val temperature: Float,
    val apparentTemperature: Float,
    val relativeHumidity: Int,
    val precipitation: Float,
    val weatherCode: Int,
    val windSpeed: Float,
    val weatherDescription: String
)

@Immutable
data class DailyForecast(
    val dateIso: String,
    val displayDate: String,
    val dayOfWeek: String,
    val weatherCode: Int,
    val weatherDescription: String,
    val tempMax: Float,
    val tempMin: Float,
    val rainProbability: Int,
    val precipitationSum: Float
)

@Immutable
data class WeatherData(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val isFallbackLocation: Boolean = false
)

class WeatherRepository(
    private val openMeteoApi: OpenMeteoApi,
    private val context: Context
) {
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    private val weatherMutex = Mutex()

    @Volatile
    private var cachedWeather: WeatherData? = null

    suspend fun fetchWeather(forceRefresh: Boolean = false): Result<WeatherData> = withContext(Dispatchers.IO) {
        weatherMutex.withLock {
            val existing = cachedWeather
            if (!forceRefresh && existing != null && (System.currentTimeMillis() - existing.lastUpdatedMillis) < CACHE_TTL_MILLIS) {
                return@withLock Result.success(existing)
            }

            try {
                val locationResult = getCurrentLocation()
                val lat = locationResult.latitude
                val lon = locationResult.longitude
                val isFallback = locationResult.isFallback

                val locationName = resolveLocationName(lat, lon, isFallback)

                val response = openMeteoApi.getForecast(
                    latitude = lat,
                    longitude = lon
                )

                val currentDto = response.current ?: throw IllegalStateException("Không nhận được dữ liệu thời tiết hiện tại.")
                val dailyDto = response.daily ?: throw IllegalStateException("Không nhận được dữ liệu dự báo 7 ngày.")

                val current = CurrentWeather(
                    temperature = currentDto.temperature ?: 25f,
                    apparentTemperature = currentDto.apparentTemperature ?: currentDto.temperature ?: 25f,
                    relativeHumidity = currentDto.relativeHumidity ?: 75,
                    precipitation = currentDto.precipitation ?: 0f,
                    weatherCode = currentDto.weatherCode ?: 0,
                    windSpeed = currentDto.windSpeed ?: 10f,
                    weatherDescription = parseWeatherCodeDescription(currentDto.weatherCode ?: 0)
                )

                val dates = dailyDto.time ?: emptyList()
                val codes = dailyDto.weatherCode ?: emptyList()
                val maxTemps = dailyDto.temperatureMax ?: emptyList()
                val minTemps = dailyDto.temperatureMin ?: emptyList()
                val rainProbs = dailyDto.precipitationProbabilityMax ?: emptyList()
                val rainSums = dailyDto.precipitationSum ?: emptyList()

                val dailyList = ArrayList<DailyForecast>(dates.size)
                val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dayDisplayFormat = SimpleDateFormat("dd/MM", NchmfScraper.LOCALE_VI)
                val reusableCalendar = Calendar.getInstance()

                for (i in dates.indices) {
                    val isoDate = dates[i]
                    val parsedDate = runCatching { isoFormat.parse(isoDate) }.getOrNull() ?: Date()
                    val displayDate = dayDisplayFormat.format(parsedDate)
                    val dayOfWeekLabel = formatDayOfWeek(parsedDate, i, reusableCalendar)

                    val code = codes.getOrElse(i) { 0 }
                    val maxT = maxTemps.getOrElse(i) { current.temperature + 3f }
                    val minT = minTemps.getOrElse(i) { current.temperature - 3f }
                    val rainP = rainProbs.getOrElse(i) { 20 }
                    val rainS = rainSums.getOrElse(i) { 0f }

                    dailyList.add(
                        DailyForecast(
                            dateIso = isoDate,
                            displayDate = displayDate,
                            dayOfWeek = dayOfWeekLabel,
                            weatherCode = code,
                            weatherDescription = parseWeatherCodeDescription(code),
                            tempMax = maxT,
                            tempMin = minT,
                            rainProbability = rainP,
                            precipitationSum = rainS
                        )
                    )
                }

                val data = WeatherData(
                    locationName = locationName,
                    latitude = lat,
                    longitude = lon,
                    current = current,
                    daily = dailyList,
                    isFallbackLocation = isFallback
                )
                cachedWeather = data
                Result.success(data)
            } catch (e: Exception) {
                cachedWeather?.let { return@withLock Result.success(it) }
                Result.failure(e)
            }
        }
    }

    private data class CoordinateResult(
        val latitude: Double,
        val longitude: Double,
        val isFallback: Boolean
    )

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): CoordinateResult {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return CoordinateResult(DEFAULT_LAT, DEFAULT_LON, isFallback = true)
        }

        return try {
            val lastLoc = withTimeoutOrNull(2_000L) {
                fusedLocationClient.lastLocation.awaitTask()
            }
            if (lastLoc != null) {
                CoordinateResult(lastLoc.latitude, lastLoc.longitude, isFallback = false)
            } else {
                val cts = CancellationTokenSource()
                val freshLoc = withTimeoutOrNull(4_000L) {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).awaitTask()
                }
                if (freshLoc == null) {
                    cts.cancel()
                }

                if (freshLoc != null) {
                    CoordinateResult(freshLoc.latitude, freshLoc.longitude, isFallback = false)
                } else {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val fallbackLoc = gpsLoc ?: netLoc
                    if (fallbackLoc != null) {
                        CoordinateResult(fallbackLoc.latitude, fallbackLoc.longitude, isFallback = false)
                    } else {
                        CoordinateResult(DEFAULT_LAT, DEFAULT_LON, isFallback = true)
                    }
                }
            }
        } catch (_: Exception) {
            CoordinateResult(DEFAULT_LAT, DEFAULT_LON, isFallback = true)
        }
    }

    private fun resolveLocationName(lat: Double, lon: Double, isFallback: Boolean): String {
        if (isFallback) {
            return "TP. Quảng Ngãi"
        }

        return try {
            val geocoder = Geocoder(context, NchmfScraper.LOCALE_VI)
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lon, 1)
            val addr = list?.firstOrNull()
            val part1 = addr?.subAdminArea ?: addr?.locality ?: addr?.subLocality
            val part2 = addr?.adminArea
            when {
                !part1.isNullOrBlank() && !part2.isNullOrBlank() && part1 != part2 -> "$part1, $part2"
                !part1.isNullOrBlank() -> part1
                !part2.isNullOrBlank() -> part2
                else -> String.format(Locale.US, "%.2f°B, %.2f°Đ", lat, lon)
            }
        } catch (_: Exception) {
            String.format(Locale.US, "Tọa độ: %.2f°B, %.2f°Đ", lat, lon)
        }
    }

    private fun formatDayOfWeek(date: Date, index: Int, cal: Calendar): String {
        if (index == 0) return "Hôm nay"
        if (index == 1) return "Ngày mai"

        cal.time = date
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Thứ 2"
            Calendar.TUESDAY -> "Thứ 3"
            Calendar.WEDNESDAY -> "Thứ 4"
            Calendar.THURSDAY -> "Thứ 5"
            Calendar.FRIDAY -> "Thứ 6"
            Calendar.SATURDAY -> "Thứ 7"
            Calendar.SUNDAY -> "Chủ nhật"
            else -> ""
        }
    }

    companion object {
        const val DEFAULT_LAT = 15.1205
        const val DEFAULT_LON = 108.8048
        private const val CACHE_TTL_MILLIS = 2 * 60 * 1_000L

        fun parseWeatherCodeDescription(code: Int): String {
            return when (code) {
                0 -> "Trời quang đãng, có nắng"
                1 -> "Trời ít mây, nắng ráo"
                2 -> "Mây rải rác, trời dịu"
                3 -> "Nhiều mây, trời u ám"
                45, 48 -> "Có sương mù dày"
                51, 53, 55 -> "Mưa phùn rải rác"
                56, 57 -> "Mưa phùn lạnh buốt"
                61 -> "Mưa rào nhẹ"
                63 -> "Mưa vừa diện rộng"
                65 -> "Mưa to đến rất to"
                66, 67 -> "Mưa đá cục bộ"
                71, 73, 75 -> "Tuyết rơi"
                77 -> "Tuyết hạt"
                80, 81 -> "Mưa rào ngắt quãng"
                82 -> "Mưa rào xối xả"
                85, 86 -> "Mưa tuyết rải rác"
                95 -> "Có dông sét mạnh"
                96, 99 -> "Dông lốc, mưa đá nguy hiểm"
                else -> "Thời tiết thuận lợi"
            }
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener {
        if (cont.isActive) cont.resume(null)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.resume(null)
    }
}
