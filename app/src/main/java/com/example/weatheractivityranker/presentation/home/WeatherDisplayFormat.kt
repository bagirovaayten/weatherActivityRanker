package com.example.weatheractivityranker.presentation.home

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

internal object WeatherDisplayFormat {
    private fun decimalFormat(pattern: String): DecimalFormat =
        DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.getDefault()))

    private val integerFormat = decimalFormat("#,##0")
    private val oneDecimalFormat = decimalFormat("#.#")

    fun temperatureCelsius(value: Double): String = integerFormat.format(value.roundToInt())

    fun precipitationMm(value: Double): String = formatOneDecimal(value)

    fun snowfallCm(value: Double): String = formatOneDecimal(value)

    fun windSpeedKmh(value: Double): String = integerFormat.format(value.roundToInt())

    private fun formatOneDecimal(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return oneDecimalFormat.format(rounded)
    }
}
