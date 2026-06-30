package com.example.laketownturf.utils

import org.json.JSONObject

object ErrorMessageHelper {

    fun getFriendlyMessage(e: Throwable?): String {
        if (e == null) return "An unknown error occurred. Please try again."

        val message = e.message ?: ""

        // Handle common network exceptions
        if (message.contains("UnknownHostException") || 
            message.contains("ConnectException") || 
            message.contains("NoRouteToHostException")) {
            return "Please check your internet connection and try again."
        }
        
        if (message.contains("SocketTimeoutException") || message.contains("TimeoutException")) {
            return "The connection timed out. Please try again."
        }

        // Firebase Auth errors
        if (message.contains("FirebaseAuthInvalidCredentialsException")) {
            return "Invalid credentials provided. Please check and try again."
        }
        if (message.contains("FirebaseAuthUserCollisionException")) {
            return "An account already exists with this email address."
        }
        if (message.contains("FirebaseNetworkException")) {
            return "A network error occurred. Please check your connection."
        }
        if (message.contains("FirebaseAuthInvalidUserException")) {
            return "No account found with these details."
        }
        if (message.contains("PERMISSION_DENIED")) {
            return "You don't have permission to perform this action."
        }

        // If the message is a raw Razorpay JSON string (sometimes thrown as Exception by HTTP clients)
        if (message.startsWith("{") && message.contains("error")) {
            return parseRazorpayError(message)
        }

        // If it's a relatively short and clean message without stacktrace artifacts, return it
        if (message.length < 80 && !message.contains("Exception") && !message.contains("java.")) {
            return message
        }

        return "Something went wrong. Please try again later."
    }

    fun parseRazorpayError(rawError: String?): String {
        if (rawError.isNullOrBlank()) return "Payment failed or was cancelled."

        return try {
            val json = JSONObject(rawError)
            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                val description = errorObj.optString("description", "")
                val reason = errorObj.optString("reason", "")

                if (description.isNotBlank() && description.lowercase() != "payment error") {
                    description
                } else if (reason.isNotBlank()) {
                    // Convert snake_case reason to sentence
                    val formattedReason = reason.replace("_", " ")
                    formattedReason.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    "Payment was cancelled or failed."
                }
            } else {
                // If it's just a regular string like "Payment Cancelled by user"
                if (rawError.startsWith("{")) "Payment failed." else rawError
            }
        } catch (e: Exception) {
            // Not a valid JSON string
            if (rawError.contains("cancelled", ignoreCase = true)) {
                "Payment was cancelled."
            } else {
                "Payment failed. Please try again."
            }
        }
    }
}
