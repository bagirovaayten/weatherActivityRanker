package com.example.weatheractivityranker.data.mapper

import com.example.weatheractivityranker.data.remote.dto.DailyForecastDto
import com.example.weatheractivityranker.data.remote.dto.GeocodingResultDto
import com.example.weatheractivityranker.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeatherMappersTest {

    @Test
    fun `geocoding dto maps to city`() {
        val dto = GeocodingResultDto(
            id = 42L,
            name = "Baku",
            latitude = 40.38,
            longitude = 49.89,
            country = "Azerbaijan",
            admin1 = "Baku",
        )

        val city = dto.toDomain()

        assertEquals(42L, city.id)
        assertEquals("Baku, Baku, Azerbaijan", city.displayLabel)
    }

    @Test
    fun `forecast dto maps daily rows`() {
        val dto = DailyForecastDto(
            time = listOf("2025-06-10", "2025-06-11"),
            temperatureMax = listOf(24.0, 26.0),
            temperatureMin = listOf(16.0, 18.0),
            precipitationSum = listOf(0.0, 3.5),
            snowfallSum = listOf(0.0, 0.0),
            windSpeedMax = listOf(12.0, 20.0),
        )

        val daily = dto.toDomain()

        assertEquals(2, daily.size)
        assertEquals(LocalDate.of(2025, 6, 10), daily[0].date)
        assertEquals(24.0, daily[0].temperatureMaxCelsius, 0.001)
        assertEquals(3.5, daily[1].precipitationMm, 0.001)
    }

    @Test(expected = AppError.InvalidResponse::class)
    fun `mismatched forecast arrays throw invalid response`() {
        val dto = DailyForecastDto(
            time = listOf("2025-06-10"),
            temperatureMax = listOf(24.0, 26.0),
            temperatureMin = listOf(16.0),
            precipitationSum = listOf(0.0),
            snowfallSum = listOf(0.0),
            windSpeedMax = listOf(12.0),
        )

        dto.toDomain()
    }

    @Test
    fun `empty forecast time throws invalid response`() {
        val result = runCatching { DailyForecastDto().toDomain() }

        assertTrue(result.exceptionOrNull() is AppError.InvalidResponse)
    }
}
