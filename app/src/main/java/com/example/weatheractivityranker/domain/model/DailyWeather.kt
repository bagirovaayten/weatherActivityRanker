package com.example.weatheractivityranker.domain.model

import java.time.LocalDate

data class DailyWeather(
    val date: LocalDate,
    val temperatureMaxCelsius: Double,
    val temperatureMinCelsius: Double,
    val precipitationMm: Double,
    val snowfallCm: Double,
    val windSpeedMaxKmh: Double,
)
