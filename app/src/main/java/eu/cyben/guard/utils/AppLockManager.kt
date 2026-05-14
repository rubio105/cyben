package eu.cyben.guard.utils

import android.content.Context

class AppLockManager(context: Context) {
    private val prefs = context.getSharedPreferences("applock", Context.MODE_PRIVATE)
    var isEnabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) { prefs.edit().putBoolean("enabled", v).apply() }
}
