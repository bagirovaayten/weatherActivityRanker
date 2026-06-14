package com.example.weatheractivityranker.presentation.home

import com.example.weatheractivityranker.domain.model.ActivityType
import com.example.weatheractivityranker.domain.model.AppError
import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.DailyWeather
import com.example.weatheractivityranker.domain.model.RankedActivity
import com.example.weatheractivityranker.domain.model.WeatherForecast
import com.example.weatheractivityranker.domain.usecase.ActivityRankingsResult
import com.example.weatheractivityranker.domain.usecase.GetActivityRankingsUseCase
import com.example.weatheractivityranker.domain.usecase.SearchCitiesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val searchCitiesUseCase: SearchCitiesUseCase = mockk()
    private val getActivityRankingsUseCase: GetActivityRankingsUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: HomeViewModel

    private val berlin = City(
        id = 1L,
        name = "Berlin",
        country = "Germany",
        admin1 = "Berlin",
        latitude = 52.52,
        longitude = 13.41,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search query debounced and updates suggestions`() = runTest {
        coEvery { searchCitiesUseCase("Ber") } returns Result.success(listOf(berlin))
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onSearchQueryChanged("Ber")
        advanceTimeBy(400)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Ber", state.searchQuery)
        assertEquals(listOf(berlin), state.citySuggestions)
        assertFalse(state.isSearching)
    }

    @Test
    fun `selecting city loads rankings`() = runTest {
        val forecast = WeatherForecast(
            city = berlin,
            dailyForecasts = listOf(
                DailyWeather(
                    date = LocalDate.of(2025, 6, 10),
                    temperatureMaxCelsius = 22.0,
                    temperatureMinCelsius = 14.0,
                    precipitationMm = 0.0,
                    snowfallCm = 0.0,
                    windSpeedMaxKmh = 12.0,
                ),
            ),
        )
        val ranked = listOf(
            RankedActivity(
                activity = ActivityType.OUTDOOR_SIGHTSEEING,
                score = 80,
                rank = 1,
                summary = "Nice week",
            ),
        )
        coEvery { getActivityRankingsUseCase(berlin) } returns Result.success(
            ActivityRankingsResult(forecast = forecast, rankedActivities = ranked),
        )
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onCitySelected(berlin)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(berlin, state.selectedCity)
        assertEquals(ranked, state.rankedActivities)
        assertTrue(state.showRankings)
        coVerify(exactly = 1) { getActivityRankingsUseCase(berlin) }
    }

    @Test
    fun `rankings failure exposes error in state`() = runTest {
        coEvery { getActivityRankingsUseCase(berlin) } returns Result.failure(AppError.Network())
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onCitySelected(berlin)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.showRankings)
        assertFalse(viewModel.uiState.value.isLoadingRankings)
    }

    @Test
    fun `blank search clears suggestions without calling use case`() = runTest {
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onSearchQueryChanged("")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.citySuggestions.isEmpty())
        coVerify(exactly = 0) { searchCitiesUseCase(any()) }
    }

    @Test
    fun `changing query after selection clears selected city`() = runTest {
        coEvery { getActivityRankingsUseCase(berlin) } returns Result.success(
            ActivityRankingsResult(
                forecast = WeatherForecast(berlin, emptyList()),
                rankedActivities = emptyList(),
            ),
        )
        coEvery { searchCitiesUseCase("Paris") } returns Result.success(emptyList())
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onCitySelected(berlin)
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("Paris")
        advanceTimeBy(400)

        assertNull(viewModel.uiState.value.selectedCity)
    }

    @Test
    fun `selecting another city cancels stale rankings response`() = runTest {
        val paris = City(
            id = 2L,
            name = "Paris",
            country = "France",
            admin1 = null,
            latitude = 48.85,
            longitude = 2.35,
        )
        val berlinRanked = listOf(
            RankedActivity(
                activity = ActivityType.OUTDOOR_SIGHTSEEING,
                score = 40,
                rank = 1,
                summary = "Berlin week",
            ),
        )
        val parisRanked = listOf(
            RankedActivity(
                activity = ActivityType.OUTDOOR_SIGHTSEEING,
                score = 90,
                rank = 1,
                summary = "Paris week",
            ),
        )
        coEvery { getActivityRankingsUseCase(berlin) } coAnswers {
            delay(1_000)
            Result.success(
                ActivityRankingsResult(
                    forecast = WeatherForecast(berlin, emptyList()),
                    rankedActivities = berlinRanked,
                ),
            )
        }
        coEvery { getActivityRankingsUseCase(paris) } returns Result.success(
            ActivityRankingsResult(
                forecast = WeatherForecast(paris, emptyList()),
                rankedActivities = parisRanked,
            ),
        )
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onCitySelected(berlin)
        viewModel.onCitySelected(paris)
        advanceUntilIdle()

        assertEquals(paris, viewModel.uiState.value.selectedCity)
        assertEquals(parisRanked, viewModel.uiState.value.rankedActivities)
        coVerify(exactly = 1) { getActivityRankingsUseCase(paris) }
    }

    @Test
    fun `newer search cancels stale suggestions response`() = runTest {
        coEvery { searchCitiesUseCase("Ber") } coAnswers {
            delay(1_000)
            Result.success(listOf(berlin))
        }
        coEvery { searchCitiesUseCase("Tok") } returns Result.success(
            listOf(
                City(
                    id = 3L,
                    name = "Tokyo",
                    country = "Japan",
                    admin1 = null,
                    latitude = 35.68,
                    longitude = 139.69,
                ),
            ),
        )
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onSearchQueryChanged("Ber")
        advanceTimeBy(400)
        viewModel.onSearchQueryChanged("Tok")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals("Tokyo, Japan", viewModel.uiState.value.citySuggestions.single().displayLabel)
        coVerify(exactly = 1) { searchCitiesUseCase("Tok") }
    }

    @Test
    fun `onErrorDismissed clears error message`() = runTest {
        coEvery { getActivityRankingsUseCase(berlin) } returns Result.failure(AppError.Network())
        viewModel = HomeViewModel(searchCitiesUseCase, getActivityRankingsUseCase)

        viewModel.onCitySelected(berlin)
        advanceUntilIdle()
        viewModel.onErrorDismissed()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
