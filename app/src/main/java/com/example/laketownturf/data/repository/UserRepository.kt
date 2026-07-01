package com.example.laketownturf.data.repository

import com.example.laketownturf.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for Firestore user document operations.
 * All user CRUD goes through this class.
 */
class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    /**
     * Creates a new user document in Firestore.
     * The document ID is the user's UID from Firebase Auth.
     */
    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches a user document by UID (one-shot read).
     */
    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes a user document in real-time via Firestore snapshot listener.
     * Emits updated User objects whenever the document changes.
     */
    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        var registration: ListenerRegistration? = null
        registration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // PERMISSION_DENIED happens naturally on logout.
                    // Close silently instead of propagating a fatal crash.
                    close()
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Updates specific fields of a user document.
     *
     * @param uid The user's UID
     * @param fields Map of field names to their new values
     */
    suspend fun updateUser(uid: String, fields: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(uid).update(fields).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a user document exists for the given UID.
     */
    suspend fun userExists(uid: String): Boolean {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates the FCM token for a user.
     */
    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> {
        return updateUser(uid, mapOf("fcmToken" to token))
    }

    /**
     * Adds a player to the user's savedPlayers list.
     */
    suspend fun addSavedPlayer(uid: String, player: com.example.laketownturf.data.model.Player): Result<Unit> {
        return try {
            usersCollection.document(uid).update("savedPlayers", com.google.firebase.firestore.FieldValue.arrayUnion(player)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a player from the user's savedPlayers list.
     */
    suspend fun removeSavedPlayer(uid: String, player: com.example.laketownturf.data.model.Player): Result<Unit> {
        return try {
            usersCollection.document(uid).update("savedPlayers", com.google.firebase.firestore.FieldValue.arrayRemove(player)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
