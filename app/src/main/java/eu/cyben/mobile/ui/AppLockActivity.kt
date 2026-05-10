package eu.cyben.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import eu.cyben.mobile.R
import eu.cyben.mobile.databinding.ActivityAppLockBinding
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppLockActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    private lateinit var binding: ActivityAppLockBinding
    private var enteredPin = StringBuilder()
    private var attemptsRemaining = 5
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetPackage = intent.getStringExtra("target_package")
        
        setupPinPad()
        setupBiometric()
    }

    private fun setupPinPad() {
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (enteredPin.length < 6) {
                    enteredPin.append(index)
                    updatePinDisplay()
                    
                    if (enteredPin.length == 4) {
                        checkPin()
                    }
                }
            }
        }

        binding.btnBackspace.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDisplay()
            }
        }

        binding.btnBiometric.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun updatePinDisplay() {
        val dots = binding.pinDots.children.toList()
        dots.forEachIndexed { index, view ->
            view.isActivated = index < enteredPin.length
        }
    }

    private val android.view.ViewGroup.children: Sequence<View>
        get() = object : Sequence<View> {
            override fun iterator() = object : Iterator<View> {
                private var index = 0
                override fun hasNext() = index < childCount
                override fun next() = getChildAt(index++)
            }
        }

    private fun checkPin() {
        val savedPin = preferencesManager.getAppLockPin()
        
        if (enteredPin.toString() == savedPin) {
            onUnlockSuccess()
        } else {
            attemptsRemaining--
            enteredPin.clear()
            updatePinDisplay()
            
            if (attemptsRemaining <= 0) {
                Toast.makeText(this, "Too many attempts. Try again later.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                binding.tvError.text = getString(R.string.attempts_remaining, attemptsRemaining)
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun setupBiometric() {
        val biometricManager = BiometricManager.from(this)
        
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.btnBiometric.visibility = View.VISIBLE
                showBiometricPrompt()
            }
            else -> {
                binding.btnBiometric.visibility = View.GONE
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlockSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User can still use PIN
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@AppLockActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock App")
            .setSubtitle("Use your fingerprint to unlock")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun onUnlockSuccess() {
        setResult(RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        // Go to home screen instead of unlocking
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
