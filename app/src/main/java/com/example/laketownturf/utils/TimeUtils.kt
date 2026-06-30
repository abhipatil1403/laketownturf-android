package com.example.laketownturf.utils

object TimeUtils {
    fun formatTime12hr(time24: String): String {
        return try {
            val parts = time24.split(":")
            var hour = parts[0].toInt()
            val minute = parts[1]
            val ampm = if (hour >= 12) "PM" else "AM"
            if (hour > 12) hour -= 12
            if (hour == 0) hour = 12
            "$hour:$minute $ampm"
        } catch (e: Exception) {
            time24
        }
    }
}
