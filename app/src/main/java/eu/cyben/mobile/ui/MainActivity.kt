package eu.cyben.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import eu.cyben.mobile.R
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    private val requiredPermissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Continue to dashboard regardless of permission results
        navigateToDashboard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Splash delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkPermissionsAndProceed()
        }, 1500)
    }

    private fun checkPermissionsAndProceed() {
        if (preferencesManager.isFirstLaunch()) {
            // First launch - request permissions
            val permissionsToRequest = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                navigateToDashboard()
            }

            preferencesManager.setFirstLaunchCompleted()
        } else {
            navigateToDashboard()
        }
    }

    private fun navigateToDashboard() {
        // Check if license is activated
        val licenseKey = preferencesManager.getLicenseKey()

        if (licenseKey == null) {
            // Go to license activation screen
            startActivity(Intent(this, LicenseActivity::class.java))
        } else {
            // Go to dashboard
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        finish()
    }
}
