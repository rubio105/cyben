package eu.cyben.guard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import eu.cyben.guard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GuardCallScreeningService : CallScreeningService() {
    companion object {
        private const val BASE_URL = "https://cyben.eu"
        private const val CHANNEL_ID = "guard_call_alerts"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        respondToCall(callDetails, CallResponse.Builder()
            .setDisallowCall(false).setRejectCall(false)
            .setSilenceCall(false).setSkipCallLog(false)
            .setSkipNotification(false).build())
        val number = callDetails.handle?.schemeSpecificPart
        if (!number.isNullOrBlank()) checkNumberAsync(number)
    }

    private fun getToken(): String? = try {
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            this, "cyben_guard_secure", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.getString("guard_token", null)
    } catch (_: Exception) { null }

    private fun checkNumberAsync(number: String) {
        val token = getToken() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/guard/check-phone")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                val payload = JSONObject().apply { put("number", number) }.toString()
                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val isScam = json.optBoolean("isScam", false)
                    val score = json.optInt("score", 100)
                    val reason = json.optString("reason", "Numero sospetto")
                    if (isScam || score < 30) showNotification(number, reason, score)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showNotification(number: String, reason: String, score: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Avvisi Chiamate", NotificationManager.IMPORTANCE_HIGH))
        }
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Chiamata Sospetta")
            .setContentText("$number - $reason")
            .setSubText("Affidabilita: $score%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).build()
        nm.notify(number.hashCode(), n)
    }
}
