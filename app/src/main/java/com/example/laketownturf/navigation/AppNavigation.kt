package com.example.laketownturf.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.laketownturf.data.model.UserStatus
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.UserRepository
import com.example.laketownturf.ui.auth.CompleteProfileScreen
import com.example.laketownturf.ui.auth.CompleteProfileViewModel
import com.example.laketownturf.ui.auth.PendingApprovalScreen
import com.example.laketownturf.ui.auth.SignInResult
import com.example.laketownturf.ui.auth.SignInScreen
import com.example.laketownturf.ui.auth.SignInViewModel
import com.example.laketownturf.ui.bookings.BookingsScreen
import com.example.laketownturf.ui.components.AppBottomBar
import com.example.laketownturf.ui.components.BottomNavItem
import com.example.laketownturf.ui.home.HomeScreen
import com.example.laketownturf.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

/** All navigation route constants. */
object Routes {
    const val SIGN_IN = "auth/signin"
    const val COMPLETE_PROFILE = "auth/complete_profile"
    const val PENDING = "auth/pending"
    const val MAIN = "main"
    const val HOME = "main/home"
    const val BOOKINGS = "main/bookings"
    const val PROFILE = "main/profile"
}

/**
 * Root-level navigation composable.
 * Determines the start destination based on auth state and user status.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository() }
    val userRepository = remember { UserRepository() }

    // Determine start destination based on auth state
    var startDestination by remember { mutableStateOf<String?>(null) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = authRepository.currentUser
        startDestination = if (currentUser == null) {
            Routes.SIGN_IN
        } else {
            // Check user status in Firestore
            val userResult = userRepository.getUser(currentUser.uid)
            val user = userResult.getOrNull()
            when {
                user == null -> Routes.COMPLETE_PROFILE
                user.status == UserStatus.ACTIVE -> Routes.HOME
                else -> Routes.PENDING
            }
        }
        isCheckingAuth = false
    }

    if (isCheckingAuth || startDestination == null) {
        // Show nothing (or splash) while checking auth
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {}
        return
    }

    // Global real-time observer to kick out revoked users or admit approved users instantly
    LaunchedEffect(authRepository.currentUser?.uid) {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            try {
                userRepository.observeUser(uid).collect { user ->
                    if (user != null) {
                        val currentRoute = navController.currentDestination?.route
                        if (user.status != UserStatus.ACTIVE) {
                            if (currentRoute == Routes.HOME || currentRoute == Routes.BOOKINGS || currentRoute == Routes.PROFILE) {
                                navController.navigate(Routes.PENDING) {
                                    popUpTo(navController.graph.id) { inclusive = false }
                                }
                            }
                        } else if (user.status == UserStatus.ACTIVE) {
                            if (currentRoute == Routes.PENDING) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(navController.graph.id) { inclusive = false }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors caused by logout permission denial
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
    ) {
        // ── Auth Screens ──────────────────────────────────────────

        composable(Routes.SIGN_IN) {
            val signInViewModel: SignInViewModel = viewModel()

            SignInScreen(
                viewModel = signInViewModel,
                onSignInSuccess = { result ->
                    when (result) {
                        is SignInResult.Active -> {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.id) { inclusive = false }
                            }
                        }
                        is SignInResult.Pending -> {
                            navController.navigate(Routes.PENDING) {
                                popUpTo(navController.graph.id) { inclusive = false }
                            }
                        }
                        is SignInResult.NoAccount -> {
                            navController.navigate(Routes.COMPLETE_PROFILE) {
                                popUpTo(Routes.SIGN_IN) { inclusive = true }
                            }
                        }
                    }
                },
            )
        }

        composable(Routes.COMPLETE_PROFILE) {
            val completeProfileViewModel: CompleteProfileViewModel = viewModel()

            CompleteProfileScreen(
                viewModel = completeProfileViewModel,
                onProfileComplete = {
                    navController.navigate(Routes.PENDING) {
                        popUpTo(navController.graph.id) { inclusive = false }
                    }
                },
            )
        }

        composable(Routes.PENDING) {
            val scope = rememberCoroutineScope()
            var isRefreshing by remember { mutableStateOf(false) }
            var isInitialLoad by remember { mutableStateOf(true) }
            var userStatus by remember { mutableStateOf(UserStatus.PENDING) }
            var userReason by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val uid = authRepository.currentUser?.uid
                if (uid != null) {
                    userRepository.observeUser(uid).collect { user ->
                        if (user != null) {
                            userStatus = user.status
                            userReason = user.revocationReason
                            isInitialLoad = false
                        }
                    }
                } else {
                    isInitialLoad = false
                }
            }

            if (isInitialLoad) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = com.example.laketownturf.theme.AmberCTA
                    )
                }
            } else {
                PendingApprovalScreen(
                status = userStatus,
                reason = userReason,
                isLoading = isRefreshing,
                onRefreshStatus = {
                    scope.launch {
                        isRefreshing = true
                        val uid = authRepository.currentUser?.uid
                        if (uid != null) {
                            val result = userRepository.getUser(uid)
                            val user = result.getOrNull()
                            if (user != null) {
                                userStatus = user.status
                                userReason = user.revocationReason
                            }
                            if (user?.status == UserStatus.ACTIVE) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(navController.graph.id) { inclusive = false }
                                }
                            }
                        }
                        isRefreshing = false
                    }
                },
                onLogout = {
                    authRepository.signOut()
                    navController.navigate(Routes.SIGN_IN) {
                        popUpTo(navController.graph.id) { inclusive = false }
                    }
                },
            )
            }
        }

        // ── Main App Screens (with Bottom Nav) ────────────────────

        composable(Routes.HOME) {
            MainScreenWithBottomNav(navController = navController, currentTab = BottomNavItem.HOME)
        }

        composable(
            Routes.BOOKINGS,
            deepLinks = listOf(navDeepLink { uriPattern = "laketownturf://bookings" })
        ) {
            MainScreenWithBottomNav(navController = navController, currentTab = BottomNavItem.BOOKINGS)
        }

        composable(
            Routes.PROFILE,
            deepLinks = listOf(navDeepLink { uriPattern = "laketownturf://profile" })
        ) {
            MainScreenWithBottomNav(navController = navController, currentTab = BottomNavItem.PROFILE)
        }
    }
}

@Composable
private fun MainScreenWithBottomNav(
    navController: NavHostController,
    currentTab: BottomNavItem,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                currentRoute = currentTab.route,
                onItemClick = { item ->
                    navController.navigate(item.route) {
                        // Pop up to the start destination to avoid building up a large stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        // Consume padding
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                BottomNavItem.HOME -> HomeScreen()
                BottomNavItem.BOOKINGS -> BookingsScreen(
                    onNavigateToReceipt = { /* unused but kept for interface match if any */ },
                    onBookAgain = { booking ->
                        com.example.laketownturf.utils.SharedBookingState.pendingRebookData = booking
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                BottomNavItem.PROFILE -> ProfileScreen(
                    onLogout = {
                        AuthRepository().signOut()
                        navController.navigate(Routes.SIGN_IN) {
                            popUpTo(navController.graph.id) { inclusive = false }
                        }
                    },
                )
            }
        }
    }
}
