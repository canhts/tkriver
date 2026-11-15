package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WaterLevelDto(
    @Json(name = "station_id") val stationId: String? = "71540",
    @Json(name = "name_vn") val nameVn: String? = "Trà Khúc",
    @Json(name = "river_name") val riverName: String? = "Trà Khúc",
    @Json(name = "province_name") val provinceName: String? = "Quảng Ngãi",
    @Json(name = "district_name") val districtName: String? = null,
    @Json(name = "source") val source: String? = "KTTV",
    @Json(name = "source_set") val sourceSet: String? = "Water",
    @Json(name = "nam_lu_lich_su") val historicalFloodYear: String? = "2013",
    @Json(name = "labels") val labels: String? = "",
    @Json(name = "value") val value: String? = "",
    @Json(name = "bao_dong1") val baoDong1: String? = "3.5",
    @Json(name = "bao_dong2") val baoDong2: String? = "5.0",
    @Json(name = "bao_dong3") val baoDong3: String? = "6.5",
    @Json(name = "gia_tri_lu_lich_su") val historicalFloodValue: String? = "8.76"
)
