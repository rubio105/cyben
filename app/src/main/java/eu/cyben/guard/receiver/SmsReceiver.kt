package eu.cyben.guard.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import eu.cyben.guard.R
import eu.cyben.guard.ui.dashboard.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIF_ID_PHISHING   = 1001
        const val NOTIF_ID_SUSPICIOUS = 1002
        const val CHANNEL_ID = "sms_alerts"

        private val URL_PATTERN = Pattern.compile(
            "(https?://|www\\.)[a-zA-Z0-9\\-._~:/?#\\[\\]@!\$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
        )
        private val PHISHING_KEYWORDS = listOf(
            "account sospeso", "account suspended",
            "clicca qui", "click here",
            "carta di credito", "credit card",
            "vincita", "hai vinto", "you won",
            "premio", "prize",
            "accedi subito", "login immediately",
            "verifica identità", "verify your identity",
            "bloccato", "suspended account"
        )
        private val SUSPICIOUS_URL_PATTERNS = listOf(
            "bit\\.ly", "tinyurl", "t\\.co", "goo\\.gl",
            "-login", "-secure", "-verify", "-update"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        Telephony.Sms.Intents.getMessagesFromIntent(intent)?.forEach { msg ->
            val sender = msg.originatingAddress ?: "Sconosciuto"
            val body = msg.messageBody ?: return@forEach
            analyzeSms(context, sender, body)
        }
    }

    private fun analyzeSms(context: Context, sender: String, body: String) {
        val urls = extractUrls(body)
        val localScore = calcLocalScore(body, urls)
        val knownContact = isKnownContact(context, sender)
        val token = getToken(context)

        // Known contacts need much higher score to trigger alert
        val phishingThreshold = if (knownContact) 0.85f else 0.6f
        val suspiciousThreshold = if (knownContact) 0.75f else 0.45f

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (token != null) {
                    val json = JSONObject().apply {
                        put("text", body); put("type", "sms"); put("sender", sender)
                        put("isKnownContact", knownContact)
                        put("preferredLanguage", java.util.Locale.getDefault().language)
                    }.toString()
                    val resp = OkHttpClient().newCall(
                        Request.Builder().url("https://cyben.eu/api/guard/analyze")
                            .post(json.toRequestBody("application/json".toMediaType()))
                            .addHeader("Authorization", "Bearer $token").build()
                    ).execute()
                    val respBody = JSONObject(resp.body?.string() ?: "")
                    val apiScore = (respBody.optJSONObject("analysis")?.optInt("riskScore", 0) ?: 0) / 100f
                    when {
                        apiScore >= phishingThreshold ->
                            showAlert(context, "phishing", sender, body, urls, apiScore, NOTIF_ID_PHISHING)
                        apiScore >= suspiciousThreshold ->
                            showAlert(context, "suspicious", sender, body, urls, apiScore, NOTIF_ID_SUSPICIOUS)
                    }
                } else if (localScore >= 0.7f) {
                    showAlert(context, "phishing", sender, body, urls, localScore, NOTIF_ID_PHISHING)
                } else if (localScore >= 0.55f) {
                    showAlert(context, "suspicious", sender, body, urls, localScore, NOTIF_ID_SUSPICIOUS)
                }
            } catch (_: Exception) {
                if (localScore >= 0.7f)
                    showAlert(context, "phishing", sender, body, urls, localScore, NOTIF_ID_PHISHING)
                else if (localScore >= 0.55f)
                    showAlert(context, "suspicious", sender, body, urls, localScore, NOTIF_ID_SUSPICIOUS)
            }
        }
    }

    private fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val m = URL_PATTERN.matcher(text)
        while (m.find()) urls.add(m.group())
        return urls
    }

    private fun calcLocalScore(body: String, urls: List<String>): Float {
        var score = 0f
        val lower = body.lowercase()
        score += (PHISHING_KEYWORDS.count { lower.contains(it) } * 0.1f).coerceAtMost(0.4f)
        urls.forEach { url ->
            SUSPICIOUS_URL_PATTERNS.forEach { p -> if (url.lowercase().contains(Regex(p))) score += 0.15f }
            if (url.matches(Regex(".*\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*"))) score += 0.4f
        }
        return score.coerceIn(0f, 1f)
    }

    private fun showAlert(context: Context, type: String, sender: String,
                          body: String, urls: List<String>, score: Float, notifId: Int) {
        saveSuspectSender(context, sender, type)
        ensureChannel(context)

        val activityIntent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("sms_sender", sender)
            putExtra("sms_body", body)
            putExtra("sms_score", score)
            putExtra("notification_id", notifId)
        }
        val pi = PendingIntent.getActivity(
            context, notifId, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPhishing = type == "phishing"
        val title = if (isPhishing) "⚠️ Phishing SMS rilevato" else "🔍 SMS Sospetto"
        val text  = "Da: $sender — Tocca per analizzare"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Da: $sender\nRischio: ${(score * 100).toInt()}%\n${if (urls.isNotEmpty()) "Link: ${urls.first()}" else ""}\nTocca per l'analisi completa."
            ))
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(if (isPhishing) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pi)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notifId, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Avvisi SMS", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    private fun saveSuspectSender(context: Context, sender: String, type: String) {
        try {
            val prefs = context.getSharedPreferences("suspect_senders", Context.MODE_PRIVATE)
            val existing = prefs.getString(sender, null)
            if (existing == null || type == "phishing") {
                prefs.edit().putString(sender, type).apply()
            }
        } catch (_: Exception) {}
    }

    private fun isKnownContact(context: Context, phone: String): Boolean = try {
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phone)
        )
        val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
        val found = (cursor?.count ?: 0) > 0
        cursor?.close()
        found
    } catch (_: Exception) { false }

    private fun getToken(context: Context): String? = try {
        val mk = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(context, "cyben_guard_secure", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).getString("guard_token", null)
    } catch (_: Exception) { null }
}
