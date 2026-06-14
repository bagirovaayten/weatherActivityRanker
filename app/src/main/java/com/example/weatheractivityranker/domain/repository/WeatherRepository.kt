package com.example.weatheractivityranker.domain.repository

import com.example.weatheractivityranker.domain.model.AppError
import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.WeatherForecast

interface WeatherRepository {
    suspend fun searchCities(query: String): Result<List<City>>
    suspend fun getForecast(city: City): Result<WeatherForecast>
}

suspend fun <T> Result<T>.mapAppError(transform: (Throwable) -> AppError = { AppError.Unknown(it.message ?: "Unknown error") }): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { throwable ->
            Result.failure(
                when (throwable) {
                    is AppError -> throwable
                    else -> transform(throwable)
                },
            )
        },
    )
}
