package com.example.weatheractivityranker.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weatheractivityranker.domain.model.City
import com.example.weatheractivityranker.domain.model.DailyWeather
import com.example.weatheractivityranker.domain.model.RankedActivity
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onErrorDismissed()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Weather Activity Ranker") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchSection(
                    query = uiState.searchQuery,
                    isSearching = uiState.isSearching,
                    onQueryChange = onSearchQueryChanged,
                )
            }

            if (uiState.showSuggestions) {
                items(uiState.citySuggestions, key = { it.id }) { city ->
                    CitySuggestionItem(
                        cityLabel = city.displayLabel,
                        onClick = { onCitySelected(city) },
                    )
                }
            }

            if (uiState.isLoadingRankings) {
                item {
                    LoadingRankingsCard()
                }
            }

            if (uiState.showRankings) {
                item {
                    RankingsHeader(cityLabel = uiState.selectedCity?.displayLabel.orEmpty())
                }
                items(uiState.rankedActivities, key = { it.activity.name }) { ranked ->
                    RankedActivityCard(rankedActivity = ranked)
                }
                if (uiState.dailyForecasts.isNotEmpty()) {
                    item {
                        ForecastSection(forecasts = uiState.dailyForecasts)
                    }
                }
            } else if (!uiState.isLoadingRankings && uiState.selectedCity == null) {
                item {
                    EmptyStateCard()
                }
            }
        }
    }
}

@Composable
private fun SearchSection(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Search for a city",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("e.g. Berlin, Tokyo, Baku") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        if (isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CitySuggestionItem(
    cityLabel: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = cityLabel,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun LoadingRankingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Text("Loading 7-day forecast and rankings…")
        }
    }
}

@Composable
private fun RankingsHeader(cityLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Best activities for",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = cityLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Ranked by suitability over the next 7 days",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RankedActivityCard(rankedActivity: RankedActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#${rankedActivity.rank} ${rankedActivity.activity.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${rankedActivity.score}/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ScoreProgressBar(
                score = rankedActivity.score,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = rankedActivity.summary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ScoreProgressBar(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val progressShape = RoundedCornerShape(8.dp)
    val clampedProgress = (score / 100f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(progressShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clampedProgress)
                .clip(progressShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ForecastSection(forecasts: List<DailyWeather>) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "7-day forecast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            forecasts.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatter.format(day.date))
                    Text(
                        text = buildString {
                            append("${day.temperatureMinCelsius.toInt()}–${day.temperatureMaxCelsius.toInt()}°C")
                            append(" · ${day.precipitationMm}mm rain")
                            if (day.snowfallCm > 0) append(" · ${day.snowfallCm}cm snow")
                            append(" · ${day.windSpeedMaxKmh.toInt()}km/h wind")
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Enter a city name to see activity recommendations for the next 7 days.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
