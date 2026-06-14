package com.example.weatheractivityranker.domain.usecase

import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.RankedActivity
import com.example.weatheractivityranker.domain.model.WeatherForecast
import com.example.weatheractivityranker.domain.ranking.ActivityRankingEngine
import com.example.weatheractivityranker.domain.repository.WeatherRepository
import javax.inject.Inject

data class ActivityRankingsResult(
    val forecast: WeatherForecast,
    val rankedActivities: List<RankedActivity>,
)

class GetActivityRankingsUseCase @Inject constructor(
    private val repository: WeatherRepository,
    private val rankingEngine: ActivityRankingEngine,
) {
    suspend operator fun invoke(city: City): Result<ActivityRankingsResult> {
        return repository.getForecast(city)
            .map { forecast ->
                ActivityRankingsResult(
                    forecast = forecast,
                    rankedActivities = rankingEngine.rankActivities(forecast.dailyForecasts),
                )
            }
    }
}

class SearchCitiesUseCase @Inject constructor(
    private val repository: WeatherRepository,
) {
    suspend operator fun invoke(query: String): Result<List<City>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        return repository.searchCities(query.trim())
    }
}
