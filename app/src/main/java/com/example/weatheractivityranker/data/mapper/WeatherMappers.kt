package com.example.weatheractivityranker.data.mapper

import com.example.weatheractivityranker.data.remote.dto.DailyForecastDto
import com.example.weatheractivityranker.data.remote.dto.GeocodingResultDto
import com.example.weatheractivityranker.domain.model.AppError
import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.DailyWeather
import com.example.weatheractivityranker.domain.model.WeatherForecast
import java.time.LocalDate

fun GeocodingResultDto.toDomain(): City = City(
    id = id,
    name = name,
    country = country,
    admin1 = admin1,
    latitude = latitude,
    longitude = longitude,
)

fun DailyForecastDto.toDomain(): List<DailyWeather> {
    if (time.isEmpty()) {
        throw AppError.InvalidResponse("Forecast dates are missing.")
    }

    val size = time.size
    val arrays = listOf(
        temperatureMax,
        temperatureMin,
        precipitationSum,
        snowfallSum,
        windSpeedMax,
    )
    if (arrays.any { it.size != size }) {
        throw AppError.InvalidResponse("Forecast arrays have inconsistent lengths.")
    }

    return time.mapIndexed { index, dateString ->
        DailyWeather(
            date = LocalDate.parse(dateString),
            temperatureMaxCelsius = temperatureMax[index],
            temperatureMinCelsius = temperatureMin[index],
            precipitationMm = precipitationSum[index],
            snowfallCm = snowfallSum[index],
            windSpeedMaxKmh = windSpeedMax[index],
        )
    }
}

fun List<DailyWeather>.toWeatherForecast(city: City): WeatherForecast =
    WeatherForecast(city = city, dailyForecasts = this)
