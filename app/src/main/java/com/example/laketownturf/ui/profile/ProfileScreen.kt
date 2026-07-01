package com.example.laketownturf.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.laketownturf.data.model.UserType
import com.example.laketownturf.theme.AmberCTA
import com.example.laketownturf.theme.DangerRed
import com.example.laketownturf.ui.components.LTTButton
import com.example.laketownturf.ui.components.LTTDangerButton
import com.example.laketownturf.ui.components.ShimmerLoadingList
import com.example.laketownturf.utils.ThemePreference
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme
    val isDarkMode by ThemePreference.isDarkMode.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAnalytics by remember { mutableStateOf(false) }

    if (showAnalytics) {
        val stats = uiState.stats
        AlertDialog(
            onDismissRequest = { showAnalytics = false },
            title = { Text("Playing Analytics", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (stats.monthMatches > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("This Month", fontWeight = FontWeight.Bold, color = cs.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Matches: ${stats.monthMatches}", color = cs.onSurfaceVariant)
                                    Text("Spent: ₹${stats.monthSpent.toInt()}", color = cs.primary, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Top Day: ${stats.monthFavDay}", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Statistics", fontWeight = FontWeight.Bold, color = cs.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Matches", stats.totalMatches.toString(), Modifier.weight(1f))
                        StatCard("Hours", stats.totalHours.toString(), Modifier.weight(1f))
                        StatCard("Guests", stats.guestsInvited.toString(), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Top Day", stats.favDay, Modifier.weight(1f))
                        StatCard("Top Time", stats.favTime, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Achievements", fontWeight = FontWeight.Bold, color = cs.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val allBadges = listOf(
                            "First Booking" to "1",
                            "Regular Player" to "50",
                            "100 Match Club" to "100",
                            "Weekend Warrior" to "W",
                            "Early Bird" to "E",
                            "Night Owl" to "N",
                            "Team Captain" to "C"
                        )
                        items(allBadges.size) { index ->
                            val badge = allBadges[index]
                            val unlocked = stats.unlockedBadges.contains(badge.first)
                            BadgeCard(badge.first, badge.second, unlocked)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnalytics = false }) {
                    Text("Close")
                }
            },
            containerColor = cs.surface,
            titleContentColor = cs.onSurface,
            textContentColor = cs.onSurfaceVariant
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) {
                    Text("Yes, Logout", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = cs.onSurface)
                }
            },
            containerColor = cs.surface,
            titleContentColor = cs.onSurface,
            textContentColor = cs.onSurfaceVariant
        )
    }

    // Handle logout
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    // Show error
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (uiState.isLoading) {
            ShimmerLoadingList(count = 3, modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val user = uiState.user ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar + Name
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Avatar circle with subtle ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(cs.primary.copy(alpha = 0.5f), cs.primary.copy(alpha = 0.1f))
                            ),
                            shape = CircleShape,
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cs.surface, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.photoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = uiState.photoUrl,
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cs.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = cs.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User type badge (moved above name for less clustering)
                Box(
                    modifier = Modifier
                        .background(
                            color = if (user.type == UserType.SOCIETY)
                                cs.primary.copy(alpha = 0.15f) else AmberCTA.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (user.type == UserType.SOCIETY) "Society Member" else "Guest",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (user.type == UserType.SOCIETY) cs.primary else AmberCTA,
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isEditing) {
                    // Edit name inline
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextField(
                                value = uiState.editName,
                                onValueChange = viewModel::onEditNameChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = cs.onBackground,
                                    unfocusedTextColor = cs.onBackground,
                                    cursorColor = cs.primary,
                                    focusedIndicatorColor = cs.primary,
                                    unfocusedIndicatorColor = cs.outline,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LTTDangerButton(
                                    text = "Cancel",
                                    onClick = viewModel::cancelEditing,
                                    modifier = Modifier.weight(1f)
                                )
                                LTTButton(
                                    text = "Save",
                                    onClick = viewModel::saveName,
                                    isLoading = uiState.isSaving,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = cs.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = viewModel::startEditing,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit name",
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Gamification & Stats
            val stats = uiState.stats
            
            // Streak
            if (stats.currentStreak > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberCTA.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("${stats.currentStreak} Week Playing Streak", fontWeight = FontWeight.Bold, color = AmberCTA)
                            Text("Keep it up! Book a match this week.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Move to Analytics Section
            LTTButton(
                text = "View My Analytics",
                onClick = { showAnalytics = true },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Weather Widget
            if (uiState.weatherInfo != null) {
                val weather = uiState.weatherInfo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (weather.isGoodForPlay) cs.primaryContainer else cs.errorContainer),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (weather.isGoodForPlay) "☀️" else "🌧️",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("${weather.temperature}°C, ${weather.description}", fontWeight = FontWeight.Bold, color = if (weather.isGoodForPlay) cs.onPrimaryContainer else cs.onErrorContainer)
                            Text(if (weather.isGoodForPlay) "Perfect conditions for a match!" else "Heavy weather expected. Consider rescheduling.", style = MaterialTheme.typography.bodySmall, color = if (weather.isGoodForPlay) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onErrorContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            } else if (uiState.weatherError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = cs.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Weather unavailable right now.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
            } else {
                // Weather Loading State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = cs.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Fetching weather info...", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Favorites Section
            Text("Favorite Players", fontWeight = FontWeight.Bold, color = cs.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToFavorites() },
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(cs.onSurfaceVariant.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Favorites",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                        Text(
                            text = "${uiState.savedPlayers.size} Players Saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Manage",
                        tint = cs.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ProfileInfoRow(
                        icon = Icons.Outlined.Phone,
                        label = "Phone",
                        value = user.phone,
                    )
                    if (user.type == UserType.SOCIETY) {
                        ProfileInfoRow(
                            icon = Icons.Outlined.Apartment,
                            label = "Flat Number",
                            value = user.flatNo,
                        )
                    } else {
                        ProfileInfoRow(
                            icon = Icons.Outlined.Home,
                            label = "Address",
                            value = user.address.ifBlank { "Not provided" },
                        )
                    }

                    // Maintenance status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (user.maintenanceCleared)
                                Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = if (user.maintenanceCleared) cs.primary else DangerRed,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Maintenance",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                            Text(
                                text = if (user.maintenanceCleared) "Cleared" else "Pending",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = cs.onSurface,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (user.maintenanceCleared)
                                        cs.primary.copy(alpha = 0.1f) else DangerRed.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (user.maintenanceCleared) "Cleared" else "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.maintenanceCleared) cs.primary else DangerRed,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance Card — Dark/Light Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(cs.onSurfaceVariant.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        Text(
                            text = if (isDarkMode) "Dark Mode" else "Light Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurface,
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { ThemePreference.setDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = cs.primary,
                            checkedTrackColor = cs.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = cs.onSurfaceVariant,
                            uncheckedTrackColor = cs.outline,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Legal Links
            val context = LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lake-town-turf-admin.netlify.app/privacy"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lake-town-turf-admin.netlify.app/terms"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout
            LTTDangerButton(
                text = "Logout",
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(cs.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
fun BadgeCard(name: String, iconStr: String, unlocked: Boolean) {
    val cs = MaterialTheme.colorScheme
    val bgColor = if (unlocked) com.example.laketownturf.theme.AmberCTA.copy(alpha = 0.15f) else cs.onSurfaceVariant.copy(alpha = 0.1f)
    val contentColor = if (unlocked) com.example.laketownturf.theme.AmberCTA else cs.onSurfaceVariant.copy(alpha = 0.5f)
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Box(
            modifier = Modifier.size(60.dp).background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (unlocked) "🏆" else "🔒", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, color = contentColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
