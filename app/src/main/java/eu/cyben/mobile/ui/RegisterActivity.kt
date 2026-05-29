package eu.cyben.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import eu.cyben.mobile.R
import eu.cyben.mobile.api.CybenApi
import eu.cyben.mobile.api.RegisterUserRequest
import eu.cyben.mobile.databinding.ActivityRegisterBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    @Inject lateinit var api: CybenApi

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvLogin.setOnClickListener { finish() }

        binding.etPartnerCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tvPartnerCodeHint.visibility =
                    if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
        })

        binding.btnRegistrati.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val partnerCode = binding.etPartnerCode.text.toString().trim()

            if (name.isEmpty()) {
                binding.etName.error = getString(R.string.error_name_required)
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.etEmail.error = getString(R.string.error_email_required)
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = getString(R.string.error_password_required)
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.etConfirmPassword.error = getString(R.string.error_password_mismatch)
                return@setOnClickListener
            }
            if (!binding.cbTerms.isChecked) {
                Toast.makeText(this, getString(R.string.error_terms_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            register(
                name = name,
                email = email,
                password = password,
                partnerCode = if (partnerCode.isEmpty()) null else partnerCode.uppercase(Locale.getDefault())
            )
        }
    }

    private fun register(name: String, email: String, password: String, partnerCode: String?) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val request = RegisterUserRequest(
                    name = name,
                    email = email,
                    password = password,
                    termsAccepted = true,
                    privacyAccepted = true,
                    marketingConsent = false,
                    preferredLanguage = Locale.getDefault().language,
                    partnerCode = partnerCode
                )
                val response = api.registerUser(request)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.register_success),
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    finish()
                } else {
                    val msg = response.body()?.message ?: getString(R.string.error_register_failed)
                    Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    getString(R.string.error_register_failed),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegistrati.isEnabled = !loading
        binding.etName.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.etConfirmPassword.isEnabled = !loading
        binding.etPartnerCode.isEnabled = !loading
    }
}
