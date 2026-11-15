package com.example.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface VndmsApi {

    @FormUrlEncoded
    @POST("home/detailRain")
    suspend fun fetchStationWaterLevel(
        @Header("Origin") origin: String = "https://vndms.gov.vn",
        @Header("Referer") referer: String = "https://vndms.gov.vn/",
        @Header("X-Requested-With") requestedWith: String = "XMLHttpRequest",
        @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT,
        @Field("id") stationId: String = DEFAULT_STATION_ID,
        @Field("source") source: String = DEFAULT_SOURCE,
        @Field("timeSelect") timeSelect: String = DEFAULT_TIME_SELECT
    ): WaterLevelDto

    companion object {
        const val BASE_URL = "https://vndms.gov.vn/"
        const val DEFAULT_STATION_ID = "71540"
        const val DEFAULT_SOURCE = "Water"
        const val DEFAULT_TIME_SELECT = "1"
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    }
}
