package eu.cyben.guard.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
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

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        Telephony.Sms.Intents.getMessagesFromIntent(intent)?.forEach { msg ->
            analyzeSms(context, msg.originatingAddress ?: "Sconosciuto", msg.messageBody ?: return@forEach)
        }
    }

    private fun analyzeSms(context: Context, sender: String, content: String) {
        val token = getToken(context) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("text", content); put("type", "sms"); put("sender", sender)
                    put("preferredLanguage", java.util.Locale.getDefault().language)
                }.toString()
                val resp = OkHttpClient().newCall(
                    Request.Builder().url("https://cyben.eu/api/guard/analyze")
                        .post(json.toRequestBody("application/json".toMediaType()))
                        .addHeader("Authorization", "Bearer $token").build()
                ).execute()
                val body = JSONObject(resp.body?.string() ?: return@launch)
                val score = body.optJSONObject("analysis")?.optInt("riskScore", 0) ?: 0
                if (score >= 40) showNotification(context, sender, "SMS sospetto ricevuto da $sender")
            } catch (_: Exception) {}
        }
    }

    private fun getToken(context: Context): String? = try {
        val mk = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(context, "cyben_guard_secure", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM).getString("guard_token", null)
    } catch (_: Exception) { null }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "sms_alerts"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel(channelId, "Avvisi SMS", NotificationManager.IMPORTANCE_HIGH))
        nm.notify(System.currentTimeMillis().toInt(), NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle(title)
            .setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build())
    }
}