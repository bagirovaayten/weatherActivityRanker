package com.example.weatheractivityranker.domain.repository

import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.WeatherForecast

interface WeatherRepository {
    suspend fun searchCities(query: String): Result<List<City>>
    suspend fun getForecast(city: City): Result<WeatherForecast>
}
