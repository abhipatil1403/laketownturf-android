package com.example.laketownturf.utils

import android.app.Activity
import com.razorpay.Checkout
import com.razorpay.PaymentData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

sealed class PaymentResult {
    data class Success(val paymentData: PaymentData) : PaymentResult()
    data class Error(val code: Int, val description: String?) : PaymentResult()
}

object PaymentManager {
    private val _paymentResult = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
    val paymentResult = _paymentResult.asSharedFlow()

    fun startPayment(activity: Activity, orderId: String, amountInPaise: Int, userEmail: String, userPhone: String) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_T7XHXIGXW99Nl0") // Ideally this should be secure or fetched from backend, but Razorpay key_id is public.
        
        try {
            val options = JSONObject()
            options.put("name", "Lake Town Turf")
            options.put("description", "Turf Slot Booking")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg")
            options.put("order_id", orderId)
            options.put("currency", "INR")
            options.put("amount", amountInPaise.toString())
            options.put("prefill.email", userEmail)
            options.put("prefill.contact", userPhone)
            
            checkout.open(activity, options)
        } catch (e: Exception) {
            _paymentResult.tryEmit(PaymentResult.Error(-1, e.message))
        }
    }

    fun handleSuccess(paymentData: PaymentData) {
        _paymentResult.tryEmit(PaymentResult.Success(paymentData))
    }

    fun handleError(code: Int, description: String?) {
        _paymentResult.tryEmit(PaymentResult.Error(code, description))
    }
}
