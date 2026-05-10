package eu.cyben.mobile.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import eu.cyben.mobile.CybenMobileApp
import eu.cyben.mobile.R
import eu.cyben.mobile.api.CybenApi
import eu.cyben.mobile.api.AppInfo
import eu.cyben.mobile.api.CheckAppsRequest
import eu.cyben.mobile.data.*
import eu.cyben.mobile.ui.DashboardActivity
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SecurityScanService : Service() {

    @Inject lateinit var api: CybenApi
    @Inject lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isScanning = false

    companion object {
        const val ACTION_START_SCAN = "eu.cyben.mobile.START_SCAN"
        const val ACTION_STOP_SCAN = "eu.cyben.mobile.STOP_SCAN"
        const val NOTIFICATION_ID = 1001

        // Known dangerous permissions
        val DANGEROUS_PERMISSIONS = listOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            "android.permission.READ_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.SYSTEM_ALERT_WINDOW"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> startScan()
            ACTION_STOP_SCAN -> stopScan()
        }
        return START_NOT_STICKY
    }

    private fun startScan() {
        if (isScanning) return
        isScanning = true

        startForeground(NOTIFICATION_ID, createScanningNotification())

        serviceScope.launch {
            try {
                val result = performFullScan()
                onScanComplete(result)
            } catch (e: Exception) {
                onScanError(e)
            } finally {
                isScanning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopScan() {
        isScanning = false
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun performFullScan(): ScanResult {
        val startTime = System.currentTimeMillis()
        val scanId = UUID.randomUUID().toString()
        val threats = mutableListOf<ThreatInfo>()

        // 1. Check for root
        if (isDeviceRooted()) {
            threats.add(
                ThreatInfo(
                    id = UUID.randomUUID().toString(),
                    type = ThreatType.ROOT_DETECTED,
                    severity = ThreatSeverity.HIGH,
                    title = "Root Detected",
                    description = "This device appears to be rooted, which can expose it to security risks",
                    packageName = null,
                    detectedAt = System.currentTimeMillis()
                )
            )
        }

        // 2. Scan installed apps
        val installedApps = getInstalledApps()
        updateNotificationProgress(0, installedApps.size)

        installedApps.forEachIndexed { index, app ->
            updateNotificationProgress(index + 1, installedApps.size)

            // Check for suspicious apps locally
            val localThreats = analyzeAppLocally(app)
            threats.addAll(localThreats)
        }

        // 3. Check apps against server database
        try {
            val appsToCheck = installedApps.filter { !it.isSystemApp }.map { app ->
                AppInfo(
                    packageName = app.packageName,
                    appName = app.appName,
                    versionCode = app.versionCode,
                    signature = app.signature
                )
            }

            if (appsToCheck.isNotEmpty()) {
                val response = api.checkApps(CheckAppsRequest(appsToCheck))
                if (response.isSuccessful) {
                    response.body()?.results?.filter { it.isMalware }?.forEach { result ->
                        threats.add(
                            ThreatInfo(
                                id = UUID.randomUUID().toString(),
                                type = ThreatType.valueOf(result.threatType ?: "MALWARE"),
                                severity = ThreatSeverity.valueOf(result.severity ?: "HIGH"),
                                title = "Malware Detected",
                                description = result.description ?: "This app has been identified as malicious",
                                packageName = result.packageName,
                                detectedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Continue with local scan results if server is unavailable
        }

        val endTime = System.currentTimeMillis()

        return ScanResult(
            scanId = scanId,
            startTime = startTime,
            endTime = endTime,
            appsScanned = installedApps.size,
            threatsFound = threats.size,
            threats = threats
        )
    }

    private fun getInstalledApps(): List<ScannedApp> {
        val pm = packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(
                PackageManager.GET_PERMISSIONS.toLong() or PackageManager.GET_SIGNATURES.toLong()
            ))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
        }

        return packages.map { packageInfo ->
            val appInfo = packageInfo.applicationInfo
            val isSystemApp = (appInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0

            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val dangerousPermissions = permissions.filter { it in DANGEROUS_PERMISSIONS }

            val signature = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.let {
                        MessageDigest.getInstance("SHA-256").digest(it.toByteArray())
                            .joinToString("") { byte -> "%02x".format(byte) }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures?.firstOrNull()?.let {
                        MessageDigest.getInstance("SHA-256").digest(it.toByteArray())
                            .joinToString("") { byte -> "%02x".format(byte) }
                    }
                }
            } catch (e: Exception) { null }

            ScannedApp(
                packageName = packageInfo.packageName,
                appName = appInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName,
                versionName = packageInfo.versionName ?: "",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                isSystemApp = isSystemApp,
                isSuspicious = false,
                permissions = permissions,
                dangerousPermissions = dangerousPermissions,
                signature = signature
            )
        }
    }

    private fun analyzeAppLocally(app: ScannedApp): List<ThreatInfo> {
        val threats = mutableListOf<ThreatInfo>()

        // Check for excessive dangerous permissions
        if (app.dangerousPermissions.size >= 5 && !app.isSystemApp) {
            threats.add(
                ThreatInfo(
                    id = UUID.randomUUID().toString(),
                    type = ThreatType.SUSPICIOUS_PERMISSION,
                    severity = ThreatSeverity.MEDIUM,
                    title = "Excessive Permissions",
                    description = "This app requests ${app.dangerousPermissions.size} sensitive permissions",
                    packageName = app.packageName,
                    detectedAt = System.currentTimeMillis()
                )
            )
        }

        // Check for accessibility service abuse (common in stalkerware)
        if (app.hasAccessibilityService && !app.isSystemApp) {
            threats.add(
                ThreatInfo(
                    id = UUID.randomUUID().toString(),
                    type = ThreatType.STALKERWARE,
                    severity = ThreatSeverity.HIGH,
                    title = "Accessibility Service Detected",
                    description = "This app uses accessibility services which can be used to monitor your activity",
                    packageName = app.packageName,
                    detectedAt = System.currentTimeMillis()
                )
            )
        }

        // Check for device admin (can prevent uninstall)
        if (app.hasDeviceAdmin && !app.isSystemApp) {
            threats.add(
                ThreatInfo(
                    id = UUID.randomUUID().toString(),
                    type = ThreatType.SUSPICIOUS_PERMISSION,
                    severity = ThreatSeverity.MEDIUM,
                    title = "Device Admin Detected",
                    description = "This app has device administrator privileges",
                    packageName = app.packageName,
                    detectedAt = System.currentTimeMillis()
                )
            )
        }

        return threats
    }

    private fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        return paths.any { java.io.File(it).exists() }
    }

    private fun createScanningNotification(): android.app.Notification {
        val intent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CybenMobileApp.CHANNEL_SCAN)
            .setContentTitle(getString(R.string.scanning))
            .setContentText(getString(R.string.scan_now))
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotificationProgress(current: Int, total: Int) {
        val notification = NotificationCompat.Builder(this, CybenMobileApp.CHANNEL_SCAN)
            .setContentTitle(getString(R.string.scanning))
            .setContentText("$current / $total apps")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setProgress(total, current, false)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun onScanComplete(result: ScanResult) {
        preferencesManager.setLastScanTime(result.endTime)
        preferencesManager.setLastScanThreats(result.threatsFound)

        // Show result notification
        val notification = NotificationCompat.Builder(this, CybenMobileApp.CHANNEL_SCAN)
            .setContentTitle(getString(R.string.scan_complete))
            .setContentText(
                if (result.threatsFound > 0) {
                    getString(R.string.threats_found, result.threatsFound)
                } else {
                    getString(R.string.no_threats)
                }
            )
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)

        // Broadcast result
        val intent = Intent("eu.cyben.mobile.SCAN_COMPLETE").apply {
            putExtra("threatsFound", result.threatsFound)
            putExtra("appsScanned", result.appsScanned)
        }
        sendBroadcast(intent)
    }

    private fun onScanError(error: Exception) {
        val notification = NotificationCompat.Builder(this, CybenMobileApp.CHANNEL_SCAN)
            .setContentTitle("Scan Error")
            .setContentText(error.message ?: "An error occurred during scan")
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
