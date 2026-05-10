package eu.cyben.mobile.api

import retrofit2.Response
import retrofit2.http.*

interface CybenApi {

    // Authentication & License
    @POST("api/mobile/auth/activate")
    suspend fun activateLicense(
        @Body request: ActivateLicenseRequest
    ): Response<ActivateLicenseResponse>

    @POST("api/mobile/auth/verify")
    suspend fun verifyLicense(
        @Body request: VerifyLicenseRequest
    ): Response<VerifyLicenseResponse>

    @POST("api/mobile/auth/deactivate")
    suspend fun deactivateLicense(
        @Body request: DeactivateLicenseRequest
    ): Response<BaseResponse>

    // Malware Detection
    @POST("api/mobile/scan/check-apps")
    suspend fun checkApps(
        @Body request: CheckAppsRequest
    ): Response<CheckAppsResponse>

    @GET("api/mobile/scan/malware-signatures")
    suspend fun getMalwareSignatures(
        @Query("lastUpdate") lastUpdate: Long? = null
    ): Response<MalwareSignaturesResponse>

    // URL/Phishing Check
    @POST("api/mobile/scan/check-urls")
    suspend fun checkUrls(
        @Body request: CheckUrlsRequest
    ): Response<CheckUrlsResponse>

    // Data Breach Check
    @POST("api/mobile/breach/check-apps")
    suspend fun checkBreaches(
        @Body request: CheckBreachesRequest
    ): Response<CheckBreachesResponse>

    // Report Scan Results (for company dashboard)
    @POST("api/mobile/report/scan")
    suspend fun reportScan(
        @Body request: ReportScanRequest
    ): Response<BaseResponse>

    @POST("api/mobile/report/threat")
    suspend fun reportThreat(
        @Body request: ReportThreatRequest
    ): Response<BaseResponse>

    // Device Management
    @POST("api/mobile/device/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<RegisterDeviceResponse>

    @POST("api/mobile/device/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: HeartbeatRequest
    ): Response<HeartbeatResponse>

    @GET("api/mobile/device/commands")
    suspend fun getCommands(
        @Query("deviceId") deviceId: String
    ): Response<CommandsResponse>

    @POST("api/mobile/device/command-response")
    suspend fun sendCommandResponse(
        @Body request: CommandResponseRequest
    ): Response<BaseResponse>
}

// Request/Response Models

data class ActivateLicenseRequest(
    val licenseKey: String,
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String
)

data class ActivateLicenseResponse(
    val success: Boolean,
    val message: String?,
    val license: LicenseData?,
    val companyId: String?,
    val companyName: String?
)

data class LicenseData(
    val type: String,
    val expiresAt: String?,
    val features: List<String>
)

data class VerifyLicenseRequest(
    val licenseKey: String,
    val deviceId: String
)

data class VerifyLicenseResponse(
    val isValid: Boolean,
    val license: LicenseData?,
    val message: String?
)

data class DeactivateLicenseRequest(
    val licenseKey: String,
    val deviceId: String
)

data class CheckAppsRequest(
    val apps: List<AppInfo>
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val signature: String?
)

data class CheckAppsResponse(
    val results: List<AppCheckResult>
)

data class AppCheckResult(
    val packageName: String,
    val isMalware: Boolean,
    val threatType: String?,
    val severity: String?,
    val description: String?
)

data class MalwareSignaturesResponse(
    val lastUpdate: Long,
    val signatures: List<MalwareSignature>
)

data class MalwareSignature(
    val packageName: String,
    val signatureHash: String?,
    val threatType: String,
    val severity: String,
    val description: String
)

data class CheckUrlsRequest(
    val urls: List<String>
)

data class CheckUrlsResponse(
    val results: List<UrlCheckResult>
)

data class UrlCheckResult(
    val url: String,
    val isPhishing: Boolean,
    val isMalware: Boolean,
    val threatType: String?,
    val riskScore: Float
)

data class CheckBreachesRequest(
    val apps: List<String> // package names or service names
)

data class CheckBreachesResponse(
    val breaches: List<BreachData>
)

data class BreachData(
    val serviceName: String,
    val breachDate: String,
    val recordsAffected: Long,
    val dataTypes: List<String>,
    val description: String
)

data class ReportScanRequest(
    val deviceId: String,
    val scanId: String,
    val scanTime: Long,
    val appsScanned: Int,
    val threatsFound: Int,
    val threats: List<ThreatData>
)

data class ThreatData(
    val type: String,
    val severity: String,
    val packageName: String?,
    val description: String
)

data class ReportThreatRequest(
    val deviceId: String,
    val threatId: String,
    val type: String,
    val severity: String,
    val packageName: String?,
    val description: String,
    val detectedAt: Long
)

data class RegisterDeviceRequest(
    val deviceId: String,
    val licenseKey: String,
    val deviceName: String,
    val deviceModel: String,
    val manufacturer: String,
    val osVersion: String,
    val appVersion: String,
    val fcmToken: String?
)

data class RegisterDeviceResponse(
    val success: Boolean,
    val deviceId: String,
    val message: String?
)

data class HeartbeatRequest(
    val deviceId: String,
    val protectionEnabled: Boolean,
    val lastScanTime: Long?,
    val threatsActive: Int,
    val batteryLevel: Int,
    val isRooted: Boolean
)

data class HeartbeatResponse(
    val success: Boolean,
    val commands: List<DeviceCommand>?
)

data class CommandsResponse(
    val commands: List<DeviceCommand>
)

data class DeviceCommand(
    val commandId: String,
    val type: String, // "locate", "lock", "wipe", "scan", "update"
    val parameters: Map<String, String>?
)

data class CommandResponseRequest(
    val deviceId: String,
    val commandId: String,
    val status: String, // "completed", "failed"
    val result: String?
)

data class BaseResponse(
    val success: Boolean,
    val message: String?
)
