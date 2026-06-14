package com.example.weatheractivityranker.presentation.home

import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.DailyWeather
import com.example.weatheractivityranker.domain.model.RankedActivity

data class HomeUiState(
    val searchQuery: String = "",
    val citySuggestions: List<City> = emptyList(),
    val selectedCity: City? = null,
    val rankedActivities: List<RankedActivity> = emptyList(),
    val dailyForecasts: List<DailyWeather> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingRankings: Boolean = false,
    val errorMessage: String? = null,
) {
    val showSuggestions: Boolean
        get() = searchQuery.isNotBlank() &&
            citySuggestions.isNotEmpty() &&
            selectedCity == null &&
            !isSearching

    val showRankings: Boolean
        get() = selectedCity != null && rankedActivities.isNotEmpty()
}
