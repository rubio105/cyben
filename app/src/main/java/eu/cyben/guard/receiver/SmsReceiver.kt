package eu.cyben.guard.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
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

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val BASE_URL = "https://cyben.eu"
        private const val CHANNEL_ID = "guard_sms_alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress ?: "Sconosciuto"
        val body = messages.joinToString("") { it.messageBody }
        if (body.isBlank()) return
        analyzeAsync(context, sender, body)
    }

    private fun getToken(context: Context): String? = try {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            context, "cyben_guard_secure", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.getString("guard_token", null)
    } catch (_: Exception) { null }

    private fun analyzeAsync(context: Context, sender: String, body: String) {
        val token = getToken(context) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/guard/analyze")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                val payload = JSONObject().apply {
                    put("type", "sms")
                    put("sender", sender)
                    put("content", body)
                }.toString()
                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val riskScore = json.optInt("riskScore", 0)
                    val reason = json.optString("reason", "Contenuto sospetto")
                    if (riskScore >= 40) showNotification(context, sender, riskScore, reason)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showNotification(context: Context, sender: String, score: Int, reason: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Avvisi SMS", NotificationManager.IMPORTANCE_HIGH))
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SMS Sospetto da $sender")
            .setContentText(reason)
            .setSubText("Rischio: $score%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).build()
        nm.notify(sender.hashCode(), n)
    }
}
