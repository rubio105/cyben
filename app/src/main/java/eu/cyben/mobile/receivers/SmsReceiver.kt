package eu.cyben.mobile.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import eu.cyben.mobile.CybenMobileApp
import eu.cyben.mobile.R
import eu.cyben.mobile.api.CybenApi
import eu.cyben.mobile.api.CheckUrlsRequest
import eu.cyben.mobile.data.SmsMessage
import eu.cyben.mobile.ui.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var api: CybenApi

    companion object {
        // URL detection pattern
        private val URL_PATTERN = Pattern.compile(
            "(https?://|www\\.)[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
        )

        // Common phishing keywords
        private val PHISHING_KEYWORDS = listOf(
            "verifica", "verify", "account sospeso", "account suspended",
            "clicca qui", "click here", "urgente", "urgent",
            "conferma", "confirm", "password", "carta", "card",
            "banca", "bank", "pagamento", "payment", "vincita", "won",
            "premio", "prize", "spedizione", "delivery", "pacco", "package"
        )

        // Suspicious URL patterns
        private val SUSPICIOUS_URL_PATTERNS = listOf(
            "bit\\.ly", "tinyurl", "t\\.co", "goo\\.gl", // URL shorteners
            "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", // IP addresses
            "-login", "-secure", "-verify", "-update", // Common phishing subdomains
            "\\d{5,}", // Many numbers in domain
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        messages.forEach { smsMessage ->
            val sender = smsMessage.displayOriginatingAddress ?: "Unknown"
            val body = smsMessage.messageBody ?: return@forEach

            val urls = extractUrls(body)
            if (urls.isNotEmpty()) {
                analyzeSms(context, sender, body, urls)
            }
        }
    }

    private fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }

    private fun analyzeSms(context: Context, sender: String, body: String, urls: List<String>) {
        val localScore = calculateLocalPhishingScore(body, urls)

        if (localScore >= 0.3f) {
            // Check with server for accurate detection
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.checkUrls(CheckUrlsRequest(urls))
                    if (response.isSuccessful) {
                        val results = response.body()?.results ?: emptyList()
                        val isPhishing = results.any { it.isPhishing || it.isMalware }
                        
                        if (isPhishing) {
                            showPhishingAlert(context, sender, body, urls)
                        } else if (localScore >= 0.6f) {
                            // High local score but server didn't flag it - still warn
                            showSuspiciousAlert(context, sender, body)
                        }
                    } else if (localScore >= 0.6f) {
                        // Server unavailable but high local score
                        showPhishingAlert(context, sender, body, urls)
                    }
                } catch (e: Exception) {
                    // Server error - rely on local detection
                    if (localScore >= 0.6f) {
                        showPhishingAlert(context, sender, body, urls)
                    }
                }
            }
        }
    }

    private fun calculateLocalPhishingScore(body: String, urls: List<String>): Float {
        var score = 0f
        val lowerBody = body.lowercase()

        // Check for phishing keywords
        val keywordMatches = PHISHING_KEYWORDS.count { lowerBody.contains(it) }
        score += (keywordMatches * 0.1f).coerceAtMost(0.4f)

        // Check for suspicious URL patterns
        urls.forEach { url ->
            SUSPICIOUS_URL_PATTERNS.forEach { pattern ->
                if (url.lowercase().contains(Regex(pattern))) {
                    score += 0.2f
                }
            }
        }

        // URL shorteners are suspicious
        if (urls.any { it.contains("bit.ly") || it.contains("tinyurl") }) {
            score += 0.2f
        }

        // IP address URLs are highly suspicious
        if (urls.any { it.matches(Regex(".*\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*")) }) {
            score += 0.4f
        }

        // Urgency indicators
        if (lowerBody.contains("urgent") || lowerBody.contains("immediately") ||
            lowerBody.contains("subito") || lowerBody.contains("immediatamente")) {
            score += 0.1f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun showPhishingAlert(context: Context, sender: String, body: String, urls: List<String>) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alert_type", "phishing")
            putExtra("sender", sender)
            putExtra("body", body)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CybenMobileApp.CHANNEL_SECURITY)
            .setContentTitle(context.getString(R.string.alert_phishing))
            .setContentText(context.getString(R.string.alert_phishing_message, sender))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Suspicious link detected from $sender:\n${urls.firstOrNull()}\n\nDo not click this link!"))
            .setSmallIcon(R.drawable.ic_warning)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showSuspiciousAlert(context: Context, sender: String, body: String) {
        val notification = NotificationCompat.Builder(context, CybenMobileApp.CHANNEL_SECURITY)
            .setContentTitle("Suspicious SMS")
            .setContentText("A suspicious message was received from $sender")
            .setSmallIcon(R.drawable.ic_warning)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
