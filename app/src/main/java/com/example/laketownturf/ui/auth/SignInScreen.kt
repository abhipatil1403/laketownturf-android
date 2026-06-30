package com.example.laketownturf.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.laketownturf.ui.components.LTTButton

@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onSignInSuccess: (SignInResult) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    // Handle sign-in results
    LaunchedEffect(uiState.signInResult) {
        uiState.signInResult?.let { result ->
            onSignInSuccess(result)
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = cs.primary.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.laketownturf.R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(56.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Text
            Text(
                text = "Lake Town Turf",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = cs.primary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sign in to book your slots\nand manage your games.",
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )

            Spacer(modifier = Modifier.height(48.dp))

            androidx.compose.material3.Button(
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.surfaceVariant,
                    contentColor = cs.onSurface,
                ),
            ) {
                if (!uiState.isLoading) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.laketownturf.R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    CircularProgressIndicator(
                        color = cs.primary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = if (uiState.isLoading) "Signing in..." else "Continue with Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.weight(1.5f))
        }
    }
}
