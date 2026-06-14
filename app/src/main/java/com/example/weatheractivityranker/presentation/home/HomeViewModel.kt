package com.example.weatheractivityranker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.usecase.GetActivityRankingsUseCase
import com.example.weatheractivityranker.domain.usecase.SearchCitiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchCitiesUseCase: SearchCitiesUseCase,
    private val getActivityRankingsUseCase: GetActivityRankingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCityFlow = MutableStateFlow<City?>(null)

    init {
        searchQueryFlow
            .debounce(SEARCH_DEBOUNCE_MS)
            .map { it.trim() }
            .distinctUntilChanged()
            .filter { query ->
                val selected = _uiState.value.selectedCity
                selected == null || query != selected.displayLabel
            }
            .flatMapLatest { query ->
                flow<Unit> {
                    if (query.isBlank()) {
                        _uiState.update { it.copy(citySuggestions = emptyList(), isSearching = false) }
                    } else {
                        _uiState.update { it.copy(isSearching = true, errorMessage = null) }
                        searchCitiesUseCase(query)
                            .onSuccess { cities ->
                                _uiState.update {
                                    it.copy(
                                        citySuggestions = cities,
                                        isSearching = false,
                                    )
                                }
                            }
                            .onFailure { throwable ->
                                _uiState.update {
                                    it.copy(
                                        citySuggestions = emptyList(),
                                        isSearching = false,
                                        errorMessage = throwable.toUserMessage(),
                                    )
                                }
                            }
                    }
                    emit(Unit)
                }
            }
            .launchIn(viewModelScope)

        selectedCityFlow
            .flatMapLatest { city ->
                flow<Unit> {
                    if (city == null) {
                        _uiState.update { it.copy(isLoadingRankings = false) }
                    } else {
                        _uiState.update { it.copy(isLoadingRankings = true, errorMessage = null) }
                        getActivityRankingsUseCase(city)
                            .onSuccess { result ->
                                _uiState.update {
                                    it.copy(
                                        rankedActivities = result.rankedActivities,
                                        dailyForecasts = result.forecast.dailyForecasts,
                                        isLoadingRankings = false,
                                    )
                                }
                            }
                            .onFailure { throwable ->
                                _uiState.update {
                                    it.copy(
                                        isLoadingRankings = false,
                                        errorMessage = throwable.toUserMessage(),
                                    )
                                }
                            }
                    }
                    emit(Unit)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        val editingAwayFromSelection = _uiState.value.selectedCity?.let { query != it.displayLabel } == true

        _uiState.update {
            it.copy(
                searchQuery = query,
                errorMessage = null,
                selectedCity = if (editingAwayFromSelection) null else it.selectedCity,
                rankedActivities = if (editingAwayFromSelection) emptyList() else it.rankedActivities,
                dailyForecasts = if (editingAwayFromSelection) emptyList() else it.dailyForecasts,
            )
        }
        searchQueryFlow.value = query
        if (editingAwayFromSelection) {
            selectedCityFlow.value = null
        }
    }

    fun onCitySelected(city: City) {
        _uiState.update {
            it.copy(
                selectedCity = city,
                searchQuery = city.displayLabel,
                citySuggestions = emptyList(),
                errorMessage = null,
                rankedActivities = emptyList(),
                dailyForecasts = emptyList(),
            )
        }
        searchQueryFlow.value = city.displayLabel
        selectedCityFlow.value = city
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 350L
    }
}
