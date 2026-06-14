package com.example.weatheractivityranker.domain.model

data class RankedActivity(
    val activity: ActivityType,
    val score: Int,
    val rank: Int,
    val summary: String,
)
