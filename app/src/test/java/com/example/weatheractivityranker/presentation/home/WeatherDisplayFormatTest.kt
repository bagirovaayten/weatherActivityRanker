package com.example.weatheractivityranker.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherDisplayFormatTest {

    @Test
    fun temperatureCelsius_roundsToNearestInteger() {
        assertEquals("22", WeatherDisplayFormat.temperatureCelsius(21.8))
        assertEquals("22", WeatherDisplayFormat.temperatureCelsius(21.5))
        assertEquals("-3", WeatherDisplayFormat.temperatureCelsius(-2.6))
    }

    @Test
    fun precipitationMm_formatsWithoutFloatingPointArtifacts() {
        assertEquals("0", WeatherDisplayFormat.precipitationMm(0.0))
        assertEquals("1.3", WeatherDisplayFormat.precipitationMm(1.2999999))
        assertEquals("5", WeatherDisplayFormat.precipitationMm(5.04))
    }

    @Test
    fun snowfallCm_formatsWithoutFloatingPointArtifacts() {
        assertEquals("0", WeatherDisplayFormat.snowfallCm(0.0))
        assertEquals("2.5", WeatherDisplayFormat.snowfallCm(2.499999))
    }

    @Test
    fun windSpeedKmh_roundsToNearestInteger() {
        assertEquals("15", WeatherDisplayFormat.windSpeedKmh(14.6))
        assertEquals("15", WeatherDisplayFormat.windSpeedKmh(14.5))
    }
}
