# Weather Activity Ranker

Native Android app that lets you search for a city and see a ranked list of activities suitable for that location over the next 7 days, based on Open-Meteo weather forecast data.

## Project overview

The user enters a city name, selects a match from Open-Meteo Geocoding results, and the app fetches a 7-day forecast. Four activities are scored and ranked:

- Skiing
- Surfing
- Outdoor sightseeing
- Indoor sightseeing

Rankings are computed on-device from forecast data. No backend is required.

## Platform and tooling choices

| Choice | Rationale |
|--------|-----------|
| **Kotlin + Jetpack Compose** | Modern declarative UI aligned with current Android best practices |
| **Material 3** | Accessible components with light/dark support via system theme |
| **Hilt** | Compile-time DI for testable, scoped dependencies |
| **Retrofit + kotlinx.serialization** | Lightweight, type-safe networking without a heavy JSON stack |
| **Coroutines + StateFlow** | Explicit async and UI state modeling |
| **JUnit 4 + MockK** | Fast JVM unit tests for domain and ViewModel logic |

- **minSdk**: 24  
- **targetSdk / compileSdk**: 35  
- **JDK**: 11  

## Architecture and technical decisions

The project follows **Clean Architecture** with **MVVM** in the presentation layer:

```
presentation/   → Compose UI, explicit UiState, HomeViewModel
domain/         → models, ActivityRankingEngine, use cases, repository interfaces
data/           → Retrofit APIs, DTOs, mappers, repository implementation
di/             → Hilt modules (network, repository, domain)
```

**Key decisions:**

1. **Use cases** (`SearchCitiesUseCase`, `GetActivityRankingsUseCase`) keep ViewModels thin and orchestration testable.
2. **`ActivityRankingEngine`** is pure Kotlin with no Android dependencies — easy to unit test and reason about.
3. **`WeatherRepository` interface** allows mocking the data layer in ViewModel tests.
4. **Explicit `HomeUiState`** exposed via `StateFlow`; the ViewModel exposes action methods (`onSearchQueryChanged`, `onCitySelected`, etc.) — classic MVVM, not MVI.
5. **Debounced search** (350 ms) reduces Geocoding API calls while typing.
6. **`flatMapLatest` request cancellation** — search and rankings flows cancel in-flight work when the query or selected city changes, preventing stale responses from overwriting newer state.
7. **Typed errors** (`AppError`) map network, not-found, and invalid-response cases to user-facing messages.

## How to build and run

### Prerequisites

- Android Studio Ladybug or newer (or compatible CLI setup)
- JDK 11+
- Android SDK 35

### Run on device/emulator

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on an emulator or device with internet access.

### Command line

```bash
./gradlew assembleDebug
./gradlew installDebug   # requires connected device/emulator
```

## How to run tests & testing strategy

```bash
./gradlew test
```

| Test class | What it covers |
|------------|----------------|
| `ActivityRankingEngineTest` | Scoring and ranking logic across cold/snowy, mild/dry, and rainy weeks |
| `WeatherMappersTest` | DTO → domain mapping, empty payloads, and mismatched array lengths |
| `WeatherDisplayFormatTest` | Locale-aware forecast number rounding and decimal formatting |
| `HomeViewModelTest` | Debounced search, city selection, stale response cancellation, loading/error state, error dismissal |

**Strategy:** Unit tests target the highest-risk logic first: the ranking engine (core product value), API mapping edge cases, and ViewModel state transitions. The ViewModel is tested against mocked use cases so presentation logic stays isolated from networking. Compose UI tests are intentionally out of scope for this exercise to keep focus on correctness and maintainability within the time budget.

**What is not covered (yet):** end-to-end repository tests with MockWebServer, instrumented UI tests, and snapshot tests. These would be natural next steps before a production release.

## API usage notes

### Geocoding API

- Base URL: `https://geocoding-api.open-meteo.com/`
- Endpoint: `GET /v1/search?name={query}&count=10`
- Used for city autocomplete after debounced user input.

### Forecast API

- Base URL: `https://api.open-meteo.com/`
- Endpoint: `GET /v1/forecast`
- Parameters: `latitude`, `longitude`, `timezone=auto`, `forecast_days=7`
- Daily fields requested:
  - `temperature_2m_max`, `temperature_2m_min`
  - `precipitation_sum`
  - `snowfall_sum`
  - `wind_speed_10m_max`

Both APIs are free, require no API key, and need the `INTERNET` permission.

## Activity recommendation logic

For each of the 7 forecast days, each activity receives a **0–100 suitability score**. Final ranking uses the **average daily score**.

| Activity | Primary signals |
|----------|-----------------|
| **Skiing** | Cold average temps (ideal ≈ −2 °C), snowfall, sub-freezing daily max |
| **Surfing** | Moderate wind (ideal ≈ 28 km/h), mild temps, low precipitation |
| **Outdoor sightseeing** | Comfortable temps (ideal ≈ 20 °C), dry days, light wind |
| **Indoor sightseeing** | Inverse of pleasant outdoor weather: rain, extremes, strong wind |

Scoring uses weighted combinations of Gaussian curves (for “ideal” ranges) and threshold buckets (for rain/snow). Each ranked item includes a short human-readable summary (e.g. number of dry or snowy days).

## Assumptions made

1. **City = geocoding result** — the user must pick a suggestion; free-text without selection does not fetch a forecast.
2. **7-day daily aggregates** are sufficient; hourly data is not used.
3. **Snowfall and precipitation** from Open-Meteo proxy skiing conditions; no ski-resort or snow-depth data.
4. **Surfing** is inferred from wind and general weather only — no wave height or ocean proximity.
5. **Indoor sightseeing** is a weather-driven fallback, not venue-specific.
6. **English** UI and geocoding language for consistency.
7. **Network required** — no offline cache in this version.

## Trade-offs and omissions

| Included | Omitted (by design) |
|----------|---------------------|
| Clean layering, DI, typed errors | Offline cache |
| Ranking engine unit tests | Full Compose/UI test suite |
| 7-day forecast summary in UI | Pull-to-refresh |
| Debounced search + `flatMapLatest` stale-request cancellation | Advanced animations / polish |
| Locale-aware forecast number formatting | Snapshot tests |
| Light & dark theme via Material 3 | |

Prioritized **architecture, correctness, and testability** over visual polish and feature volume, per the brief.

## Production-readiness notes

Before shipping to production, consider:

- **Error analytics** and structured logging (remove BODY-level HTTP logging)
- **Retry/backoff** for transient network failures
- **Rate limiting** for API abuse (debounce and `flatMapLatest` already cancel in-flight stale responses)
- **Accessibility** audit (content descriptions, TalkBack)
- **ProGuard/R8** rules for Retrofit/serialization models
- **Localization** (`strings.xml`, locale-aware geocoding; forecast numbers already use locale-aware `DecimalFormat`, UI copy is still English-only)
- **Certificate pinning** if threat model requires it

## AI usage disclosure

I used Cursor (AI-assisted IDE) as a development tool during this exercise. It helped with:

- **README drafting** — initial structure and section layout; I reviewed and edited the final content.
- **Unit test scaffolding** — boilerplate for `HomeViewModelTest`, `ActivityRankingEngineTest`, and mapper tests; I adjusted assertions and scenarios to match the actual behaviour I wanted to verify.
- **Boilerplate generation** — Gradle setup, Retrofit/Hilt wiring, and repetitive Compose/UI code.

I did **not** delegate architectural or product decisions to AI. The following were defined and validated by me:

- Clean Architecture + MVVM structure and layer boundaries
- Activity scoring rules and weightings in `ActivityRankingEngine`
- Error handling approach (`AppError`, repository mapping, user-facing messages)
- Trade-offs documented in this README



