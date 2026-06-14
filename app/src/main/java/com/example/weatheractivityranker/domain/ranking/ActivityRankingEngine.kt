package com.example.weatheractivityranker.domain.ranking

import com.example.weatheractivityranker.domain.model.ActivityType
import com.example.weatheractivityranker.domain.model.DailyWeather
import com.example.weatheractivityranker.domain.model.RankedActivity
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Scores each activity from 0–100 using daily forecast data, then averages over the forecast window.
 */
class ActivityRankingEngine {

    fun rankActivities(dailyForecasts: List<DailyWeather>): List<RankedActivity> {
        require(dailyForecasts.isNotEmpty()) { "At least one daily forecast is required." }

        val averageScores = ActivityType.entries.associateWith { activity ->
            val dailyScores = dailyForecasts.map { day -> scoreDay(activity, day) }
            dailyScores.average().toInt().coerceIn(0, 100)
        }

        val sorted = averageScores.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, (activity, score) ->
                RankedActivity(
                    activity = activity,
                    score = score,
                    rank = index + 1,
                    summary = buildSummary(activity, dailyForecasts),
                )
            }

        return sorted
    }

    internal fun scoreDay(activity: ActivityType, day: DailyWeather): Int {
        val score = when (activity) {
            ActivityType.SKIING -> scoreSkiing(day)
            ActivityType.SURFING -> scoreSurfing(day)
            ActivityType.OUTDOOR_SIGHTSEEING -> scoreOutdoorSightseeing(day)
            ActivityType.INDOOR_SIGHTSEEING -> scoreIndoorSightseeing(day)
        }
        return score.coerceIn(0, 100)
    }

    private fun scoreSkiing(day: DailyWeather): Int {
        val avgTemp = (day.temperatureMaxCelsius + day.temperatureMinCelsius) / 2.0
        val coldScore = gaussianScore(avgTemp, ideal = -2.0, spread = 6.0)
        val snowScore = when {
            day.snowfallCm >= 5.0 -> 100.0
            day.snowfallCm >= 1.0 -> 75.0
            day.snowfallCm > 0.0 -> 55.0
            avgTemp <= 2.0 && day.precipitationMm >= 1.0 -> 35.0
            else -> 10.0
        }
        val freezeBonus = if (day.temperatureMaxCelsius <= 3.0) 15.0 else 0.0
        return weightedAverage(
            weights = listOf(0.45, 0.4, 0.15),
            scores = listOf(coldScore, snowScore, 50.0 + freezeBonus),
        ).toInt()
    }

    private fun scoreSurfing(day: DailyWeather): Int {
        val avgTemp = (day.temperatureMaxCelsius + day.temperatureMinCelsius) / 2.0
        val windScore = gaussianScore(day.windSpeedMaxKmh, ideal = 28.0, spread = 12.0)
        val warmthScore = clampScore((avgTemp - 8.0) / 18.0 * 100.0)
        val dryScore = when {
            day.precipitationMm <= 1.0 -> 100.0
            day.precipitationMm <= 5.0 -> 70.0
            day.precipitationMm <= 10.0 -> 40.0
            else -> 10.0
        }
        return weightedAverage(
            weights = listOf(0.45, 0.3, 0.25),
            scores = listOf(windScore, warmthScore, dryScore),
        ).toInt()
    }

    private fun scoreOutdoorSightseeing(day: DailyWeather): Int {
        val avgTemp = (day.temperatureMaxCelsius + day.temperatureMinCelsius) / 2.0
        val comfortScore = gaussianScore(avgTemp, ideal = 20.0, spread = 8.0)
        val dryScore = when {
            day.precipitationMm <= 0.5 -> 100.0
            day.precipitationMm <= 2.0 -> 80.0
            day.precipitationMm <= 8.0 -> 45.0
            else -> 10.0
        }
        val calmScore = gaussianScore(day.windSpeedMaxKmh, ideal = 12.0, spread = 15.0, invert = true)
        return weightedAverage(
            weights = listOf(0.4, 0.35, 0.25),
            scores = listOf(comfortScore, dryScore, calmScore),
        ).toInt()
    }

    private fun scoreIndoorSightseeing(day: DailyWeather): Int {
        val outdoorScore = scoreOutdoorSightseeing(day)
        val avgTemp = (day.temperatureMaxCelsius + day.temperatureMinCelsius) / 2.0
        val extremeTempPenalty = when {
            avgTemp <= 0.0 || avgTemp >= 32.0 -> 25.0
            avgTemp <= 5.0 || avgTemp >= 28.0 -> 15.0
            else -> 0.0
        }
        val wetBonus = when {
            day.precipitationMm >= 10.0 -> 30.0
            day.precipitationMm >= 5.0 -> 20.0
            day.precipitationMm >= 2.0 -> 10.0
            else -> 0.0
        }
        val windyBonus = if (day.windSpeedMaxKmh >= 35.0) 10.0 else 0.0
        val inverseOutdoor = (100.0 - outdoorScore) * 0.65
        return (inverseOutdoor + extremeTempPenalty + wetBonus + windyBonus)
            .coerceIn(0.0, 100.0)
            .toInt()
    }

    private fun buildSummary(activity: ActivityType, days: List<DailyWeather>): String {
        val dryDays = days.count { it.precipitationMm <= 2.0 }
        val snowyDays = days.count { it.snowfallCm >= 1.0 }
        val windyDays = days.count { it.windSpeedMaxKmh in 20.0..45.0 }
        val mildDays = days.count {
            val avg = (it.temperatureMaxCelsius + it.temperatureMinCelsius) / 2.0
            avg in 12.0..26.0
        }

        return when (activity) {
            ActivityType.SKIING -> "$snowyDays of ${days.size} days with snowfall; cold conditions expected."
            ActivityType.SURFING -> "$windyDays of ${days.size} days with surf-friendly wind; check local swell."
            ActivityType.OUTDOOR_SIGHTSEEING -> "$mildDays mild days and $dryDays mostly dry days in the window."
            ActivityType.INDOOR_SIGHTSEEING -> "Best when weather is poor; ${days.size - dryDays} days may need shelter."
        }
    }

    private fun gaussianScore(
        value: Double,
        ideal: Double,
        spread: Double,
        invert: Boolean = false,
    ): Double {
        val normalized = exp(-0.5 * ((value - ideal) / spread).let { it * it })
        val score = normalized * 100.0
        return if (invert) {
            (100.0 - score).coerceIn(0.0, 100.0)
        } else {
            score.coerceIn(0.0, 100.0)
        }
    }

    private fun clampScore(value: Double): Double = max(0.0, min(100.0, value))

    private fun weightedAverage(weights: List<Double>, scores: List<Double>): Double {
        require(weights.size == scores.size)
        val totalWeight = weights.sum()
        return weights.zip(scores) { weight, score -> weight * score }.sum() / totalWeight
    }
}
