package com.example.laketownturf.ui.home

import android.app.Activity
import com.example.laketownturf.utils.PaymentManager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.laketownturf.data.model.Guest
import com.example.laketownturf.data.model.Player
import com.example.laketownturf.data.model.Slot
import com.example.laketownturf.theme.*
import com.example.laketownturf.ui.components.LTTButton
import com.example.laketownturf.ui.components.LTTTextField
import com.example.laketownturf.utils.TimeUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSlotToBook by remember { mutableStateOf<Slot?>(null) }
    val cs = MaterialTheme.colorScheme
    
    // SnackBar for errors or success
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    // Remove booking success snackbar, we will use a dialog instead.
    
    if (uiState.bookingSuccess) {
        AlertDialog(
            onDismissRequest = { /* Force user to click OK */ },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(48.dp)) },
            title = { Text("Booking Successful!") },
            text = { Text("Your payment was processed and your slot has been successfully booked. You can view it in your profile.") },
            confirmButton = {
                TextButton(onClick = { 
                    selectedSlotToBook = null
                    viewModel.clearBookingSuccess() 
                }) {
                    Text("Awesome!")
                }
            },
            containerColor = cs.surface,
            titleContentColor = cs.onSurface,
            textContentColor = cs.onSurfaceVariant
        )
    }

    if (uiState.paymentError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPaymentError() },
            icon = { Icon(Icons.Filled.Error, contentDescription = null, tint = DangerRed, modifier = Modifier.size(48.dp)) },
            title = { Text("Payment Failed") },
            text = { Text(uiState.paymentError ?: "An unknown error occurred during payment.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPaymentError() }) {
                    Text("Try Again", color = DangerRed)
                }
            },
            containerColor = cs.surface,
            titleContentColor = cs.onSurface,
            textContentColor = cs.onSurfaceVariant
        )
    }

    val context = LocalContext.current
    LaunchedEffect(uiState.pendingPaymentOrder) {
        uiState.pendingPaymentOrder?.let { order ->
            PaymentManager.startPayment(
                activity = context as Activity,
                orderId = order.orderId,
                amountInPaise = order.amountInPaise,
                userEmail = order.userEmail,
                userPhone = order.userPhone
            )
            viewModel.clearPendingPaymentOrder()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Text(
                text = "Book a Slot",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 0.dp, bottom = 16.dp)
            )

            // Date Picker Row
            DateSelector(
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::onDateSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Slots List or Maintenance Banner
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary)
                }
            } else if (uiState.isMaintenanceActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(cs.errorContainer, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        androidx.compose.material.icons.Icons.Default.let {
                            // Use standard alert icon
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                contentDescription = "Maintenance",
                                tint = cs.onErrorContainer,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bookings Paused",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.maintenanceMessage ?: "The turf is currently not accepting bookings. Please check back later.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onErrorContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (uiState.slots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No slots available for this date.", color = cs.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.slots) { slot ->
                        SlotCard(
                            slot = slot,
                            currentUserId = uiState.currentUserId,
                            isTogglingWaitlist = uiState.togglingWaitlistForSlotId == slot.slotId,
                            onClick = { 
                                if (!slot.isBooked) selectedSlotToBook = slot 
                            },
                            onToggleWaitlist = { viewModel.toggleWaitlist(slot) }
                        )
                    }
                }
            }
        }
    }

    // Booking Details Bottom Sheet
    if (selectedSlotToBook != null) {
        BookingDetailsSheet(
            slot = selectedSlotToBook!!,
            isBooking = uiState.isBooking,
            onDismiss = { selectedSlotToBook = null },
            onConfirm = { players, guests, totalAmount ->
                viewModel.bookSlot(selectedSlotToBook!!, players, guests, totalAmount)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsSheet(
    slot: Slot,
    isBooking: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<Player>, List<Guest>, Double) -> Unit
) {
    var players by remember { mutableStateOf(listOf(Player("", ""))) }
    var guests by remember { mutableStateOf(listOf<Guest>()) }
    var policyAgreed by remember { mutableStateOf(false) }
    val guestFee = 100.0
    val cs = MaterialTheme.colorScheme
    
    val totalAmount = slot.price + (guests.size * guestFee)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = cs.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Book Slot",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Close", modifier = Modifier.rotate(45f), tint = cs.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            
            // Slot Info Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val formattedDate = try {
                    LocalDate.parse(slot.date).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                } catch (e: Exception) { slot.date }
                Text(formattedDate, color = cs.onSurface)
                Text("${TimeUtils.formatTime12hr(slot.startTime)} - ${TimeUtils.formatTime12hr(slot.endTime)}", color = cs.onSurface, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Players Section
            Text("Resident Players", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            players.forEachIndexed { index, player ->
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Player ${index + 1}", color = cs.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (players.size > 1) {
                            IconButton(onClick = {
                                val updated = players.toMutableList()
                                updated.removeAt(index)
                                players = updated
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = DangerRed)
                            }
                        }
                    }
                    LTTTextField(
                        value = player.name,
                        onValueChange = { newName ->
                            val updated = players.toMutableList()
                            updated[index] = player.copy(name = newName)
                            players = updated
                        },
                        label = "Full Name"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        LTTTextField(
                            value = player.blockNo,
                            onValueChange = { newBlock ->
                                val updated = players.toMutableList()
                                updated[index] = player.copy(blockNo = newBlock)
                                players = updated
                            },
                            label = "Block",
                            placeholder = "e.g. D1",
                            keyboardCapitalization = KeyboardCapitalization.Characters,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        LTTTextField(
                            value = player.flatNo,
                            onValueChange = { newFlat ->
                                val updated = players.toMutableList()
                                updated[index] = player.copy(flatNo = newFlat)
                                players = updated
                            },
                            label = "Flat",
                            placeholder = "e.g. 101",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            TextButton(
                onClick = { players = players + Player("", "") },
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Resident Player")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = cs.outline)
            Spacer(modifier = Modifier.height(16.dp))

            // Guests Section
            Text("Guest Passes (₹100 each)", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            guests.forEachIndexed { index, guest ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = guest.name,
                        onValueChange = { newName ->
                            val updated = guests.toMutableList()
                            updated[index] = guest.copy(name = newName)
                            guests = updated
                        },
                        placeholder = { Text("Guest Name", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            unfocusedBorderColor = cs.outline,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val updated = guests.toMutableList()
                        updated.removeAt(index)
                        guests = updated
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = DangerRed)
                    }
                }
            }

            TextButton(
                onClick = { guests = guests + Guest("", 100.0) },
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Guest Pass")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Total Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Amount", color = cs.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                Text("₹${totalAmount.toInt()}", color = cs.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = policyAgreed,
                    onCheckedChange = { policyAgreed = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = cs.primary,
                        uncheckedColor = cs.outline
                    )
                )
                Text(
                    text = "I agree to the cancellation policy (Cancel up to 4 hours before slot time).",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

                val isFormValid = players.isNotEmpty() &&
                    players.all { it.name.isNotBlank() && it.blockNo.isNotBlank() && it.flatNo.isNotBlank() } &&
                    guests.all { it.name.isNotBlank() }

                LTTButton(
                    text = "Submit for Verification",
                    onClick = { 
                        onConfirm(players, guests, totalAmount)
                        onDismiss()
                    },
                    isLoading = isBooking,
                    enabled = policyAgreed && isFormValid,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val dates = (0..6).map { LocalDate.now().plusDays(it.toLong()) }
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val dateFormatter = DateTimeFormatter.ofPattern("dd")
    val cs = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            Card(
                modifier = Modifier
                    .width(64.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDateSelected(date) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) cs.primary else cs.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = date.format(dayFormatter).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) cs.onPrimary else cs.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date.format(dateFormatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) cs.onPrimary else cs.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SlotCard(
    slot: Slot,
    currentUserId: String?,
    isTogglingWaitlist: Boolean = false,
    onClick: () -> Unit,
    onToggleWaitlist: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme

    val isPast = try {
        val slotDateTime = LocalDateTime.parse("${slot.date}T${slot.startTime}:00")
        slotDateTime.isBefore(LocalDateTime.now())
    } catch (e: Exception) {
        false
    }
    val isUnavailable = slot.isBooked || isPast

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isUnavailable) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnavailable) cs.surfaceVariant.copy(alpha = 0.3f) else cs.surfaceVariant.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${TimeUtils.formatTime12hr(slot.startTime)} - ${TimeUtils.formatTime12hr(slot.endTime)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isUnavailable) cs.onSurfaceVariant.copy(alpha = 0.5f) else cs.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${slot.price.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnavailable) cs.onSurfaceVariant.copy(alpha=0.4f) else cs.primary
                )
            }
            
            if (slot.isBooked) {
                if (currentUserId != null && slot.bookedBy == currentUserId) {
                    // Show "Your Booking" label
                    Box(
                        modifier = Modifier
                            .background(cs.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Your Booking",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val isOnWaitlist = currentUserId != null && slot.waitlistUsers.contains(currentUserId)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isOnWaitlist) cs.primary else cs.surface, 
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isOnWaitlist) cs.primary else cs.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp)) // Fixes the click ripple bleed
                            .clickable(enabled = currentUserId != null && !isTogglingWaitlist) { onToggleWaitlist() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (isTogglingWaitlist) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = if (isOnWaitlist) cs.onPrimary else cs.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isOnWaitlist) cs.onPrimary else cs.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isOnWaitlist) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Opted In",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = cs.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Notify Me",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (isPast) {
                Box(
                    modifier = Modifier
                        .background(cs.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Passed",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(cs.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Book",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                }
            }
        }
    }
}
