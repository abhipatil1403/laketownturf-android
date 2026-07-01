package com.example.laketownturf.data.model

/**
 * Represents a user in the Lake Town Turf system.
 * Maps directly to a document in the `users` Firestore collection.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val flatNo: String = "",
    val type: String = UserType.SOCIETY,
    val address: String = "",
    val maintenanceCleared: Boolean = false,
    val status: String = UserStatus.PENDING,
    val role: String = UserRole.USER,
    val fcmToken: String? = null,
    val revocationReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val savedPlayers: List<Player> = emptyList()
)

/** User type constants matching Firestore field values. */
object UserType {
    const val SOCIETY = "society"
    const val OUTSIDER = "outsider"
}

/** Account status constants matching Firestore field values. */
object UserStatus {
    const val PENDING = "pending"
    const val ACTIVE = "active"
    const val REJECTED = "rejected"
}

/** User role constants matching Firestore field values. */
object UserRole {
    const val USER = "user"
    const val ADMIN = "admin"
}
