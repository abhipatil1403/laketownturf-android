package com.example.laketownturf.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    val stats = uiState.stats

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playing Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground,
                    navigationIconContentColor = cs.onBackground
                )
            )
        },
        containerColor = cs.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
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
                Spacer(modifier = Modifier.height(24.dp))
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

            Spacer(modifier = Modifier.height(32.dp))

            Text("Achievements", fontWeight = FontWeight.Bold, color = cs.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
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
            Spacer(modifier = Modifier.height(32.dp))
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
