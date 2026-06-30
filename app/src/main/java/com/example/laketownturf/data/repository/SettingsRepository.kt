package com.example.laketownturf.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate

data class AppSettings(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val maintenanceStartDate: String = "",
    val maintenanceEndDate: String = ""
) {
    fun isMaintenanceActiveForDate(date: LocalDate): Boolean {
        if (!maintenanceMode) return false
        
        if (maintenanceStartDate.isNotBlank() && maintenanceEndDate.isNotBlank()) {
            try {
                val start = LocalDate.parse(maintenanceStartDate)
                val end = LocalDate.parse(maintenanceEndDate)
                // If date is within range, it's active
                return !date.isBefore(start) && !date.isAfter(end)
            } catch (e: Exception) {
                // If parsing fails, fall back to global toggle
                return true
            }
        }
        
        // If no dates are set but toggle is true, it's globally active
        return true
    }
}

class SettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeSettings(): Flow<AppSettings> = callbackFlow {
        val registration = firestore.collection("settings").document("general")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // PERMISSION_DENIED happens normally on logout (auth token revoked).
                    // Emit default settings and close silently instead of crashing.
                    trySend(AppSettings())
                    close()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val settings = snapshot.toObject(AppSettings::class.java) ?: AppSettings()
                    trySend(settings)
                } else {
                    trySend(AppSettings())
                }
            }
            
        awaitClose { registration.remove() }
    }
}
