package eu.cyben.mobile.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import eu.cyben.mobile.CybenMobileApp
import eu.cyben.mobile.R
import eu.cyben.mobile.api.CybenApi
import eu.cyben.mobile.api.HeartbeatRequest
import eu.cyben.mobile.data.ThreatSeverity
import eu.cyben.mobile.data.WifiNetworkInfo
import eu.cyben.mobile.ui.DashboardActivity
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ProtectionService : Service() {

    @Inject lateinit var api: CybenApi
    @Inject lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        const val ACTION_START = "eu.cyben.mobile.START_PROTECTION"
        const val ACTION_STOP = "eu.cyben.mobile.STOP_PROTECTION"
        const val NOTIFICATION_ID = 2001
        const val HEARTBEAT_INTERVAL = 15 * 60 * 1000L // 15 minutes
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProtection()
            ACTION_STOP -> stopProtection()
        }
        return START_STICKY
    }

    private fun startProtection() {
        startForeground(NOTIFICATION_ID, createProtectionNotification())
        registerNetworkCallback()
        startHeartbeat()
    }

    private fun stopProtection() {
        unregisterNetworkCallback()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createProtectionNotification(): android.app.Notification {
        val intent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CybenMobileApp.CHANNEL_SERVICE)
            .setContentTitle("Cyben Protection Active")
            .setContentText("Your device is protected")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)

        connectivityCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                checkWifiSecurity()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    checkWifiSecurity()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, connectivityCallback!!)
    }

    private fun unregisterNetworkCallback() {
        connectivityCallback?.let {
            val connectivityManager = getSystemService(ConnectivityManager::class.java)
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    private fun checkWifiSecurity() {
        val wifiManager = applicationContext.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo
        
        if (wifiInfo == null) return

        @Suppress("DEPRECATION")
        val ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: return

        // Get current network security type
        val networkInfo = analyzeCurrentNetwork(ssid)

        if (!networkInfo.isSecure) {
            showWifiAlert(networkInfo)
        }
    }

    private fun analyzeCurrentNetwork(ssid: String): WifiNetworkInfo {
        val wifiManager = applicationContext.getSystemService(WifiManager::class.java)

        @Suppress("DEPRECATION")
        val scanResults = wifiManager.scanResults
        val currentNetwork = scanResults.find { it.SSID == ssid }

        val capabilities = currentNetwork?.capabilities ?: ""
        val isOpenNetwork = !capabilities.contains("WPA") && 
                           !capabilities.contains("WEP") && 
                           !capabilities.contains("PSK")
        val hasWeakEncryption = capabilities.contains("WEP")
        val isSecure = !isOpenNetwork && !hasWeakEncryption

        val riskLevel = when {
            isOpenNetwork -> ThreatSeverity.HIGH
            hasWeakEncryption -> ThreatSeverity.MEDIUM
            else -> ThreatSeverity.LOW
        }

        return WifiNetworkInfo(
            ssid = ssid,
            bssid = currentNetwork?.BSSID ?: "",
            securityType = when {
                capabilities.contains("WPA3") -> "WPA3"
                capabilities.contains("WPA2") -> "WPA2"
                capabilities.contains("WPA") -> "WPA"
                capabilities.contains("WEP") -> "WEP"
                else -> "Open"
            },
            signalStrength = currentNetwork?.level ?: 0,
            isSecure = isSecure,
            hasWeakEncryption = hasWeakEncryption,
            isOpenNetwork = isOpenNetwork,
            riskLevel = riskLevel
        )
    }

    private fun showWifiAlert(networkInfo: WifiNetworkInfo) {
        val message = when {
            networkInfo.isOpenNetwork -> 
                "You are connected to an open WiFi network \"${networkInfo.ssid}\". Your data may be intercepted."
            networkInfo.hasWeakEncryption -> 
                "The WiFi network \"${networkInfo.ssid}\" uses weak WEP encryption."
            else -> return
        }

        val notification = NotificationCompat.Builder(this, CybenMobileApp.CHANNEL_SECURITY)
            .setContentTitle(getString(R.string.alert_wifi))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_wifi_warning)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 100, notification)
    }

    private fun startHeartbeat() {
        serviceScope.launch {
            while (isActive) {
                try {
                    sendHeartbeat()
                } catch (e: Exception) {
                    // Ignore heartbeat errors
                }
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    private suspend fun sendHeartbeat() {
        val deviceId = preferencesManager.getDeviceId() ?: return
        
        val request = HeartbeatRequest(
            deviceId = deviceId,
            protectionEnabled = true,
            lastScanTime = preferencesManager.getLastScanTime(),
            threatsActive = preferencesManager.getActiveThreatsCount(),
            batteryLevel = getBatteryLevel(),
            isRooted = isDeviceRooted()
        )

        val response = api.sendHeartbeat(request)
        if (response.isSuccessful) {
            response.body()?.commands?.forEach { command ->
                handleCommand(command)
            }
        }
    }

    private fun handleCommand(command: eu.cyben.mobile.api.DeviceCommand) {
        when (command.type) {
            "scan" -> {
                val intent = Intent(this, SecurityScanService::class.java).apply {
                    action = SecurityScanService.ACTION_START_SCAN
                }
                startService(intent)
            }
            "locate" -> {
                // Implement location sharing
            }
            "lock" -> {
                // Implement device lock
            }
            "wipe" -> {
                // Implement secure wipe (requires device admin)
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(android.os.BatteryManager::class.java)
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su"
        )
        return paths.any { java.io.File(it).exists() }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        serviceScope.cancel()
    }
}
