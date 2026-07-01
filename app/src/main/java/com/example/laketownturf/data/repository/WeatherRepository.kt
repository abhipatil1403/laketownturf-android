package com.example.laketownturf.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherInfo(
    val temperature: Double,
    val weatherCode: Int,
    val description: String,
    val isGoodForPlay: Boolean
)

class WeatherRepository {
    // Open-Meteo API for Lake Town Turf coordinates (approximate Kolkata: lat=22.57, lon=88.36)
    // Using simple HttpURLConnection so we don't need to add Retrofit just for one API call.
    suspend fun getCurrentWeather(): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=22.57&longitude=88.36&current_weather=true")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val current = json.getJSONObject("current_weather")
                
                val temp = current.getDouble("temperature")
                val code = current.getInt("weathercode")
                
                // Decode WMO weather code (https://open-meteo.com/en/docs)
                val description = getWeatherDescription(code)
                val isGoodForPlay = isGoodForPlay(code)
                
                Result.success(WeatherInfo(temp, code, description, isGoodForPlay))
            } else {
                Result.failure(Exception("Failed to fetch weather: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown weather"
        }
    }

    private fun isGoodForPlay(code: Int): Boolean {
        // Bad weather codes: Heavy Rain (65), Thunderstorms (95, 96, 99), Heavy Snow/Showers (75, 82, 86)
        val badCodes = listOf(65, 67, 75, 82, 86, 95, 96, 99)
        return !badCodes.contains(code)
    }
}
