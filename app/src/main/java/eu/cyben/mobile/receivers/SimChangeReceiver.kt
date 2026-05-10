package eu.cyben.mobile.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import eu.cyben.mobile.CybenMobileApp
import eu.cyben.mobile.R
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.SIM_STATE_CHANGED") return

        val simState = intent.getStringExtra("ss") ?: return

        when (simState) {
            "READY" -> {
                checkSimChange(context)
            }
            "ABSENT" -> {
                notifySimRemoved(context)
            }
        }
    }

    private fun checkSimChange(context: Context) {
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        
        @Suppress("DEPRECATION")
        val currentSimId = telephonyManager.simSerialNumber ?: return
        
        val savedSimId = preferencesManager.getSavedSimId()

        if (savedSimId != null && savedSimId != currentSimId) {
            // SIM has changed!
            notifySimChange(context, savedSimId, currentSimId)
        }

        // Save current SIM ID
        preferencesManager.setSavedSimId(currentSimId)
    }

    private fun notifySimChange(context: Context, oldSimId: String, newSimId: String) {
        val notification = NotificationCompat.Builder(context, CybenMobileApp.CHANNEL_SECURITY)
            .setContentTitle("SIM Card Changed")
            .setContentText("A different SIM card has been inserted in your device")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("The SIM card in your device has been changed. If this wasn't you, your device may have been stolen."))
            .setSmallIcon(R.drawable.ic_warning)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(5001, notification)

        // Report to server for anti-theft tracking
        // TODO: Implement server reporting
    }

    private fun notifySimRemoved(context: Context) {
        val notification = NotificationCompat.Builder(context, CybenMobileApp.CHANNEL_SECURITY)
            .setContentTitle("SIM Card Removed")
            .setContentText("The SIM card has been removed from your device")
            .setSmallIcon(R.drawable.ic_warning)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(5002, notification)
    }
}
