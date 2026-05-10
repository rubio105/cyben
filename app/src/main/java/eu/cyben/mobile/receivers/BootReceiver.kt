package eu.cyben.mobile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import eu.cyben.mobile.services.ProtectionService
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // Check if protection is enabled
            if (preferencesManager.isProtectionEnabled()) {
                startProtectionService(context)
            }
        }
    }

    private fun startProtectionService(context: Context) {
        val serviceIntent = Intent(context, ProtectionService::class.java).apply {
            action = ProtectionService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
