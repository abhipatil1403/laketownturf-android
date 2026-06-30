package com.example.laketownturf.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laketownturf.data.model.UserStatus
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.UserRepository
import com.example.laketownturf.utils.ErrorMessageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.tasks.OnCompleteListener

/**
 * UI state for the sign-in flow.
 */
data class SignInUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val signInResult: SignInResult? = null,
)

/**
 * Possible outcomes after successful sign-in.
 */
sealed class SignInResult {
    /** User is active, proceed to main app. */
    data object Active : SignInResult()
    /** User is pending admin approval. */
    data object Pending : SignInResult()
    /** No user document found — new user needs to complete their profile. */
    data object NoAccount : SignInResult()
}

class SignInViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    /**
     * Initiates Google Sign-In and checks user status in Firestore.
     */
    fun signInWithGoogle(context: Context) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context)
            result.fold(
                onSuccess = { firebaseUser ->
                    checkUserStatus(firebaseUser.uid)
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = ErrorMessageHelper.getFriendlyMessage(exception),
                        )
                    }
                },
            )
        }
    }

    private suspend fun checkUserStatus(uid: String) {
        val userResult = userRepository.getUser(uid)
        userResult.fold(
            onSuccess = { user ->
                val signInResult = when {
                    user == null -> SignInResult.NoAccount
                    user.status == UserStatus.ACTIVE -> SignInResult.Active
                    else -> SignInResult.Pending
                }
                
                // Fetch and update FCM token if user exists
                if (user != null) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (task.isSuccessful) {
                            viewModelScope.launch {
                                userRepository.updateFcmToken(uid, task.result)
                            }
                        }
                    })
                }

                _uiState.update {
                    it.copy(isLoading = false, signInResult = signInResult)
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = ErrorMessageHelper.getFriendlyMessage(exception)
                    )
                }
            },
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
