package eu.cyben.guard.data.api

import eu.cyben.guard.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("/api/guard/auth/register") suspend fun register(@Body body: RegisterRequest): Response<GuardAuthResponse>
    @POST("/api/guard/auth/login") suspend fun login(@Body body: LoginRequest): Response<GuardAuthResponse>
    @GET("/api/guard/auth/me") suspend fun getMe(): Response<GuardUser>
    @POST("/api/guard/auth/resend-verification") suspend fun resendVerification(@Body body: EmailRequest): Response<OkResponse>
    @POST("/api/guard/auth/forgot-password") suspend fun forgotPassword(@Body body: EmailRequest): Response<MessageResponse>
    @POST("/api/guard/analyze") suspend fun analyze(@Body body: AnalyzeRequest): Response<AnalyzeResponse>
    @GET("/api/guard/analyses") suspend fun getAnalyses(): Response<List<GuardAnalysis>>
    @GET("/api/guard/monitored-emails") suspend fun getMonitoredEmails(): Response<List<GuardMonitoredEmail>>
    @POST("/api/guard/monitored-emails") suspend fun addMonitoredEmail(@Body body: AddEmailRequest): Response<GuardMonitoredEmail>
    @DELETE("/api/guard/monitored-emails/{id}") suspend fun deleteMonitoredEmail(@Path("id") id: Int): Response<Unit>
    @GET("/api/guard/breach-alerts") suspend fun getBreachAlerts(): Response<List<GuardBreachAlert>>
    @POST("/api/guard/breach-check/{emailId}") suspend fun checkBreach(@Path("emailId") emailId: Int): Response<BreachCheckResponse>
    @POST("/api/guard/subscribe") suspend fun subscribe(@Body body: SubscribeRequest): Response<SubscribeResponse>
    @POST("/api/guard/sync-subscription") suspend fun syncSubscription(): Response<SyncResponse>
    @POST("/api/guard/billing-portal") suspend fun getBillingPortal(): Response<BillingPortalResponse>
    @POST("/api/guard/human-request") suspend fun requestHuman(@Body body: HumanRequest): Response<HumanRequestResponse>
    @POST("/api/guard/critical-requests") suspend fun sendSOS(@Body body: SOSRequest): Response<CriticalRequestResponse>
    @GET("/api/guard/vpn/credentials") suspend fun getVPNCredentials(): Response<VPNCredentials>
    @GET("/api/guard/vpn/dns-stats") suspend fun getVPNDnsStats(): Response<VPNDnsStats>
}

data class RegisterRequest(val name: String, val email: String, val password: String, val termsAccepted: Boolean, val privacyAccepted: Boolean, val marketingConsent: Boolean, val preferredLanguage: String = "it")
data class LoginRequest(val email: String, val password: String)
data class EmailRequest(val email: String)
data class OkResponse(val ok: Boolean?)
data class MessageResponse(val message: String?)
data class AnalyzeRequest(val text: String, val type: String, val chatHistory: List<Map<String, String>> = emptyList(), val preferredLanguage: String = "it")
data class AddEmailRequest(val email: String, val label: String?)
data class SubscribeRequest(val plan: String, val billingPeriod: String?)
data class HumanRequest(val text: String, val analysisId: Int?)
data class SOSRequest(val description: String, val incidentType: String = "other")
