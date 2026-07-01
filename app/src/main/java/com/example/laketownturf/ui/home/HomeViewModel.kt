package com.example.laketownturf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laketownturf.data.model.Booking
import com.example.laketownturf.data.model.Player
import com.example.laketownturf.data.model.Guest
import com.example.laketownturf.data.model.Slot
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.BookingRepository
import com.example.laketownturf.data.repository.SettingsRepository
import com.example.laketownturf.data.repository.UserRepository
import com.example.laketownturf.data.api.ApiClient
import com.example.laketownturf.utils.ErrorMessageHelper
import com.example.laketownturf.utils.PaymentManager
import com.example.laketownturf.utils.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PaymentOrderInfo(
    val orderId: String,
    val amountInPaise: Int,
    val userEmail: String,
    val userPhone: String
)

data class PendingBooking(
    val slot: Slot,
    val players: List<Player>,
    val guests: List<Guest>,
    val totalAmount: Double
)

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val slots: List<Slot> = emptyList(),
    val isLoading: Boolean = false,
    val isBooking: Boolean = false,
    val bookingSuccess: Boolean = false,
    val error: String? = null,
    val paymentError: String? = null,
    val isMaintenanceActive: Boolean = false,
    val maintenanceMessage: String? = null,
    val pendingPaymentOrder: PaymentOrderInfo? = null,
    val currentUserId: String? = null,
    val togglingWaitlistForSlotId: String? = null
)

class HomeViewModel(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val settingsRepository: SettingsRepository = SettingsRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private var pendingBooking: PendingBooking? = null

    init {
        _uiState.update { it.copy(currentUserId = authRepository.currentUser?.uid) }
        fetchSlotsForDate(_uiState.value.selectedDate)
        observeSettings()
        listenToPaymentResults()
    }

    private var currentSettings: com.example.laketownturf.data.repository.AppSettings? = null

    private fun listenToPaymentResults() {
        viewModelScope.launch {
            PaymentManager.paymentResult.collect { result ->
                when (result) {
                    is PaymentResult.Success -> {
                        val paymentData = result.paymentData
                        val orderId = paymentData.orderId
                        val paymentId = paymentData.paymentId
                        val signature = paymentData.signature
                        verifyAndBook(orderId, paymentId, signature)
                    }
                    is PaymentResult.Error -> {
                        val friendlyMessage = ErrorMessageHelper.parseRazorpayError(result.description)
                        _uiState.update { it.copy(isBooking = false, paymentError = "Payment Failed: $friendlyMessage") }
                        pendingBooking = null
                    }
                }
            }
        }
    }

    fun clearPaymentError() {
        _uiState.update { it.copy(paymentError = null) }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                currentSettings = settings
                _uiState.update { currentState ->
                    val isMaintenance = settings.isMaintenanceActiveForDate(currentState.selectedDate)
                    currentState.copy(
                        isMaintenanceActive = isMaintenance,
                        maintenanceMessage = if (isMaintenance) settings.maintenanceMessage else null
                    )
                }
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        val settings = currentSettings
        val isMaintenance = settings?.isMaintenanceActiveForDate(date) ?: false
        val msg = if (isMaintenance) settings.maintenanceMessage else null
        
        _uiState.update { 
            it.copy(
                selectedDate = date, 
                isLoading = true, 
                error = null,
                isMaintenanceActive = isMaintenance,
                maintenanceMessage = msg
            ) 
        }
        fetchSlotsForDate(date)
    }

    private fun fetchSlotsForDate(date: LocalDate) {
        fetchJob?.cancel() // Cancel previous listener if any
        fetchJob = viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            bookingRepository.getSlotsForDateFlow(dateStr).collect { result ->
                result.fold(
                    onSuccess = { slots ->
                        _uiState.update { it.copy(slots = slots, isLoading = false) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = ErrorMessageHelper.getFriendlyMessage(e), isLoading = false) }
                    }
                )
            }
        }
    }

    fun bookSlot(slot: Slot, players: List<Player>, guests: List<Guest>, totalAmount: Double) {
        val uid = authRepository.currentUser?.uid
        val email = authRepository.currentUser?.email ?: ""
        if (uid == null) {
            _uiState.update { it.copy(error = "User not logged in") }
            return
        }

        _uiState.update { it.copy(isBooking = true, error = null) }
        pendingBooking = PendingBooking(slot, players, guests, totalAmount)

        viewModelScope.launch {
            val amountInInr = totalAmount.toInt()
            val amountInPaise = amountInInr * 100
            val orderId = ApiClient.createRazorpayOrder(amountInInr)
            
            if (orderId != null) {
                _uiState.update { 
                    it.copy(
                        pendingPaymentOrder = PaymentOrderInfo(
                            orderId = orderId,
                            amountInPaise = amountInPaise,
                            userEmail = email,
                            userPhone = "" // Can be fetched from user doc if needed
                        )
                    )
                }
            } else {
                _uiState.update { it.copy(isBooking = false, error = "Failed to initialize payment") }
                pendingBooking = null
            }
        }
    }

    private fun verifyAndBook(orderId: String, paymentId: String, signature: String) {
        val booking = pendingBooking
        if (booking == null) {
            _uiState.update { it.copy(isBooking = false, error = "Booking context lost") }
            return
        }

        val uid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val isValid = ApiClient.verifyRazorpayPayment(orderId, paymentId, signature)
            if (isValid) {
                val result = bookingRepository.bookSlot(uid, booking.slot, booking.players, booking.guests, booking.totalAmount, orderId, paymentId, signature)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isBooking = false, bookingSuccess = true) }
                        com.example.laketownturf.utils.SharedBookingState.pendingRebookData = null
                        fetchSlotsForDate(_uiState.value.selectedDate)
                        
                        // Send success push notification
                        viewModelScope.launch {
                            val userResult = userRepository.getUser(uid)
                            if (userResult.isSuccess) {
                                val userToken = userResult.getOrNull()?.fcmToken
                                if (!userToken.isNullOrBlank()) {
                                    ApiClient.sendPushNotification(
                                        token = userToken,
                                        title = "Booking Confirmed!",
                                        body = "Your slot on ${booking.slot.date} at ${com.example.laketownturf.utils.TimeUtils.formatTime12hr(booking.slot.startTime)} is confirmed."
                                    )
                                }
                            }
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isBooking = false, paymentError = ErrorMessageHelper.getFriendlyMessage(e)) }
                    }
                )
            } else {
                _uiState.update { it.copy(isBooking = false, paymentError = "Payment verification failed") }
            }
            pendingBooking = null
        }
    }

    fun clearPendingPaymentOrder() {
        _uiState.update { it.copy(pendingPaymentOrder = null) }
    }

    fun clearBookingSuccess() {
        _uiState.update { it.copy(bookingSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun toggleWaitlist(slot: Slot) {
        val uid = authRepository.currentUser?.uid ?: return
        _uiState.update { it.copy(togglingWaitlistForSlotId = slot.slotId) }
        viewModelScope.launch {
            val result = bookingRepository.toggleWaitlist(slot, uid)
            _uiState.update { it.copy(togglingWaitlistForSlotId = null) }
            result.onFailure { e ->
                _uiState.update { it.copy(error = ErrorMessageHelper.getFriendlyMessage(e)) }
            }
        }
    }
}
