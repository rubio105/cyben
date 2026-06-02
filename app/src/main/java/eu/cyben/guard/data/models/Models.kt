package eu.cyben.guard.data.models

data class GuardAuthResponse(val token: String?, val user: GuardUser?, val error: String?, val ok: Boolean?, val needsVerification: Boolean?)

data class GuardUser(
    val id: Int, val name: String, val email: String,
    val plan: String, val subscriptionStatus: String?, val subscriptionInterval: String?,
    val isVoiceEnabled: Boolean? = false
) {
    val normalizedPlan: String get() = plan.trim().lowercase()
    val isPremium: Boolean get() = normalizedPlan == "premium"
    val isPremiumMonthly: Boolean get() = isPremium && (subscriptionInterval == "month" || subscriptionInterval == "monthly")
    val isPremiumAnnual: Boolean get() = isPremium && (subscriptionInterval == "year" || subscriptionInterval == "annual" || subscriptionInterval == "yearly")
    val isProhmedEnabled: Boolean get() = isPremiumAnnual
    val hasActiveSubscription: Boolean get() = isPremium && (subscriptionStatus == "active" || subscriptionStatus == "trialing")
    val planLabel: String get() = when {
        isPremiumAnnual -> "Premium Annuale"
        isPremiumMonthly -> "Premium Mensile"
        else -> "Gratuito"
    }
}

data class GuardAnalysis(
    val id: Int = 0,
    val inputText: String? = null, val input_text: String? = null,
    val inputType: String? = null, val input_type: String? = null,
    val riskLevel: String? = null, val risk_level: String? = null,
    val riskScore: Int? = null, val risk_score: Int? = null,
    val explanation: String? = null,
    val recommendation: String? = null,
    val indicators: List<String>? = null,
    val createdAt: String? = null, val created_at: String? = null
) {
    val resolvedRiskLevel: String get() = riskLevel ?: risk_level ?: "unknown"
    val resolvedRiskScore: Int? get() = riskScore ?: risk_score
    val riskLabel: String get() = when (resolvedRiskLevel) {
        "safe" -> "Sicuro"; "suspicious" -> "Sospetto"; "dangerous" -> "Pericoloso"; else -> "Sconosciuto"
    }
}

data class AnalyzeResponse(
    val analysis: GuardAnalysis? = null,
    val conversationalMessage: String? = null, val conversational_message: String? = null,
    val dailyUsed: Int? = null, val daily_used: Int? = null,
    val dailyLimit: Int? = null, val daily_limit: Int? = null,
    val error: String? = null
) {
    val displayMessage: String? get() = conversationalMessage ?: conversational_message
    val resolvedDailyUsed: Int? get() = dailyUsed ?: daily_used
    val resolvedDailyLimit: Int? get() = dailyLimit ?: daily_limit
}

data class LiveAnalyzeResponse(
    val rischio_attuale: String? = null,
    val motivo_breve: String? = null,
    val domande_subito: List<String>? = null,
    val non_condividere: List<String>? = null,
    val messaggio_live: String? = null
)

data class GuardMonitoredEmail(val id: Int, val email: String, val label: String?, val lastChecked: String?, val breachCount: Int?)
data class GuardBreachAlert(val id: Int, val emailId: Int?, val breachName: String?, val breachDate: String?, val dataClasses: List<String>?, val description: String?, val isRead: Boolean?, val createdAt: String?)
data class BreachCheckResponse(val found: Boolean?, val breaches: List<BreachInfo>?, val error: String?)
data class BreachInfo(val name: String?, val domain: String?, val breachDate: String?, val dataClasses: List<String>?, val pwnCount: Int?)
data class SubscribeResponse(val url: String?, val error: String?)
data class SyncResponse(val plan: String?, val subscriptionStatus: String?, val subscriptionInterval: String?, val error: String?)
data class BillingPortalResponse(val url: String?, val error: String?)
data class HumanRequestResponse(val id: Int?, val error: String?, val message: String?)
data class CriticalRequestResponse(val id: Int?, val error: String?, val message: String?)
data class VPNCredentials(val username: String, val password: String, val authMethod: String?)
data class VPNDnsStats(val active: Boolean, val blockedDomains: Int, val lastUpdated: String?, val dnsServer: String, val feedSource: String, val updateSchedule: String)
data class ErrorResponse(val error: String?, val message: String?) { val display: String get() = error ?: message ?: "Errore sconosciuto" }
data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis(), val analysis: GuardAnalysis? = null)

data class ProhmedStatus(val activated: Boolean, val activatedAt: String?, val name: String?, val fiscalCode: String?, val email: String?, val residui: Int?)
data class ProhmedActivateRequest(val name: String, val fiscalCode: String, val birthDate: String, val phone: String)
data class ProhmedActivateResponse(val ok: Boolean?, val error: String?)
data class ProhmedConsultRequest(val specialty: String, val description: String, val urgency: String)
data class ProhmedConsultResponse(val id: Int?, val error: String?)
data class ProhmedConsult(val id: Int, val specialty: String, val description: String?, val urgency: String?, val status: String?, val createdAt: String?)

data class ImageAnalyzeRequest(val imageBase64: String, val mimeType: String = "image/jpeg")
data class PhoneCheckResponse(val isScam: Boolean?, val score: Int?, val explanation: String?, val error: String?)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
data class HibpCheckResponse(val found: Boolean?, val count: Int?, val error: String?)

// Protection feed
data class ProtectionFeed(
    val blockedPhones: List<String>?,
    val suspiciousPhones: List<String>?,
    val trustedPhones: List<String>?,
    val suspiciousDomains: List<String>?,
    val blockedPhrases: List<String>?,
    val suspiciousPhrases: List<String>?,
    val lastUpdatedAt: String?
)

// Network scan
data class NetworkScanRequest(
    val platform: String = "android",
    val appVersion: String? = null,
    val deviceTime: String? = null,
    val scanType: String = "network_anomaly",
    val signals: Map<String, String> = emptyMap()
)
data class NetworkScanResponse(
    val status: String?,
    val score: Int?,
    val summary: String?,
    val notifyUser: Boolean?,
    val notificationText: String?,
    val generatedAt: String?,
    val scanId: Int?
)
data class ScanHistoryItem(
    val id: String?,
    val type: String?,
    val status: String?,
    val score: Int?,
    val summary: String?,
    val acknowledged: Boolean?,
    val generatedAt: String?
)
data class ScanHistoryResponse(val items: List<ScanHistoryItem>?)
data class ScanAcknowledgeRequest(val scanId: String)

// Phone report
data class ReportPhoneRequest(val phone: String, val category: String? = null, val description: String? = null)
data class ReportPhoneResponse(val communityReports: Int?)

// Profile updates
data class UpdateEmailRequest(val email: String)
data class UpdateEmailResponse(val ok: Boolean?, val email: String?)
data class UpdatePreferencesRequest(
    val preferredLanguage: String? = null,
    val name: String? = null,
    val appLockEnabled: Boolean? = null,
    val biometricUnlockEnabled: Boolean? = null
)

// Reset password
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)
