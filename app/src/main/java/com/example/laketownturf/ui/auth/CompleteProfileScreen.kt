package com.example.laketownturf.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.laketownturf.data.model.UserType
import com.example.laketownturf.ui.components.LTTButton
import com.example.laketownturf.ui.components.LTTTextField

@Composable
fun CompleteProfileScreen(
    viewModel: CompleteProfileViewModel,
    onProfileComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme

    // Handle completion
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onProfileComplete()
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Hi ${uiState.name.split(" ").firstOrNull() ?: "there"}!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onBackground,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Complete your profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please provide a few more details so we can get you set up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Form
            LTTTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = "Phone Number",
                placeholder = "98765 43210",
                keyboardType = KeyboardType.Phone,
                error = uiState.phoneError,
                prefix = {
                    Text(
                        text = "+91 ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Are you a society resident?",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                // Resident Card
                androidx.compose.material3.Surface(
                    selected = uiState.type == UserType.SOCIETY,
                    onClick = { viewModel.onTypeChange(UserType.SOCIETY) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (uiState.type == UserType.SOCIETY) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.5f),
                    border = if (uiState.type == UserType.SOCIETY) androidx.compose.foundation.BorderStroke(1.dp, cs.primary) else null,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Yes, Resident",
                            color = if (uiState.type == UserType.SOCIETY) cs.onPrimaryContainer else cs.onSurfaceVariant,
                            fontWeight = if (uiState.type == UserType.SOCIETY) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Outsider Card
                androidx.compose.material3.Surface(
                    selected = uiState.type == UserType.OUTSIDER,
                    onClick = { viewModel.onTypeChange(UserType.OUTSIDER) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (uiState.type == UserType.OUTSIDER) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.5f),
                    border = if (uiState.type == UserType.OUTSIDER) androidx.compose.foundation.BorderStroke(1.dp, cs.primary) else null,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No, Outsider",
                            color = if (uiState.type == UserType.OUTSIDER) cs.onPrimaryContainer else cs.onSurfaceVariant,
                            fontWeight = if (uiState.type == UserType.OUTSIDER) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = uiState.type == UserType.SOCIETY,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        LTTTextField(
                            value = uiState.blockNo,
                            onValueChange = viewModel::onBlockNoChange,
                            label = "Block",
                            placeholder = "e.g. D1",
                            keyboardCapitalization = KeyboardCapitalization.Characters,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        LTTTextField(
                            value = uiState.flatNo,
                            onValueChange = viewModel::onFlatNoChange,
                            label = "Flat",
                            placeholder = "e.g. 101",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            AnimatedVisibility(
                visible = uiState.type == UserType.OUTSIDER,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    LTTTextField(
                        value = uiState.address,
                        onValueChange = viewModel::onAddressChange,
                        label = "Full Address",
                        placeholder = "Enter your complete residential address",
                        keyboardCapitalization = KeyboardCapitalization.Words,
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Terms Agreement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.isAgreedToTerms,
                    onCheckedChange = { viewModel.onTermsAgreementChange(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = cs.primary,
                        uncheckedColor = cs.outline
                    )
                )
                val annotatedString = buildAnnotatedString {
                    append("I agree to the ")
                    
                    pushLink(LinkAnnotation.Url("https://lake-town-turf-admin.netlify.app/terms"))
                    withStyle(style = SpanStyle(color = cs.primary, fontWeight = FontWeight.Bold)) {
                        append("Terms of Service")
                    }
                    pop()
                    
                    append(" and ")
                    
                    pushLink(LinkAnnotation.Url("https://lake-town-turf-admin.netlify.app/privacy"))
                    withStyle(style = SpanStyle(color = cs.primary, fontWeight = FontWeight.Bold)) {
                        append("Privacy Policy")
                    }
                    pop()
                    
                    append(".")
                }
                
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            // Submit Button
            LTTButton(
                text = "Submit for Verification",
                onClick = viewModel::submitProfile,
                isLoading = uiState.isLoading,
                enabled = uiState.phone.length == 10 && 
                          uiState.isAgreedToTerms &&
                          ((uiState.type == UserType.OUTSIDER && uiState.address.isNotBlank()) || 
                           (uiState.type == UserType.SOCIETY && uiState.flatNo.isNotBlank() && uiState.blockNo.isNotBlank())),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
