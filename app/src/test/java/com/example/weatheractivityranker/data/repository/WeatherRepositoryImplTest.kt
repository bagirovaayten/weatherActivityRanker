package com.example.weatheractivityranker.data.repository

import com.example.weatheractivityranker.data.remote.ForecastApi
import com.example.weatheractivityranker.data.remote.GeocodingApi
import com.example.weatheractivityranker.domain.model.AppError
import com.example.weatheractivityranker.domain.model.City
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.time.LocalDate

class WeatherRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: WeatherRepositoryImpl

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
        server = MockWebServer()
        server.start()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val client = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        repository = WeatherRepositoryImpl(
            geocodingApi = retrofit.create(GeocodingApi::class.java),
            forecastApi = retrofit.create(ForecastApi::class.java),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `searchCities maps successful geocoding response`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(
                    """
                    {
                      "results": [
                        {
                          "id": 1,
                          "name": "Berlin",
                          "latitude": 52.52,
                          "longitude": 13.41,
                          "country": "Germany",
                          "admin1": "Berlin"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.searchCities("Berlin")

        assertTrue(result.isSuccess)
        assertEquals(listOf(berlin), result.getOrNull())
    }

    @Test
    fun `searchCities returns not found when geocoding has no matches`() = runTest {
        server.enqueue(MockResponse().setBody("""{ "results": [] }"""))

        val result = repository.searchCities("Nowhere")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.NotFound)
    }

    @Test
    fun `getForecast maps successful forecast response`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(
                    """
                    {
                      "daily": {
                        "time": ["2025-06-10"],
                        "temperature_2m_max": [22.0],
                        "temperature_2m_min": [14.0],
                        "precipitation_sum": [0.0],
                        "snowfall_sum": [0.0],
                        "wind_speed_10m_max": [12.0]
                      }
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.getForecast(berlin)

        assertTrue(result.isSuccess)
        val forecast = result.getOrNull()!!
        assertEquals(berlin, forecast.city)
        assertEquals(LocalDate.of(2025, 6, 10), forecast.dailyForecasts.single().date)
        assertEquals(22.0, forecast.dailyForecasts.single().temperatureMaxCelsius, 0.001)
    }

    @Test
    fun `searchCities maps server error to network app error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = repository.searchCities("Berlin")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as AppError.Network
        assertTrue(error.message!!.contains("503"))
    }

    @Test
    fun `getForecast maps malformed json to invalid response`() = runTest {
        server.enqueue(MockResponse().setBody("""{ "daily": { "time": [1, 2, 3] } }"""))

        val result = repository.getForecast(berlin)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.InvalidResponse)
    }

    @Test
    fun `getForecast maps missing daily payload to invalid response`() = runTest {
        server.enqueue(MockResponse().setBody("""{ "daily": null }"""))

        val result = repository.getForecast(berlin)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.InvalidResponse)
    }
}
