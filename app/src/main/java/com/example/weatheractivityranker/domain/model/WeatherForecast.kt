package com.example.weatheractivityranker.domain.model

data class WeatherForecast(
    val city: City,
    val dailyForecasts: List<DailyWeather>,
)
