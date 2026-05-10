package eu.cyben.mobile.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "cyben_mobile_prefs"
        private const val SECURE_PREFS_NAME = "cyben_secure_prefs"

        // Regular preferences keys
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_LAST_SCAN_THREATS = "last_scan_threats"
        private const val KEY_ACTIVE_THREATS = "active_threats_count"
        private const val KEY_AUTO_SCAN_ENABLED = "auto_scan_enabled"
        private const val KEY_SCAN_FREQUENCY_HOURS = "scan_frequency_hours"
        private const val KEY_SMS_PROTECTION_ENABLED = "sms_protection_enabled"
        private const val KEY_WIFI_PROTECTION_ENABLED = "wifi_protection_enabled"
        private const val KEY_SAVED_SIM_ID = "saved_sim_id"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"

        // Secure preferences keys
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_LICENSE_TYPE = "license_type"
        private const val KEY_APP_LOCK_PIN = "app_lock_pin"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Device ID
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    // Protection Status
    fun isProtectionEnabled(): Boolean = prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
    }

    // Scan History
    fun getLastScanTime(): Long? {
        val time = prefs.getLong(KEY_LAST_SCAN_TIME, -1)
        return if (time == -1L) null else time
    }

    fun setLastScanTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SCAN_TIME, time).apply()
    }

    fun getLastScanThreats(): Int = prefs.getInt(KEY_LAST_SCAN_THREATS, 0)
    fun setLastScanThreats(count: Int) {
        prefs.edit().putInt(KEY_LAST_SCAN_THREATS, count).apply()
    }

    // Active Threats
    fun getActiveThreatsCount(): Int = prefs.getInt(KEY_ACTIVE_THREATS, 0)
    fun setActiveThreatsCount(count: Int) {
        prefs.edit().putInt(KEY_ACTIVE_THREATS, count).apply()
    }

    // Auto Scan
    fun isAutoScanEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SCAN_ENABLED, true)
    fun setAutoScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCAN_ENABLED, enabled).apply()
    }

    fun getScanFrequencyHours(): Int = prefs.getInt(KEY_SCAN_FREQUENCY_HOURS, 24)
    fun setScanFrequencyHours(hours: Int) {
        prefs.edit().putInt(KEY_SCAN_FREQUENCY_HOURS, hours).apply()
    }

    // SMS Protection
    fun isSmsProtectionEnabled(): Boolean = prefs.getBoolean(KEY_SMS_PROTECTION_ENABLED, true)
    fun setSmsProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_PROTECTION_ENABLED, enabled).apply()
    }

    // WiFi Protection
    fun isWifiProtectionEnabled(): Boolean = prefs.getBoolean(KEY_WIFI_PROTECTION_ENABLED, true)
    fun setWifiProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_PROTECTION_ENABLED, enabled).apply()
    }

    // SIM Tracking
    fun getSavedSimId(): String? = prefs.getString(KEY_SAVED_SIM_ID, null)
    fun setSavedSimId(simId: String) {
        prefs.edit().putString(KEY_SAVED_SIM_ID, simId).apply()
    }

    // App Lock
    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun getAppLockPin(): String? = securePrefs.getString(KEY_APP_LOCK_PIN, null)
    fun setAppLockPin(pin: String) {
        securePrefs.edit().putString(KEY_APP_LOCK_PIN, pin).apply()
    }

    // License
    fun getLicenseKey(): String? = securePrefs.getString(KEY_LICENSE_KEY, null)
    fun setLicenseKey(key: String?) {
        if (key == null) {
            securePrefs.edit().remove(KEY_LICENSE_KEY).apply()
        } else {
            securePrefs.edit().putString(KEY_LICENSE_KEY, key).apply()
        }
    }

    fun getLicenseType(): String = prefs.getString(KEY_LICENSE_TYPE, "free") ?: "free"
    fun setLicenseType(type: String) {
        prefs.edit().putString(KEY_LICENSE_TYPE, type).apply()
    }

    // First Launch
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    // Clear all data
    fun clearAll() {
        prefs.edit().clear().apply()
        securePrefs.edit().clear().apply()
    }
}
