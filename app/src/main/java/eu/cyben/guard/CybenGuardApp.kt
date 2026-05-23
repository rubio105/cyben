package eu.cyben.guard

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CybenGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.Debug.logLevel = LogLevel.NONE
        OneSignal.initWithContext(this, "31270916-7dbe-4d64-817f-cbb7aa808060")
    }
}
