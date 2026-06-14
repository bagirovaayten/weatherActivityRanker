package com.example.weatheractivityranker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponseDto(
    val results: List<GeocodingResultDto>? = null,
)

@Serializable
data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    @SerialName("admin1") val admin1: String? = null,
)

@Serializable
data class ForecastResponseDto(
    val daily: DailyForecastDto? = null,
)

@Serializable
data class DailyForecastDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double> = emptyList(),
    @SerialName("snowfall_sum") val snowfallSum: List<Double> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double> = emptyList(),
)
