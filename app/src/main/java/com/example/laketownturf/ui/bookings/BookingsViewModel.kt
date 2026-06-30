package com.example.laketownturf.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laketownturf.data.model.Booking
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingsUiState(
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val isCancelling: Boolean = false,
    val error: String? = null,
    val cancelError: String? = null
)

class BookingsViewModel(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    init {
        fetchUserBookings()
    }

    fun fetchUserBookings() {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            bookingRepository.getUserBookingsFlow(uid).collect { result ->
                result.fold(
                    onSuccess = { bookings ->
                        _uiState.update { it.copy(bookings = bookings, isLoading = false) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                )
            }
        }
    }

    fun clearCancelError() {
        _uiState.update { it.copy(cancelError = null) }
    }

    fun cancelBooking(booking: Booking) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, cancelError = null) }
            val result = bookingRepository.cancelBooking(booking)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isCancelling = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isCancelling = false, cancelError = e.message ?: "Failed to cancel booking") }
                }
            )
        }
    }
}
