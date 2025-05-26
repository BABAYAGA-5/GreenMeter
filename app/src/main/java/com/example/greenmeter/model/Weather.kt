package com.example.greenmeter.model

data class Weather(
    val temperature: Double = 0.0,
    val condition: String = "",
    val humidity: Int = 0,
    val visibility: Int = 0,
    val windSpeed: Double = 0.0,
    val windDirection: String = "",
    val location: String = ""
) 