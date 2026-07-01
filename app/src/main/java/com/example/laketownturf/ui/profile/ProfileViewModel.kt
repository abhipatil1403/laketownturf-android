package com.example.laketownturf.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laketownturf.data.model.User
import com.example.laketownturf.data.model.UserStatus
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.UserRepository
import com.example.laketownturf.utils.ErrorMessageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.laketownturf.data.repository.BookingRepository

data class UserStats(
    val totalMatches: Int = 0,
    val totalHours: Int = 0,
    val guestsInvited: Int = 0,
    val totalSpent: Double = 0.0,
    val favDay: String = "N/A",
    val favTime: String = "N/A",
    val currentStreak: Int = 0,
    val monthMatches: Int = 0,
    val monthHours: Int = 0,
    val monthSpent: Double = 0.0,
    val monthFavDay: String = "N/A",
    val unlockedBadges: List<String> = emptyList()
)

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    val photoUrl: String? = null,
    val stats: UserStats = UserStats(),
    val savedPlayers: List<com.example.laketownturf.data.model.Player> = emptyList()
)

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val bookingRepository: BookingRepository = BookingRepository()
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
            bookingRepository.getUserBookingsFlow(uid).collect { result ->
                result.onSuccess { bookings ->
                    calculateStats(bookings)
                }
            }
        }
        
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        isLoading = false,
                        editName = user?.name ?: "",
                        photoUrl = photoUrl,
                        savedPlayers = user?.savedPlayers ?: emptyList()
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
                        it.copy(isSaving = false, error = ErrorMessageHelper.getFriendlyMessage(e))
                    }
                },
            )
        }
    }

    fun logout() {
        authRepository.signOut()
        _uiState.update { it.copy(isLoggedOut = true) }
    }

    fun removeSavedPlayer(player: com.example.laketownturf.data.model.Player) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val updatedPlayers = _uiState.value.savedPlayers.filter { it.name != player.name }
            _uiState.update { it.copy(savedPlayers = updatedPlayers) }
            userRepository.removeSavedPlayer(uid, player)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun calculateStats(bookings: List<com.example.laketownturf.data.model.Booking>) {
        val validBookings = bookings.filter { it.status == com.example.laketownturf.data.model.BookingStatus.CONFIRMED || it.status == com.example.laketownturf.data.model.BookingStatus.COMPLETED }
        
        val totalMatches = validBookings.size
        val totalHours = validBookings.size // assuming 1 slot = 1 hour
        val guestsInvited = validBookings.sumOf { it.guests.size }
        val totalSpent = validBookings.sumOf { it.amount }
        
        val dayFreq = validBookings.groupingBy { java.time.LocalDate.parse(it.date).dayOfWeek.name }.eachCount()
        val favDay = dayFreq.maxByOrNull { it.value }?.key?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "N/A"
        
        val timeFreq = validBookings.groupingBy { it.startTime }.eachCount()
        val favTime = timeFreq.maxByOrNull { it.value }?.key?.let { com.example.laketownturf.utils.TimeUtils.formatTime12hr(it) } ?: "N/A"
        
        // Month stats
        val currentMonth = java.time.LocalDate.now().month
        val currentYear = java.time.LocalDate.now().year
        val monthBookings = validBookings.filter { 
            val d = java.time.LocalDate.parse(it.date)
            d.month == currentMonth && d.year == currentYear
        }
        val monthMatches = monthBookings.size
        val monthHours = monthBookings.size
        val monthSpent = monthBookings.sumOf { it.amount }
        val monthDayFreq = monthBookings.groupingBy { java.time.LocalDate.parse(it.date).dayOfWeek.name }.eachCount()
        val monthFavDay = monthDayFreq.maxByOrNull { it.value }?.key?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "N/A"
        
        // Streak calculation
        var streak = 0
        var currentWeekDate = java.time.LocalDate.now()
        val datesPlayed = validBookings.map { java.time.LocalDate.parse(it.date) }.toSet()
        while (true) {
            val startOfWeek = currentWeekDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val endOfWeek = startOfWeek.plusDays(6)
            val playedThisWeek = datesPlayed.any { !it.isBefore(startOfWeek) && !it.isAfter(endOfWeek) }
            if (playedThisWeek) {
                streak++
                currentWeekDate = currentWeekDate.minusWeeks(1)
            } else {
                if (streak == 0) {
                    val lastWeekDate = currentWeekDate.minusWeeks(1)
                    val startOfLastWeek = lastWeekDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    val endOfLastWeek = startOfLastWeek.plusDays(6)
                    val playedLastWeek = datesPlayed.any { !it.isBefore(startOfLastWeek) && !it.isAfter(endOfLastWeek) }
                    if (playedLastWeek) {
                        streak++
                        currentWeekDate = lastWeekDate.minusWeeks(1)
                        continue
                    }
                }
                break
            }
        }
        
        // Badges
        val unlockedBadges = mutableListOf<String>()
        if (totalMatches >= 1) unlockedBadges.add("First Booking")
        if (totalMatches >= 50) unlockedBadges.add("Regular Player")
        if (totalMatches >= 100) unlockedBadges.add("100 Match Club")
        
        val weekendMatches = validBookings.count { 
            val d = java.time.LocalDate.parse(it.date).dayOfWeek
            d == java.time.DayOfWeek.SATURDAY || d == java.time.DayOfWeek.SUNDAY
        }
        if (weekendMatches >= 10) unlockedBadges.add("Weekend Warrior")
        
        val morningMatches = validBookings.count { it.startTime.substringBefore(":").toInt() < 12 }
        if (morningMatches >= 10) unlockedBadges.add("Early Bird")
        
        val nightMatches = validBookings.count { it.startTime.substringBefore(":").toInt() >= 18 }
        if (nightMatches >= 10) unlockedBadges.add("Night Owl")
        
        if (guestsInvited >= 10) unlockedBadges.add("Team Captain")
        
        _uiState.update { 
            it.copy(
                stats = UserStats(
                    totalMatches, totalHours, guestsInvited, totalSpent, favDay, favTime,
                    streak, monthMatches, monthHours, monthSpent, monthFavDay, unlockedBadges
                )
            )
        }
    }
}
