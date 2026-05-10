package eu.cyben.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CybenMobileApp : Application() {

    companion object {
        const val CHANNEL_SECURITY = "security_alerts"
        const val CHANNEL_SCAN = "scan_notifications"
        const val CHANNEL_SERVICE = "protection_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Security Alerts Channel (High Priority)
            val securityChannel = NotificationChannel(
                CHANNEL_SECURITY,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical security alerts and threat notifications"
                enableVibration(true)
                enableLights(true)
            }

            // Scan Notifications Channel
            val scanChannel = NotificationChannel(
                CHANNEL_SCAN,
                "Scan Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Scan progress and results"
            }

            // Protection Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background protection service status"
            }

            notificationManager.createNotificationChannels(
                listOf(securityChannel, scanChannel, serviceChannel)
            )
        }
    }
}
