package eu.cyben.mobile.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import eu.cyben.mobile.databinding.ActivitySettingsBinding
import eu.cyben.mobile.services.ProtectionService
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadSettings()
    }

    private fun setupViews() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Protection toggle
        binding.switchProtection.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setProtectionEnabled(isChecked)
            updateProtectionService(isChecked)
        }

        // Auto scan toggle
        binding.switchAutoScan.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setAutoScanEnabled(isChecked)
        }

        // Scan frequency
        binding.layoutScanFrequency.setOnClickListener {
            showScanFrequencyDialog()
        }

        // SMS protection toggle
        binding.switchSmsProtection.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setSmsProtectionEnabled(isChecked)
        }

        // WiFi protection toggle
        binding.switchWifiProtection.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setWifiProtectionEnabled(isChecked)
        }

        // License info
        binding.layoutLicense.setOnClickListener {
            showLicenseInfo()
        }

        // Deactivate license
        binding.btnDeactivateLicense.setOnClickListener {
            showDeactivateDialog()
        }

        // About
        binding.layoutAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun loadSettings() {
        binding.switchProtection.isChecked = preferencesManager.isProtectionEnabled()
        binding.switchAutoScan.isChecked = preferencesManager.isAutoScanEnabled()
        binding.switchSmsProtection.isChecked = preferencesManager.isSmsProtectionEnabled()
        binding.switchWifiProtection.isChecked = preferencesManager.isWifiProtectionEnabled()

        // Scan frequency
        val hours = preferencesManager.getScanFrequencyHours()
        binding.tvScanFrequency.text = when (hours) {
            6 -> "Every 6 hours"
            12 -> "Every 12 hours"
            24 -> "Daily"
            72 -> "Every 3 days"
            168 -> "Weekly"
            else -> "Every $hours hours"
        }

        // License
        val licenseKey = preferencesManager.getLicenseKey()
        if (licenseKey == "TRIAL") {
            binding.tvLicenseType.text = "Trial"
            binding.tvLicenseStatus.text = "Active"
        } else if (licenseKey != null) {
            binding.tvLicenseType.text = "Standard"
            binding.tvLicenseStatus.text = "Active"
        } else {
            binding.tvLicenseType.text = "None"
            binding.tvLicenseStatus.text = "Not activated"
        }
    }

    private fun updateProtectionService(enable: Boolean) {
        val intent = Intent(this, ProtectionService::class.java).apply {
            action = if (enable) ProtectionService.ACTION_START else ProtectionService.ACTION_STOP
        }

        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun showScanFrequencyDialog() {
        val options = arrayOf("Every 6 hours", "Every 12 hours", "Daily", "Every 3 days", "Weekly")
        val values = arrayOf(6, 12, 24, 72, 168)
        val currentHours = preferencesManager.getScanFrequencyHours()
        val currentIndex = values.indexOf(currentHours).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Scan Frequency")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                preferencesManager.setScanFrequencyHours(values[which])
                binding.tvScanFrequency.text = options[which]
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLicenseInfo() {
        val licenseKey = preferencesManager.getLicenseKey()
        val deviceId = preferencesManager.getDeviceId()

        AlertDialog.Builder(this)
            .setTitle("License Information")
            .setMessage("""
                License Key: ${licenseKey?.take(5) ?: "None"}...
                Device ID: ${deviceId.take(8)}...
                Status: Active
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeactivateDialog() {
        AlertDialog.Builder(this)
            .setTitle("Deactivate License")
            .setMessage("Are you sure you want to deactivate your license? You will need to enter a new license key to use the app.")
            .setPositiveButton("Deactivate") { _, _ ->
                deactivateLicense()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deactivateLicense() {
        lifecycleScope.launch {
            preferencesManager.setLicenseKey(null)
            Toast.makeText(this@SettingsActivity, "License deactivated", Toast.LENGTH_SHORT).show()
            
            // Go back to license screen
            val intent = Intent(this@SettingsActivity, LicenseActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }

        AlertDialog.Builder(this)
            .setTitle("About Cyben Defender")
            .setMessage("""
                Version: $versionName
                
                Cyben Mobile Defender protects your Android device from:
                • Malware and spyware
                • Phishing attacks via SMS
                • Insecure WiFi networks
                • Data breaches
                
                © 2024 Cyben di Ruben Guarnaccia
                www.cyben.eu
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
}
