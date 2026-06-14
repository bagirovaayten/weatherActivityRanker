package com.example.weatheractivityranker.data.repository

import com.example.weatheractivityranker.data.mapper.toDomain
import com.example.weatheractivityranker.data.mapper.toWeatherForecast
import com.example.weatheractivityranker.data.remote.ForecastApi
import com.example.weatheractivityranker.data.remote.GeocodingApi
import com.example.weatheractivityranker.domain.model.AppError
import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.WeatherForecast
import com.example.weatheractivityranker.domain.repository.WeatherRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val geocodingApi: GeocodingApi,
    private val forecastApi: ForecastApi,
) : WeatherRepository {

    override suspend fun searchCities(query: String): Result<List<City>> = runCatching {
        val response = geocodingApi.searchCities(name = query)
        val cities = response.results?.map { it.toDomain() }.orEmpty()
        if (cities.isEmpty()) {
            throw AppError.NotFound("No cities matched \"$query\".")
        }
        cities
    }.mapErrors()

    override suspend fun getForecast(city: City): Result<WeatherForecast> = runCatching {
        val response = forecastApi.getForecast(
            latitude = city.latitude,
            longitude = city.longitude,
        )
        val daily = response.daily ?: throw AppError.InvalidResponse("Daily forecast is missing.")
        daily.toDomain().toWeatherForecast(city)
    }.mapErrors()

    private fun <T> Result<T>.mapErrors(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { throwable ->
            Result.failure(mapThrowable(throwable))
        },
    )

    private fun mapThrowable(throwable: Throwable): AppError = when (throwable) {
        is AppError -> throwable
        is IOException -> AppError.Network(
            throwable.message?.takeIf { it.isNotBlank() }
                ?.let { "Network error: $it" }
                ?: "Network error. Check your connection and try again.",
        )
        is HttpException -> AppError.Network(
            "Server error (${throwable.code()}). Please try again.",
        )
        is SerializationException -> AppError.InvalidResponse(
            "Failed to parse weather data.",
        )
        else -> AppError.Unknown(
            throwable.message ?: "Something went wrong. Please try again.",
        )
    }
}
