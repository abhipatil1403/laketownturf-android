package com.example.laketownturf.data.repository

import com.example.laketownturf.data.model.Booking
import com.example.laketownturf.data.model.Player
import com.example.laketownturf.data.model.Guest
import com.example.laketownturf.data.model.Slot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.time.LocalDate

class BookingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val slotsCollection = db.collection("slots")
    private val bookingsCollection = db.collection("bookings")
    private val waitlistsCollection = db.collection("waitlists")

    /**
     * Observes all slots for a specific date (YYYY-MM-DD) in real-time.
     */
    fun getSlotsForDateFlow(dateStr: String): Flow<Result<List<Slot>>> = callbackFlow {
        try {
            val dateObj = LocalDate.parse(dateStr)
            val dayOfWeek = dateObj.dayOfWeek.value // 1 = Monday, 7 = Sunday

            // 1. Generate Virtual Slots
            val virtualSlots = mutableListOf<Slot>()
            
            if (dayOfWeek == 1) {
                // Monday: Closed (No slots)
            } else if (dayOfWeek in 2..5) {
                // Tuesday - Friday: 7-8 PM, 200 rs
                virtualSlots.add(Slot(slotId = "$dateStr-19:00", date = dateStr, startTime = "19:00", endTime = "20:00", price = 200.0))
            } else {
                // Saturday & Sunday: Morning 7-11 AM, Evening 4-8 PM, 300 rs
                val morningHours = listOf(7, 8, 9, 10)
                morningHours.forEach { h ->
                    val sTime = h.toString().padStart(2, '0') + ":00"
                    val eTime = (h + 1).toString().padStart(2, '0') + ":00"
                    virtualSlots.add(Slot(slotId = "$dateStr-$sTime", date = dateStr, startTime = sTime, endTime = eTime, price = 300.0))
                }
                
                val eveningHours = listOf(16, 17, 18, 19)
                eveningHours.forEach { h ->
                    val sTime = h.toString().padStart(2, '0') + ":00"
                    val eTime = (h + 1).toString().padStart(2, '0') + ":00"
                    virtualSlots.add(Slot(slotId = "$dateStr-$sTime", date = dateStr, startTime = sTime, endTime = eTime, price = 300.0))
                }
            }

            var currentOverrides = mapOf<String, Slot>()
            var currentBookings = mapOf<String, Booking>()
            var currentWaitlists = mapOf<String, List<String>>()

            fun emitFinalSlots() {
                val finalSlots = virtualSlots.map { vSlot ->
                    val override = currentOverrides[vSlot.slotId]
                    val booking = currentBookings[vSlot.slotId]
                    val waitlistUsers = currentWaitlists[vSlot.slotId] ?: emptyList()
                    
                    vSlot.copy(
                        price = override?.price ?: vSlot.price,
                        isBooked = booking != null,
                        bookedBy = booking?.uid,
                        waitlistUsers = waitlistUsers
                    )
                }
                trySend(Result.success(finalSlots))
            }

            // 2. Fetch Overrides
            val overridesReg = slotsCollection.whereEqualTo("date", dateStr).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Silently close on PERMISSION_DENIED (e.g. after logout)
                    close()
                    return@addSnapshotListener
                }
                currentOverrides = snapshot?.toObjects(Slot::class.java)?.associateBy { it.slotId } ?: emptyMap()
                emitFinalSlots()
            }

            // 3. Fetch Bookings
            val bookingsReg = bookingsCollection.whereEqualTo("date", dateStr).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Silently close on PERMISSION_DENIED (e.g. after logout)
                    close()
                    return@addSnapshotListener
                }
                currentBookings = snapshot?.toObjects(Booking::class.java)
                    ?.filter { it.status != com.example.laketownturf.data.model.BookingStatus.CANCELLED }
                    ?.associateBy { it.slotId } ?: emptyMap()
                emitFinalSlots()
            }

            // 4. Fetch Waitlists
            val waitlistsReg = waitlistsCollection.whereEqualTo("date", dateStr).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }
                val waitlistsMap = mutableMapOf<String, List<String>>()
                snapshot?.documents?.forEach { doc ->
                    val slotId = doc.getString("slotId")
                    val uids = doc.get("uids") as? List<String>
                    if (slotId != null && uids != null) {
                        waitlistsMap[slotId] = uids
                    }
                }
                currentWaitlists = waitlistsMap
                emitFinalSlots()
            }

            awaitClose {
                overridesReg.remove()
                bookingsReg.remove()
                waitlistsReg.remove()
            }
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close(e)
        }
    }

    /**
     * Toggles a user's presence on the waitlist for a specific slot.
     */
    suspend fun toggleWaitlist(slot: Slot, uid: String): Result<Unit> {
        return try {
            val waitlistRef = waitlistsCollection.document(slot.slotId)
            val isOnWaitlist = slot.waitlistUsers.contains(uid)
            
            if (isOnWaitlist) {
                // Remove from waitlist
                waitlistRef.update("uids", com.google.firebase.firestore.FieldValue.arrayRemove(uid)).await()
            } else {
                // Add to waitlist (create if not exists)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(waitlistRef)
                    if (snapshot.exists()) {
                        transaction.update(waitlistRef, "uids", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                    } else {
                        val data = mapOf(
                            "slotId" to slot.slotId,
                            "date" to slot.date,
                            "uids" to listOf(uid)
                        )
                        transaction.set(waitlistRef, data)
                    }
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Attempts to book a slot using a Firestore transaction to prevent double-booking.
     */
    suspend fun bookSlot(uid: String, slot: Slot, players: List<Player>, guests: List<Guest>, totalAmount: Double, razorpayOrderId: String, razorpayPaymentId: String, razorpaySignature: String): Result<Booking> {
        return try {
            val bookingRef = bookingsCollection.document()
            
            val uniqueBookingRef = bookingsCollection.document("booking_${slot.slotId}")
            val settingsRef = db.collection("settings").document("general")

            db.runTransaction { transaction ->
                // Check Maintenance Mode
                val settingsSnap = transaction.get(settingsRef)
                if (settingsSnap.exists()) {
                    val isMaintenance = settingsSnap.getBoolean("maintenanceMode") ?: false
                    if (isMaintenance) {
                        // For simplicity, we just fail it if maintenance mode is true globally. 
                        // If we wanted date-specific, we would check the date ranges here too.
                        val start = settingsSnap.getString("maintenanceStartDate") ?: ""
                        val end = settingsSnap.getString("maintenanceEndDate") ?: ""
                        if (start.isBlank() || end.isBlank()) {
                            throw Exception("Bookings are currently paused for maintenance.")
                        } else {
                            try {
                                val sDate = java.time.LocalDate.parse(start)
                                val eDate = java.time.LocalDate.parse(end)
                                val slotDate = java.time.LocalDate.parse(slot.date)
                                if (!slotDate.isBefore(sDate) && !slotDate.isAfter(eDate)) {
                                    throw Exception("This date is blocked for maintenance.")
                                }
                            } catch (e: Exception) {
                                throw Exception("Bookings are currently paused for maintenance.")
                            }
                        }
                    }
                }

                val snapshot = transaction.get(uniqueBookingRef)
                
                if (snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status != com.example.laketownturf.data.model.BookingStatus.CANCELLED) {
                        throw Exception("This slot was just booked by someone else.")
                    }
                }

                val booking = Booking(
                    bookingId = uniqueBookingRef.id,
                    uid = uid,
                    slotId = slot.slotId,
                    date = slot.date,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    amount = totalAmount,
                    players = players,
                    guests = guests,
                    razorpayOrderId = razorpayOrderId,
                    razorpayPaymentId = razorpayPaymentId,
                    razorpaySignature = razorpaySignature
                )
                
                // Create booking
                transaction.set(uniqueBookingRef, booking)
                
                booking // Return the booking object
            }.await()
            
            val successfulBooking = Booking(
                bookingId = uniqueBookingRef.id,
                uid = uid,
                slotId = slot.slotId,
                date = slot.date,
                startTime = slot.startTime,
                endTime = slot.endTime,
                amount = totalAmount,
                players = players,
                guests = guests,
                razorpayOrderId = razorpayOrderId,
                razorpayPaymentId = razorpayPaymentId,
                razorpaySignature = razorpaySignature
            )
            
            Result.success(successfulBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes a user's booking history, ordered by date descending in real-time.
     */
    fun getUserBookingsFlow(uid: String): Flow<Result<List<Booking>>> = callbackFlow {
        val registration = bookingsCollection
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Silently close on PERMISSION_DENIED (e.g. after logout)
                    close()
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(Booking::class.java)?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(Result.success(bookings))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Cancels a booking, issues a Razorpay refund, and updates the status to CANCELLED.
     */
    suspend fun cancelBooking(booking: Booking): Result<Unit> {
        return try {
            // Ensure payment info exists
            val paymentId = booking.razorpayPaymentId
            val amount = booking.amount

            if (paymentId == null || paymentId.isBlank()) {
                throw Exception("Cannot refund: No payment ID found.")
            }

            // Call the Netlify refund endpoint
            val isRefunded = com.example.laketownturf.data.api.ApiClient.refundRazorpayPayment(paymentId, amount)
            
            if (!isRefunded) {
                throw Exception("Failed to process refund through Razorpay.")
            }

            // Update Firestore document
            val updates = mapOf(
                "status" to com.example.laketownturf.data.model.BookingStatus.CANCELLED,
                "paymentStatus" to "refunded"
            )
            
            bookingsCollection.document(booking.bookingId).update(updates).await()
            
            // Notify waitlist users via Netlify function
            com.example.laketownturf.data.api.ApiClient.notifyWaitlist(
                slotId = booking.slotId,
                date = booking.date,
                startTime = booking.startTime,
                endTime = booking.endTime
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Temporary helper to generate mock slots for testing if none exist in the database.
     */
    private suspend fun generateMockSlotsForDate(date: String): List<Slot> {
        val slots = mutableListOf<Slot>()
        val startHours = listOf("06:00", "07:00", "08:00", "09:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00")
        
        for (startTime in startHours) {
            val endTimeInt = startTime.split(":")[0].toInt() + 1
            val endTime = "${endTimeInt.toString().padStart(2, '0')}:00"
            
            slots.add(
                Slot(
                    slotId = "$date-$startTime", // Deterministic ID so it can be booked
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    isBooked = false,
                    price = if (endTimeInt > 18) 800.0 else 500.0 // Prime time is more expensive
                )
            )
        }
        
        // Save them to Firestore so they are real
        db.runBatch { batch ->
            for (slot in slots) {
                batch.set(slotsCollection.document(slot.slotId), slot)
            }
        }.await()

        return slots
    }
}
