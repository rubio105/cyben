package eu.cyben.guard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GuardCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        respondToCall(callDetails, CallResponse.Builder().build())
        val number = callDetails.handle?.schemeSpecificPart ?: return
        val token = getToken() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply { put("number", number) }.toString()
                val resp = OkHttpClient().newCall(
                    Request.Builder().url("https://cyben.eu/api/guard/check-phone")
                        .post(json.toRequestBody("application/json".toMediaType()))
                        .addHeader("Authorization", "Bearer $token").build()
                ).execute()
                val body = JSONObject(resp.body?.string() ?: return@launch)
                if (body.optBoolean("isScam", false) || body.optInt("score", 100) < 30)
                    showNotification("Chiamata sospetta", "Il numero $number potrebbe essere una truffa")
            } catch (_: Exception) {}
        }
    }

    private fun getToken(): String? = try {
        val mk = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(this, "cyben_guard_secure", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM).getString("guard_token", null)
    } catch (_: Exception) { null }

    private fun showNotification(title: String, message: String) {
        val channelId = "call_alerts"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel(channelId, "Avvisi Chiamate", NotificationManager.IMPORTANCE_HIGH))
        nm.notify(System.currentTimeMillis().toInt(), NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle(title)
            .setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build())
    }
}