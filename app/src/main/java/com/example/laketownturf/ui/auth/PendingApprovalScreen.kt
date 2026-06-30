package com.example.laketownturf.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.laketownturf.theme.AmberCTA
import com.example.laketownturf.theme.DangerRed
import com.example.laketownturf.data.model.UserStatus
import com.example.laketownturf.ui.components.LTTDangerButton
import com.example.laketownturf.ui.components.LTTSecondaryButton

/**
 * Gate screen shown when a user's account status is "pending".
 * Blocks access to the main app until admin approves.
 */
@Composable
fun PendingApprovalScreen(
    status: String = UserStatus.PENDING,
    reason: String? = null,
    onRefreshStatus: () -> Unit,
    onLogout: () -> Unit,
    isLoading: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val isRevoked = status == UserStatus.REJECTED

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
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

    // Pulsing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Animated icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .background(
                        color = (if (isRevoked) DangerRed else AmberCTA).copy(alpha = 0.08f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = (if (isRevoked) DangerRed else AmberCTA).copy(alpha = 0.12f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isRevoked) Icons.Rounded.Block else Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (isRevoked) DangerRed else AmberCTA,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isRevoked) "Access Revoked" else "Account Pending Approval",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRevoked) DangerRed else cs.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isRevoked) "Access to your account has been disabled.\nPlease reach out to the society office to resolve this." 
                       else "We're currently verifying your details.\nYou'll receive a notification as soon as your account is approved.",
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )

            if (isRevoked && !reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DangerRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Reason for Revocation",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DangerRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Refresh button
            LTTSecondaryButton(
                text = if (isLoading) "Checking..." else "Refresh Status",
                onClick = onRefreshStatus,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Logout button
            LTTDangerButton(
                text = "Logout",
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
