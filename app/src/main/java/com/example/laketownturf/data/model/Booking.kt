package com.example.laketownturf.data.model

data class Booking(
    val bookingId: String = "",
    val uid: String = "",
    val slotId: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = BookingStatus.PENDING_VERIFICATION,
    val paymentStatus: String = PaymentStatus.PAID,
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    
    // New fields for Phase 5
    val players: List<Player> = emptyList(),
    val guests: List<Guest> = emptyList(),
    
    // Cancellation
    val cancellationReason: String? = null,
    
    // Payment Details
    val razorpayOrderId: String? = null,
    val razorpayPaymentId: String? = null,
    val razorpaySignature: String? = null
)

data class Player(
    val name: String = "",
    val blockNo: String = "",
    val flatNo: String = ""
)

data class Guest(
    val name: String = "",
    val fee: Double = 100.0
)

object BookingStatus {
    const val PENDING_VERIFICATION = "pending_verification"
    const val CONFIRMED = "confirmed"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
}

object PaymentStatus {
    const val PENDING = "pending"
    const val PAID = "paid"
    const val FAILED = "failed"
}
