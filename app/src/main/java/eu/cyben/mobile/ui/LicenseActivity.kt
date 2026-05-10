package eu.cyben.mobile.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import eu.cyben.mobile.api.ActivateLicenseRequest
import eu.cyben.mobile.api.CybenApi
import eu.cyben.mobile.databinding.ActivityLicenseBinding
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LicenseActivity : AppCompatActivity() {

    @Inject lateinit var api: CybenApi
    @Inject lateinit var preferencesManager: PreferencesManager

    private lateinit var binding: ActivityLicenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.btnActivate.setOnClickListener {
            val licenseKey = binding.etLicenseKey.text.toString().trim().uppercase()

            if (licenseKey.isEmpty()) {
                binding.etLicenseKey.error = "Please enter a license key"
                return@setOnClickListener
            }

            if (!isValidLicenseFormat(licenseKey)) {
                binding.etLicenseKey.error = "Invalid license key format"
                return@setOnClickListener
            }

            activateLicense(licenseKey)
        }

        binding.btnSkipTrial.setOnClickListener {
            // Start with trial mode
            startTrialMode()
        }
    }

    private fun isValidLicenseFormat(key: String): Boolean {
        // Expected format: XXXXX-XXXXX-XXXXX-XXXXX
        val pattern = Regex("^[A-Z0-9]{5}-[A-Z0-9]{5}-[A-Z0-9]{5}-[A-Z0-9]{5}$")
        return pattern.matches(key)
    }

    private fun activateLicense(licenseKey: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val request = ActivateLicenseRequest(
                    licenseKey = licenseKey,
                    deviceId = preferencesManager.getDeviceId(),
                    deviceName = Build.DEVICE,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    osVersion = Build.VERSION.RELEASE,
                    appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                )

                val response = api.activateLicense(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    // Save license
                    preferencesManager.setLicenseKey(licenseKey)
                    
                    Toast.makeText(
                        this@LicenseActivity,
                        "License activated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Go to dashboard
                    startActivity(Intent(this@LicenseActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    val errorMessage = response.body()?.message ?: "Activation failed"
                    binding.etLicenseKey.error = errorMessage
                    Toast.makeText(this@LicenseActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LicenseActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun startTrialMode() {
        // Set a trial license marker
        preferencesManager.setLicenseKey("TRIAL")
        
        Toast.makeText(this, "Trial mode activated (7 days)", Toast.LENGTH_SHORT).show()
        
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnActivate.isEnabled = !loading
        binding.btnSkipTrial.isEnabled = !loading
        binding.etLicenseKey.isEnabled = !loading
    }
}
