package com.example.laketownturf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.laketownturf.data.repository.AuthRepository
import com.example.laketownturf.data.repository.UserRepository
import com.example.laketownturf.navigation.AppNavigation
import com.example.laketownturf.theme.LakeTownTurfTheme
import com.example.laketownturf.utils.ThemePreference
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.example.laketownturf.utils.PaymentManager

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        askNotificationPermission()
        fetchFcmToken()

        enableEdgeToEdge()
        setContent {
            val isDark by ThemePreference.isDarkMode.collectAsState()

            LakeTownTurfTheme(isDarkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val link = intent?.extras?.getString("link")
        if (link != null) {
            intent.data = Uri.parse(link)
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permission already granted
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            val token = task.result
            val currentUser = AuthRepository().currentUser
            if (currentUser != null) {
                lifecycleScope.launch {
                    UserRepository().updateFcmToken(currentUser.uid, token)
                }
            }
        })
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        paymentData?.let { PaymentManager.handleSuccess(it) }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        PaymentManager.handleError(code, response)
    }
}
