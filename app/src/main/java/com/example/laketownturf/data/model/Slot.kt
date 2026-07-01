package com.example.laketownturf.data.model

data class Slot(
    val slotId: String = "",
    val date: String = "", // Format: YYYY-MM-DD
    val startTime: String = "", // Format: HH:mm (24-hour)
    val endTime: String = "",
    val isBooked: Boolean = false,
    val bookedBy: String? = null,
    val price: Double = 500.0,
    val waitlistUsers: List<String> = emptyList()
) {
    fun isPast(): Boolean {
        try {
            val dateObj = java.time.LocalDate.parse(date)
            val timeObj = java.time.LocalTime.parse(startTime)
            val slotDateTime = java.time.LocalDateTime.of(dateObj, timeObj)
            return slotDateTime.isBefore(java.time.LocalDateTime.now())
        } catch (e: Exception) {
            return true
        }
    }
}
