package com.example.laketownturf.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.laketownturf.data.model.Booking
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import com.example.laketownturf.data.model.BookingStatus
import com.example.laketownturf.theme.AmberCTA
import com.example.laketownturf.theme.DangerRed
import com.example.laketownturf.ui.components.LTTButton
import com.example.laketownturf.utils.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel = viewModel(),
    deepLinkBookingId: String? = null,
    onNavigateToReceipt: (Booking) -> Unit = {},
    onBookAgain: (Booking) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(deepLinkBookingId) {
        if (deepLinkBookingId != null) {
            viewModel.handleDeepLink(deepLinkBookingId)
        }
    }
    
    LaunchedEffect(uiState.deepLinkedBookingId, uiState.bookings) {
        val deepLinkedId = uiState.deepLinkedBookingId
        if (deepLinkedId != null && uiState.bookings.isNotEmpty()) {
            val index = uiState.bookings.indexOfFirst { it.bookingId == deepLinkedId }
            if (index != -1) {
                listState.animateScrollToItem(index)
                viewModel.clearDeepLinkedBooking()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchUserBookings()
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
        }
    }

    LaunchedEffect(uiState.cancelError) {
        uiState.cancelError?.let {
            snackbarHostState.showSnackbar("Cancellation Error: $it")
            viewModel.clearCancelError()
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
                .padding(start = 20.dp, end = 20.dp)
        ) {
            Text(
                text = "My Bookings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Track your past and upcoming games",
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary)
                }
            } else if (uiState.bookings.isEmpty()) {
                EmptyBookingsState()
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.bookings) { booking ->
                        BookingCard(
                            booking = booking,
                            isCancelling = uiState.isCancelling,
                            onCancel = { viewModel.cancelBooking(it) },
                            onBookAgain = onBookAgain
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    isCancelling: Boolean,
    onCancel: (Booking) -> Unit,
    onBookAgain: (Booking) -> Unit = {}
) {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val parsedDate = try {
        LocalDate.parse(booking.date).format(formatter)
    } catch (e: Exception) {
        booking.date
    }
    val cs = MaterialTheme.colorScheme

    val isConfirmed = booking.status == BookingStatus.CONFIRMED
    val isCancelled = booking.status == BookingStatus.CANCELLED

    val statusColor = when {
        isConfirmed -> cs.primary
        isCancelled -> DangerRed
        else -> AmberCTA
    }
    
    val statusText = when (booking.status) {
        BookingStatus.PENDING_VERIFICATION -> "PENDING"
        else -> booking.status.uppercase()
    }

    val slotStartDateTime = try {
        LocalDateTime.of(LocalDate.parse(booking.date), LocalTime.parse(booking.startTime))
    } catch (e: Exception) { null }

    val bookingTime = try {
        java.time.Instant.ofEpochMilli(booking.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    } catch(e: Exception) { null }

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            currentTime = LocalDateTime.now()
        }
    }

    var canCancel = false
    var timeRemainingString = ""
    
    if (slotStartDateTime != null && bookingTime != null && (isConfirmed || booking.status == BookingStatus.PENDING_VERIFICATION)) {
        val rule1 = slotStartDateTime.minusHours(4)
        val rule2 = bookingTime.plusMinutes(15)
        var deadline = if (rule1.isAfter(rule2)) rule1 else rule2
        
        if (deadline.isAfter(slotStartDateTime)) {
            deadline = slotStartDateTime
        }
        
        if (currentTime.isBefore(deadline)) {
            canCancel = true
            val duration = java.time.Duration.between(currentTime, deadline)
            val totalSeconds = duration.seconds
            val days = totalSeconds / 86400
            val hours = (totalSeconds % 86400) / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            timeRemainingString = if (days > 0) {
                String.format(java.util.Locale.getDefault(), "%dd %02d:%02d:%02d", days, hours, minutes, seconds)
            } else if (hours > 0) {
                String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        var wasCancelling by remember { mutableStateOf(false) }
        
        LaunchedEffect(isCancelling) {
            if (isCancelling) {
                wasCancelling = true
            } else if (wasCancelling) {
                showCancelDialog = false
                wasCancelling = false
            }
        }
        
        AlertDialog(
            onDismissRequest = { if (!isCancelling) showCancelDialog = false },
            title = { Text("Cancel Booking?") },
            text = { Text("Are you sure you want to cancel this booking? A refund will be processed immediately to your original payment method. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onCancel(booking) },
                    enabled = !isCancelling
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DangerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Yes, Cancel", color = if (isCancelling) DangerRed.copy(alpha = 0.5f) else DangerRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    enabled = !isCancelling
                ) {
                    Text("No, Keep it")
                }
            },
            containerColor = cs.surface,
            titleContentColor = cs.onSurface,
            textContentColor = cs.onSurfaceVariant
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = parsedDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface
                )

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Time", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${TimeUtils.formatTime12hr(booking.startTime)} - ${TimeUtils.formatTime12hr(booking.endTime)}", 
                        color = cs.onSurface, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Paid", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "₹${booking.amount.toInt()}", 
                        color = cs.primary, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            if (isCancelled && !booking.cancellationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DangerRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Cancellation Reason",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = booking.cancellationReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface
                        )
                    }
                }
            }
            
            if (isConfirmed) {
                Spacer(modifier = Modifier.height(16.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LTTButton(
                        text = "Receipt",
                        onClick = {
                            val uri = com.example.laketownturf.utils.PdfReceiptGenerator.generateAndGetUri(context, booking)
                            if (uri != null) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Open Receipt PDF"))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    FilledIconButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val formattedDate = try { java.time.LocalDate.parse(booking.date).format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")) } catch (e: Exception) { booking.date }
                                val formattedTime = "${com.example.laketownturf.utils.TimeUtils.formatTime12hr(booking.startTime)} - ${com.example.laketownturf.utils.TimeUtils.formatTime12hr(booking.endTime)}"
                                val message = "Hey! I've booked Lake Town Turf for $formattedDate at $formattedTime. \n\nView details here: https://laketownturf.netlify.app/booking/${booking.bookingId}"
                                putExtra(android.content.Intent.EXTRA_TEXT, message)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Booking"))
                        },
                        modifier = Modifier.size(50.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Send, contentDescription = "Share")
                    }
                    
                    if (canCancel) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(timeRemainingString, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else if (canCancel) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                ) {
                    Text("Cancel Booking ($timeRemainingString)", fontWeight = FontWeight.Bold)
                }
            }
            
            // Rebook / Book Again feature
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onBookAgain(booking) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, cs.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Book Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyBookingsState() {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = cs.primary.copy(alpha = 0.05f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = cs.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = cs.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "No Bookings Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onBackground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your upcoming and past turf games\nwill appear here once you book.",
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        )
    }
}
