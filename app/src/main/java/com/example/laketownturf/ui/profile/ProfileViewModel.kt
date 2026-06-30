package com.example.laketownturf.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laketownturf.data.model.User
import com.example.laketownturf.data.model.UserStatus
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    val photoUrl: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val currentUser = authRepository.currentUser
        val uid = currentUser?.uid ?: return
        val photoUrl = currentUser.photoUrl?.toString()
        
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        isLoading = false,
                        editName = user?.name ?: "",
                        photoUrl = photoUrl
                    )
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(isEditing = true, editName = it.user?.name ?: "")
        }
    }

    fun onEditNameChange(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun saveName() {
        val uid = authRepository.currentUser?.uid ?: return
        val newName = _uiState.value.editName.trim()
        if (newName.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = userRepository.updateUser(uid, mapOf("name" to newName))
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isEditing = false, isSaving = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Failed to update name")
                    }
                },
            )
        }
    }

    fun logout() {
        authRepository.signOut()
        _uiState.update { it.copy(isLoggedOut = true) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
