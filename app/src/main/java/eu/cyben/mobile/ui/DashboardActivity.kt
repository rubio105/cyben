package eu.cyben.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import eu.cyben.mobile.R
import eu.cyben.mobile.databinding.ActivityDashboardBinding
import eu.cyben.mobile.services.ProtectionService
import eu.cyben.mobile.services.SecurityScanService
import eu.cyben.mobile.utils.BreachChecker
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var breachChecker: BreachChecker

    private lateinit var binding: ActivityDashboardBinding
    private var isScanning = false

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            preferencesManager.setSmsProtectionEnabled(true)
            updateSmsProtectionStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        updateDashboard()
        startProtectionService()
    }

    private fun setupViews() {
        // Scan button
        binding.btnScan.setOnClickListener {
            if (!isScanning) {
                startScan()
            }
        }

        // Feature cards
        binding.cardAppScan.setOnClickListener {
            startScan()
        }

        binding.cardSmsProtection.setOnClickListener {
            toggleSmsProtection()
        }

        binding.cardBreachCheck.setOnClickListener {
            checkBreaches()
        }

        binding.cardWifiScanner.setOnClickListener {
            // WiFi scanner activity
        }

        binding.cardQrScanner.setOnClickListener {
            openQrScanner()
        }

        binding.cardAppLock.setOnClickListener {
            // App lock settings
        }

        binding.cardPermissions.setOnClickListener {
            // Permission manager activity
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateDashboard() {
        // Protection status
        val isProtected = preferencesManager.isProtectionEnabled()
        val threatsCount = preferencesManager.getActiveThreatsCount()

        if (isProtected && threatsCount == 0) {
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
            binding.tvStatusTitle.text = getString(R.string.status_protected)
            binding.tvStatusTitle.setTextColor(getColor(R.color.status_safe))
            binding.cardStatus.setCardBackgroundColor(getColor(R.color.status_safe).withAlpha(20))
        } else if (threatsCount > 0) {
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_warning)
            binding.tvStatusTitle.text = getString(R.string.threats_found, threatsCount)
            binding.tvStatusTitle.setTextColor(getColor(R.color.status_danger))
            binding.cardStatus.setCardBackgroundColor(getColor(R.color.status_danger).withAlpha(20))
        } else {
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield)
            binding.tvStatusTitle.text = getString(R.string.status_warning)
            binding.tvStatusTitle.setTextColor(getColor(R.color.status_warning))
            binding.cardStatus.setCardBackgroundColor(getColor(R.color.status_warning).withAlpha(20))
        }

        // Last scan info
        val lastScanTime = preferencesManager.getLastScanTime()
        if (lastScanTime != null) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.tvLastScan.text = getString(R.string.last_scan) + ": " + dateFormat.format(Date(lastScanTime))
        } else {
            binding.tvLastScan.text = getString(R.string.last_scan) + ": " + getString(R.string.never_scanned)
        }

        // SMS protection status
        updateSmsProtectionStatus()
    }

    private fun updateSmsProtectionStatus() {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val isEnabled = hasSmsPermission && preferencesManager.isSmsProtectionEnabled()

        binding.tvSmsStatus.text = if (isEnabled) "Active" else "Disabled"
        binding.tvSmsStatus.setTextColor(
            if (isEnabled) getColor(R.color.status_safe) else getColor(R.color.status_warning)
        )
    }

    private fun startScan() {
        isScanning = true
        binding.btnScan.isEnabled = false
        binding.progressScan.visibility = View.VISIBLE
        binding.tvScanStatus.text = getString(R.string.scanning)

        val intent = Intent(this, SecurityScanService::class.java).apply {
            action = SecurityScanService.ACTION_START_SCAN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Listen for scan completion
        // In a real app, use a BroadcastReceiver or ViewModel with LiveData
        binding.root.postDelayed({
            isScanning = false
            binding.btnScan.isEnabled = true
            binding.progressScan.visibility = View.GONE
            binding.tvScanStatus.text = getString(R.string.scan_now)
            updateDashboard()
        }, 30000) // Timeout after 30 seconds
    }

    private fun toggleSmsProtection() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        } else {
            val currentState = preferencesManager.isSmsProtectionEnabled()
            preferencesManager.setSmsProtectionEnabled(!currentState)
            updateSmsProtectionStatus()
        }
    }

    private fun checkBreaches() {
        binding.progressBreaches.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Get installed apps
                val packages = packageManager.getInstalledPackages(0)
                    .map { it.packageName }

                val breaches = breachChecker.checkAppsForBreaches(packages)

                binding.progressBreaches.visibility = View.GONE

                if (breaches.isNotEmpty()) {
                    binding.tvBreachCount.text = "${breaches.size} breaches found"
                    binding.tvBreachCount.setTextColor(getColor(R.color.status_warning))
                    // Show breach details dialog or activity
                } else {
                    binding.tvBreachCount.text = "No breaches found"
                    binding.tvBreachCount.setTextColor(getColor(R.color.status_safe))
                }
            } catch (e: Exception) {
                binding.progressBreaches.visibility = View.GONE
                binding.tvBreachCount.text = "Check failed"
            }
        }
    }

    private fun startProtectionService() {
        if (preferencesManager.isProtectionEnabled()) {
            val intent = Intent(this, ProtectionService::class.java).apply {
                action = ProtectionService.ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun openQrScanner() {
        val licenseType = preferencesManager.getLicenseType()
        
        if (licenseType == "premium" || licenseType == "enterprise") {
            startActivity(Intent(this, QRScannerActivity::class.java))
        } else {
            // Show premium required dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.premium_required)
                .setMessage(R.string.premium_qr_message)
                .setPositiveButton(R.string.activate_license) { _, _ ->
                    startActivity(Intent(this, LicenseActivity::class.java))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (alpha shl 24) or (this and 0x00FFFFFF)
    }
}
