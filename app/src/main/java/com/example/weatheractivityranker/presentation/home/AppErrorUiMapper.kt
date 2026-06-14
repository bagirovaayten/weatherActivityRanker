package com.example.weatheractivityranker.presentation.home

import com.example.weatheractivityranker.domain.model.AppError

fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network -> message ?: "Network error. Check your connection and try again."
    is AppError.NotFound -> message ?: "No results found."
    is AppError.InvalidResponse -> message ?: "Unexpected response from weather service."
    is AppError.Unknown -> message ?: "Something went wrong. Please try again."
}
