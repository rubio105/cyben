package eu.cyben.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Threat Severity Levels
enum class ThreatSeverity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

// Threat Types
enum class ThreatType {
    MALWARE,
    SPYWARE,
    PHISHING,
    SUSPICIOUS_PERMISSION,
    HIDDEN_APP,
    ROOT_DETECTED,
    INSECURE_WIFI,
    DATA_BREACH,
    STALKERWARE
}

// App Info with security analysis
data class ScannedApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val isSystemApp: Boolean,
    val isSuspicious: Boolean,
    val threatType: ThreatType? = null,
    val threatSeverity: ThreatSeverity? = null,
    val permissions: List<String>,
    val dangerousPermissions: List<String>,
    val hasAccessibilityService: Boolean = false,
    val hasDeviceAdmin: Boolean = false,
    val isHidden: Boolean = false,
    val signature: String? = null
)

// SMS Message for phishing detection
data class SmsMessage(
    val sender: String,
    val body: String,
    val timestamp: Long,
    val containsUrl: Boolean,
    val urls: List<String>,
    val isPhishing: Boolean = false,
    val phishingScore: Float = 0f
)

// WiFi Network Security Info
data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val securityType: String,
    val signalStrength: Int,
    val isSecure: Boolean,
    val hasWeakEncryption: Boolean,
    val isOpenNetwork: Boolean,
    val riskLevel: ThreatSeverity
)

// Data Breach Info
data class DataBreachInfo(
    val serviceName: String,
    val packageName: String?,
    val breachDate: String,
    val recordsAffected: Long,
    val dataTypes: List<String>,
    val description: String
)

// License Info
data class LicenseInfo(
    val key: String,
    val type: String, // "trial", "standard", "enterprise"
    val isActive: Boolean,
    val expiresAt: Date?,
    val deviceId: String,
    val companyId: String?,
    val features: List<String>
)

// Scan Result
data class ScanResult(
    val scanId: String,
    val startTime: Long,
    val endTime: Long,
    val appsScanned: Int,
    val threatsFound: Int,
    val threats: List<ThreatInfo>
)

// Threat Info
data class ThreatInfo(
    val id: String,
    val type: ThreatType,
    val severity: ThreatSeverity,
    val title: String,
    val description: String,
    val packageName: String?,
    val detectedAt: Long,
    val isResolved: Boolean = false
)

// Room Entities

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey val scanId: String,
    val startTime: Long,
    val endTime: Long,
    val appsScanned: Int,
    val threatsFound: Int,
    val threatDetails: String // JSON serialized
)

@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val description: String,
    val packageName: String?,
    val detectedAt: Long,
    val isResolved: Boolean,
    val resolvedAt: Long?
)

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val lockedAt: Long
)

@Entity(tableName = "blocked_sms")
data class BlockedSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val body: String,
    val urls: String, // JSON array
    val phishingScore: Float,
    val blockedAt: Long
)

@Entity(tableName = "breach_alerts")
data class BreachAlertEntity(
    @PrimaryKey val id: String,
    val serviceName: String,
    val packageName: String?,
    val breachDate: String,
    val recordsAffected: Long,
    val dataTypes: String, // JSON array
    val description: String,
    val alertedAt: Long,
    val isDismissed: Boolean
)
