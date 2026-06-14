package com.example.weatheractivityranker.domain.ranking

import com.example.weatheractivityranker.domain.model.ActivityType
import com.example.weatheractivityranker.domain.model.DailyWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ActivityRankingEngineTest {

    private lateinit var engine: ActivityRankingEngine

    @Before
    fun setUp() {
        engine = ActivityRankingEngine()
    }

    @Test
    fun `rankActivities returns four activities with unique ranks`() {
        val forecasts = snowyWeek()

        val rankings = engine.rankActivities(forecasts)

        assertEquals(4, rankings.size)
        assertEquals(listOf(1, 2, 3, 4), rankings.map { it.rank })
        assertEquals(ActivityType.entries.size, rankings.map { it.activity }.toSet().size)
        rankings.forEach { assertTrue(it.score in 0..100) }
    }

    @Test
    fun `skiing ranks highest in cold snowy week`() {
        val rankings = engine.rankActivities(snowyWeek())

        assertEquals(ActivityType.SKIING, rankings.first().activity)
    }

    @Test
    fun `outdoor sightseeing ranks highest in mild dry week`() {
        val rankings = engine.rankActivities(mildDryWeek())

        assertEquals(ActivityType.OUTDOOR_SIGHTSEEING, rankings.first().activity)
    }

    @Test
    fun `indoor sightseeing ranks higher during rainy week than outdoor`() {
        val rankings = engine.rankActivities(rainyWeek())
        val indoor = rankings.first { it.activity == ActivityType.INDOOR_SIGHTSEEING }
        val outdoor = rankings.first { it.activity == ActivityType.OUTDOOR_SIGHTSEEING }

        assertTrue(indoor.score > outdoor.score)
    }

    @Test
    fun `surfing scores higher on windy warm day than calm cold day`() {
        val windyWarm = day(
            max = 22.0,
            min = 16.0,
            precipitation = 0.5,
            snowfall = 0.0,
            wind = 30.0,
        )
        val calmCold = day(
            max = 4.0,
            min = -1.0,
            precipitation = 0.0,
            snowfall = 0.0,
            wind = 5.0,
        )

        val windyScore = engine.scoreDay(ActivityType.SURFING, windyWarm)
        val calmScore = engine.scoreDay(ActivityType.SURFING, calmCold)

        assertTrue(windyScore > calmScore)
    }

    private fun snowyWeek(): List<DailyWeather> =
        List(7) { index ->
            day(
                max = -1.0,
                min = -8.0,
                precipitation = 2.0,
                snowfall = 4.0,
                wind = 18.0,
                dayOffset = index,
            )
        }

    private fun mildDryWeek(): List<DailyWeather> =
        List(7) { index ->
            day(
                max = 22.0,
                min = 14.0,
                precipitation = 0.2,
                snowfall = 0.0,
                wind = 10.0,
                dayOffset = index,
            )
        }

    private fun rainyWeek(): List<DailyWeather> =
        List(7) { index ->
            day(
                max = 11.0,
                min = 7.0,
                precipitation = 12.0,
                snowfall = 0.0,
                wind = 28.0,
                dayOffset = index,
            )
        }

    private fun day(
        max: Double,
        min: Double,
        precipitation: Double,
        snowfall: Double,
        wind: Double,
        dayOffset: Int = 0,
    ): DailyWeather = DailyWeather(
        date = LocalDate.of(2025, 1, 1).plusDays(dayOffset.toLong()),
        temperatureMaxCelsius = max,
        temperatureMinCelsius = min,
        precipitationMm = precipitation,
        snowfallCm = snowfall,
        windSpeedMaxKmh = wind,
    )
}
