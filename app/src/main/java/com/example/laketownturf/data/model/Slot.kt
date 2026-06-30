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
)
