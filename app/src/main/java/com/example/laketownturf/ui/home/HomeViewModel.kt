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
    val togglingWaitlistForSlotId: String? = null,
    val savedPlayers: List<Player> = emptyList(),
    val recommendedSlot: Slot? = null,
    val recommendationReason: String? = null,
    val deepLinkedSlot: Slot? = null
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
    private var lastBookingAttemptTime: Long = 0L

    private var currentSettings: com.example.laketownturf.data.repository.AppSettings? = null
    private var validBookingsCache: List<Booking> = emptyList()

    init {
        val uid = authRepository.currentUser?.uid
        _uiState.update { it.copy(currentUserId = uid) }
        
        if (uid != null) {
            viewModelScope.launch {
                bookingRepository.getUserBookingsFlow(uid).collect { result ->
                    val bookings = result.getOrNull() ?: emptyList()
                    validBookingsCache = bookings.filter { it.status == com.example.laketownturf.data.model.BookingStatus.CONFIRMED || it.status == com.example.laketownturf.data.model.BookingStatus.COMPLETED }
                    if (_uiState.value.slots.isNotEmpty()) {
                        calculateRecommendation(_uiState.value.slots)
                    }
                }
            }
        }
        
        fetchSlotsForDate(_uiState.value.selectedDate)
        observeSettings()
        listenToPaymentResults()
    }

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
                        
                        // Release lock on failure
                        val booking = pendingBooking
                        val uid = authRepository.currentUser?.uid
                        if (booking != null && uid != null) {
                            bookingRepository.releaseSlotLock(booking.slot.slotId, uid)
                        }
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
        
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                userRepository.observeUser(uid).collect { user ->
                    _uiState.update { it.copy(savedPlayers = user?.savedPlayers ?: emptyList()) }
                }
            }
        }
    }

    fun savePlayer(player: Player) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.addSavedPlayer(uid, player)
        }
    }

    fun removeSavedPlayer(player: Player) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.removeSavedPlayer(uid, player)
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
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        fetchJob?.cancel()
        _uiState.update { it.copy(isLoading = true, selectedDate = date, error = null, recommendedSlot = null, recommendationReason = null) }

        fetchJob = viewModelScope.launch {
            bookingRepository.getSlotsForDateFlow(dateStr).collect { result ->
                result.fold(
                    onSuccess = { slots ->
                        _uiState.update { it.copy(slots = slots, isLoading = false) }
                        calculateRecommendation(slots)
                        checkDeepLinkSlot(slots)
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = ErrorMessageHelper.getFriendlyMessage(e), isLoading = false) }
                    }
                )
            }
        }
    }

    private fun calculateRecommendation(slots: List<Slot>) {
        val validBookings = validBookingsCache
        if (validBookings.isEmpty()) {
            _uiState.update { it.copy(recommendedSlot = null, recommendationReason = null) }
            return
        }
        
        val availableSlots = slots.filter { !it.isBooked && !it.isPast() }
        if (availableSlots.isEmpty()) {
            _uiState.update { it.copy(recommendedSlot = null, recommendationReason = null) }
            return
        }
        
        // Find most frequent properties
        val timeFreq = validBookings.groupingBy { it.startTime }.eachCount()
        val favTime = timeFreq.maxByOrNull { it.value }?.key

        val dayFreq = validBookings.groupingBy { java.time.LocalDate.parse(it.date).dayOfWeek.name }.eachCount()
        val favDay = dayFreq.maxByOrNull { it.value }?.key
        
        val todayName = _uiState.value.selectedDate.dayOfWeek.name
        val isFavDay = todayName == favDay
        
        // Determine preferred period (Morning: <12, Afternoon: 12-17, Evening: >=17)
        val periodFreq = validBookings.groupingBy { 
            val hour = it.startTime.substringBefore(":").toInt()
            when {
                hour < 12 -> "Morning"
                hour < 17 -> "Afternoon"
                else -> "Evening"
            }
        }.eachCount()
        val favPeriod = periodFreq.maxByOrNull { it.value }?.key ?: "Evening"
        
        // Priority 1: Exact favorite time match
        if (favTime != null) {
            val matchingSlot = availableSlots.find { it.startTime == favTime }
            if (matchingSlot != null) {
                val reason = if (isFavDay) "Your favourite time on your favourite day! 🔥" else "You frequently play at ${com.example.laketownturf.utils.TimeUtils.formatTime12hr(favTime)}. This slot is available!"
                _uiState.update { it.copy(recommendedSlot = matchingSlot, recommendationReason = reason) }
                return
            }
        }
        
        // Priority 2: Preferred period match
        val periodSlots = availableSlots.filter { 
            val hour = it.startTime.substringBefore(":").toInt()
            val period = when {
                hour < 12 -> "Morning"
                hour < 17 -> "Afternoon"
                else -> "Evening"
            }
            period == favPeriod
        }
        
        if (periodSlots.isNotEmpty()) {
            val matchingSlot = periodSlots.first()
            val reason = if (isFavDay) "It's your favourite day to play! Here's an open slot in the $favPeriod." else "You usually play in the $favPeriod. This slot is open!"
            _uiState.update { it.copy(recommendedSlot = matchingSlot, recommendationReason = reason) }
            return
        }
        
        // Priority 3: Fallback (Earliest available)
        val recommended = availableSlots.first()
        _uiState.update { it.copy(recommendedSlot = recommended, recommendationReason = "Earliest available slot today.") }
    }

    fun bookSlot(slot: Slot, players: List<Player>, guests: List<Guest>, totalAmount: Double) {
        val uid = authRepository.currentUser?.uid
        val email = authRepository.currentUser?.email ?: ""
        val phone = authRepository.currentUser?.phoneNumber ?: ""
        
        if (uid == null) {
            _uiState.update { it.copy(error = "User not logged in.") }
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastBookingAttemptTime < 10000) {
            _uiState.update { it.copy(error = "Please wait a few seconds before trying again.") }
            return
        }
        lastBookingAttemptTime = now

        _uiState.update { it.copy(isBooking = true, error = null, paymentError = null) }
        pendingBooking = PendingBooking(slot, players, guests, totalAmount)

        viewModelScope.launch {
            try {
                // Reserve slot in Firestore before creating Razorpay order
                val reserveResult = bookingRepository.reserveSlotForPayment(slot.slotId, uid)
                if (reserveResult.isFailure) {
                    _uiState.update { it.copy(isBooking = false, error = reserveResult.exceptionOrNull()?.message ?: "Failed to lock slot") }
                    pendingBooking = null
                    return@launch
                }
                val amountInPaise = (totalAmount * 100).toInt()
                val amountForApi = totalAmount.toInt()
                val orderId = ApiClient.createRazorpayOrder(amountForApi)
                
                if (orderId != null) {
                    _uiState.update { it.copy(
                        pendingPaymentOrder = PaymentOrderInfo(
                            orderId = orderId,
                            amountInPaise = amountInPaise,
                            userEmail = email,
                            userPhone = phone
                        )
                    ) }
                } else {
                    _uiState.update { it.copy(isBooking = false, paymentError = "Failed to initialize payment on the server. Make sure your Netlify environment variables are fully deployed.") }
                    pendingBooking = null
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBooking = false, paymentError = "Failed to initiate payment: ${e.message}") }
                pendingBooking = null
            }
        }
    }

    private var targetDeepLinkSlotId: String? = null

    fun handleDeepLink(dateStr: String?, slotId: String?) {
        if (dateStr != null) {
            try {
                val targetDate = LocalDate.parse(dateStr)
                if (targetDate != _uiState.value.selectedDate) {
                    onDateSelected(targetDate)
                }
            } catch (e: Exception) {
                // Invalid date
            }
        }
        if (slotId != null) {
            targetDeepLinkSlotId = slotId
            checkDeepLinkSlot(_uiState.value.slots)
        }
    }

    private fun checkDeepLinkSlot(slots: List<Slot>) {
        if (targetDeepLinkSlotId != null && slots.isNotEmpty()) {
            val slot = slots.find { it.slotId == targetDeepLinkSlotId }
            if (slot != null && !slot.isBooked && !slot.isPast()) {
                _uiState.update { it.copy(deepLinkedSlot = slot) }
                targetDeepLinkSlotId = null
            }
        }
    }
    
    fun clearDeepLinkedSlot() {
        _uiState.update { it.copy(deepLinkedSlot = null) }
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
                        viewModelScope.launch { bookingRepository.releaseSlotLock(booking.slot.slotId, uid) }
                    }
                )
            } else {
                _uiState.update { it.copy(isBooking = false, paymentError = "Payment verification failed") }
                bookingRepository.releaseSlotLock(booking.slot.slotId, uid)
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
