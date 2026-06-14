package com.example.weatheractivityranker.domain.model

sealed class AppError(message: String) : Exception(message) {
    class Network(message: String = "Network error. Check your connection and try again.") : AppError(message)
    class NotFound(message: String = "No results found.") : AppError(message)
    class InvalidResponse(message: String = "Unexpected response from weather service.") : AppError(message)
    class Unknown(message: String = "Something went wrong. Please try again.") : AppError(message)
}
