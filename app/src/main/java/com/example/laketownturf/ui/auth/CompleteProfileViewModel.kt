package com.example.laketownturf.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laketownturf.data.model.User
import com.example.laketownturf.data.model.UserStatus
import com.example.laketownturf.data.model.UserType
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Complete Profile flow.
 */
data class CompleteProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val blockNo: String = "",
    val flatNo: String = "",
    val address: String = "",
    val type: String = UserType.SOCIETY,
    val phoneError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,
    val isAgreedToTerms: Boolean = false,
)

class CompleteProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompleteProfileUiState())
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    init {
        // Pre-fill name and email from Google account
        authRepository.currentUser?.let { user ->
            _uiState.update {
                it.copy(
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                )
            }
        }
    }

    fun onPhoneChange(phone: String) {
        if (phone.length <= 10 && phone.all { it.isDigit() }) {
            _uiState.update { it.copy(phone = phone, phoneError = null, error = null) }
        }
    }

    fun onBlockNoChange(blockNo: String) {
        _uiState.update { it.copy(blockNo = blockNo, error = null) }
    }

    fun onFlatNoChange(flatNo: String) {
        _uiState.update { it.copy(flatNo = flatNo, error = null) }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address, error = null) }
    }

    fun onTypeChange(type: String) {
        _uiState.update { it.copy(type = type, error = null) }
    }

    fun onTermsAgreementChange(agreed: Boolean) {
        _uiState.update { it.copy(isAgreedToTerms = agreed) }
    }

    /**
     * Submits the profile to Firestore and sets the status to PENDING.
     */
    fun submitProfile() {
        val state = _uiState.value
        
        if (state.phone.length != 10) {
            _uiState.update { it.copy(phoneError = "Enter a valid 10-digit phone number") }
            return
        }
        
        if (state.type == UserType.SOCIETY && (state.flatNo.isBlank() || state.blockNo.isBlank())) {
            _uiState.update { it.copy(error = "Block and Flat number are required for society members") }
            return
        }

        if (state.type == UserType.OUTSIDER && state.address.isBlank()) {
            _uiState.update { it.copy(error = "Address is required for outsiders") }
            return
        }

        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _uiState.update { it.copy(error = "User not authenticated. Please sign in again.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val user = User(
                uid = currentUser.uid,
                name = state.name,
                email = state.email,
                phone = "+91${state.phone}",
                flatNo = if (state.type == UserType.SOCIETY) "${state.blockNo.trim()}-${state.flatNo.trim()}" else "N/A",
                type = state.type,
                address = if (state.type == UserType.OUTSIDER) state.address.trim() else "",
                status = UserStatus.PENDING, // Always pending until admin approval
                maintenanceCleared = false,
                role = "user"
            )

            val result = userRepository.createUser(user)
            result.fold(
                onSuccess = {
                    // Fetch and update FCM token
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener(
                        com.google.android.gms.tasks.OnCompleteListener { task ->
                            if (task.isSuccessful) {
                                viewModelScope.launch {
                                    userRepository.updateFcmToken(currentUser.uid, task.result)
                                }
                            }
                        }
                    )
                    _uiState.update { it.copy(isLoading = false, isComplete = true) }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(isLoading = false, error = exception.message ?: "Failed to save profile")
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
