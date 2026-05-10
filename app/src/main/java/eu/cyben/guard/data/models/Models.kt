package eu.cyben.guard.data.models

data class GuardAuthResponse(val token: String?, val user: GuardUser?, val error: String?, val ok: Boolean?, val needsVerification: Boolean?)

data class GuardUser(val id: Int, val name: String, val email: String, val plan: String, val subscriptionStatus: String?, val subscriptionInterval: String?) {
    val normalizedPlan: String get() = plan.trim().lowercase()
    val isPremium: Boolean get() = normalizedPlan == "premium"
    val isBasic: Boolean get() = normalizedPlan == "basic"
    val isPaid: Boolean get() = isPremium || isBasic
    val planLabel: String get() = when (normalizedPlan) { "basic" -> "Base"; "premium" -> "Premium"; else -> "Gratuito" }
}

data class GuardAnalysis(val id: Int, val inputText: String?, val inputType: String?, val riskLevel: String?, val riskScore: Int?, val explanation: String?, val recommendation: String?, val indicators: List<String>?, val createdAt: String?) {
    val riskLabel: String get() = when (riskLevel) { "safe" -> "Sicuro"; "suspicious" -> "Sospetto"; "dangerous" -> "Pericoloso"; else -> "Sconosciuto" }
}

data class AnalyzeResponse(val analysis: GuardAnalysis?, val conversationalMessage: String?, val dailyUsed: Int?, val dailyLimit: Int?, val error: String?)
data class GuardMonitoredEmail(val id: Int, val email: String, val label: String?, val lastChecked: String?, val breachCount: Int?)
data class GuardBreachAlert(val id: Int, val emailId: Int?, val breachName: String?, val breachDate: String?, val dataClasses: List<String>?, val description: String?, val isRead: Boolean?, val createdAt: String?)
data class BreachCheckResponse(val found: Boolean?, val breaches: List<BreachInfo>?, val error: String?)
data class BreachInfo(val name: String?, val domain: String?, val breachDate: String?, val dataClasses: List<String>?, val pwnCount: Int?)
data class SubscribeResponse(val url: String?, val error: String?)
data class SyncResponse(val plan: String?, val subscriptionStatus: String?, val error: String?)
data class BillingPortalResponse(val url: String?, val error: String?)
data class HumanRequestResponse(val id: Int?, val error: String?, val message: String?)
data class CriticalRequestResponse(val id: Int?, val error: String?, val message: String?)
data class VPNCredentials(val username: String, val password: String, val authMethod: String?)
data class VPNDnsStats(val active: Boolean, val blockedDomains: Int, val lastUpdated: String?, val dnsServer: String, val feedSource: String, val updateSchedule: String)
data class ErrorResponse(val error: String?, val message: String?) { val display: String get() = error ?: message ?: "Errore sconosciuto" }
data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())
