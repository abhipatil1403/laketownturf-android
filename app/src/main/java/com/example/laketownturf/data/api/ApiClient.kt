package com.example.laketownturf.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val BASE_URL = "https://lake-town-turf-admin.netlify.app/.netlify/functions"

    suspend fun createRazorpayOrder(amount: Int): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/create-razorpay-order")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonParam = JSONObject()
            jsonParam.put("amount", amount)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonParam.toString())
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                return@withContext jsonObject.getString("id")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun verifyRazorpayPayment(orderId: String, paymentId: String, signature: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/verify-razorpay-payment")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonParam = JSONObject()
            jsonParam.put("razorpay_order_id", orderId)
            jsonParam.put("razorpay_payment_id", paymentId)
            jsonParam.put("razorpay_signature", signature)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonParam.toString())
                writer.flush()
            }

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun refundRazorpayPayment(paymentId: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/refund-razorpay-payment")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonParam = JSONObject()
            jsonParam.put("paymentId", paymentId)
            jsonParam.put("amount", amount)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonParam.toString())
                writer.flush()
            }

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun notifyWaitlist(slotId: String, date: String, startTime: String, endTime: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/notify-waitlist")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonParam = JSONObject()
            jsonParam.put("slotId", slotId)
            jsonParam.put("date", date)
            jsonParam.put("startTime", startTime)
            jsonParam.put("endTime", endTime)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonParam.toString())
                writer.flush()
            }

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
    suspend fun sendPushNotification(token: String, title: String, body: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/notify")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonParam = JSONObject()
            jsonParam.put("token", token)
            jsonParam.put("title", title)
            jsonParam.put("body", body)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonParam.toString())
                writer.flush()
            }

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}
