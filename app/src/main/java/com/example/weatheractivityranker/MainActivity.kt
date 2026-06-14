package com.example.weatheractivityranker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weatheractivityranker.presentation.home.HomeScreen
import com.example.weatheractivityranker.presentation.home.HomeViewModel
import com.example.weatheractivityranker.presentation.theme.WeatherActivityRankerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherActivityRankerTheme {
                val viewModel: HomeViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onCitySelected = viewModel::onCitySelected,
                    onErrorDismissed = viewModel::onErrorDismissed,
                )
            }
        }
    }
}
