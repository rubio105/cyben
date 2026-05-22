package eu.cyben.guard.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.R
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.RegisterRequest
import eu.cyben.guard.data.api.ValidatePartnerCodeRequest
import eu.cyben.guard.databinding.ActivityRegisterBinding
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityRegisterBinding
    private var passwordVisible = false
    private var selectedLanguage = "it"
    private var partnerCodeValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogin.setOnClickListener { finish() }
        binding.btnRegister.setOnClickListener { doRegister() }
        binding.btnTogglePwd.setOnClickListener { togglePassword() }
        binding.chipLangIT.setOnClickListener { selectLanguage("it") }
        binding.chipLangEN.setOnClickListener { selectLanguage("en") }
        binding.btnVerifyCode.setOnClickListener { verifyPartnerCode() }
        binding.etPartnerCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                partnerCodeValid = false
                binding.tvPartnerCodeStatus.visibility = View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateStrength(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun selectLanguage(lang: String) {
        selectedLanguage = lang
        binding.chipLangIT.setBackgroundResource(if (lang == "it") R.drawable.chip_lang_selected else R.drawable.chip_lang_unselected)
        binding.chipLangEN.setBackgroundResource(if (lang == "en") R.drawable.chip_lang_selected else R.drawable.chip_lang_unselected)
        binding.chipLangIT.setTextColor(if (lang == "it") 0xFFFFFFFF.toInt() else 0x99FFFFFF.toInt())
        binding.chipLangEN.setTextColor(if (lang == "en") 0xFFFFFFFF.toInt() else 0x99FFFFFF.toInt())
    }

    private fun togglePassword() {
        passwordVisible = !passwordVisible
        val cursor = binding.etPassword.selectionEnd
        binding.etPassword.transformationMethod = if (passwordVisible)
            HideReturnsTransformationMethod.getInstance()
        else PasswordTransformationMethod.getInstance()
        binding.etPassword.setSelection(cursor)
        binding.btnTogglePwd.setImageResource(if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye)
    }

    private fun updateStrength(pwd: String) {
        if (pwd.isEmpty()) { binding.strengthBar.visibility = View.GONE; return }
        binding.strengthBar.visibility = View.VISIBLE
        var score = 0
        if (pwd.length >= 8) score++
        if (pwd.length >= 12) score++
        if (pwd.any { it.isUpperCase() }) score++
        if (pwd.any { it.isDigit() }) score++
        if (pwd.any { "!@#\$%^&*()_+-=[]{}|;':\",./<>?".contains(it) }) score++
        val (label, hex) = when {
            score <= 1 -> Pair("Molto debole", "#FF4444")
            score == 2 -> Pair("Debole", "#FF8800")
            score == 3 -> Pair("Discreta", "#E6B300")
            else -> Pair("Forte", "#44BB44")
        }
        val filled = minOf(score + 1, 4).coerceAtLeast(1)
        listOf(binding.seg1, binding.seg2, binding.seg3, binding.seg4).forEachIndexed { i, v ->
            v.setBackgroundColor(if (i < filled) Color.parseColor(hex) else Color.parseColor("#FFFFFF26"))
        }
        binding.tvPasswordStrength.text = label
        binding.tvPasswordStrength.setTextColor(Color.parseColor(hex))
    }

    private fun verifyPartnerCode() {
        val code = binding.etPartnerCode.text.toString().trim()
        if (code.isEmpty()) return
        binding.btnVerifyCode.isEnabled = false
        binding.tvPartnerCodeStatus.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val resp = api.validatePartnerCode(ValidatePartnerCodeRequest(code))
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val body = resp.body()!!
                    val planLabel = if (body.plan == "premium") "Premium" else "Basic"
                    val intervalLabel = if (body.interval == "monthly") "1 mese" else "12 mesi"
                    partnerCodeValid = true
                    binding.tvPartnerCodeStatus.text = "✓ Codice valido — Piano $planLabel per $intervalLabel"
                    binding.tvPartnerCodeStatus.setTextColor(Color.parseColor("#44BB44"))
                } else {
                    partnerCodeValid = false
                    binding.tvPartnerCodeStatus.text = "✗ Codice non valido o scaduto"
                    binding.tvPartnerCodeStatus.setTextColor(Color.parseColor("#FF4444"))
                }
                binding.tvPartnerCodeStatus.visibility = View.VISIBLE
            } catch (e: Exception) {
                partnerCodeValid = false
                binding.tvPartnerCodeStatus.text = "✗ Errore di rete"
                binding.tvPartnerCodeStatus.setTextColor(Color.parseColor("#FF4444"))
                binding.tvPartnerCodeStatus.visibility = View.VISIBLE
            } finally {
                binding.btnVerifyCode.isEnabled = true
            }
        }
    }

    private fun doRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()
        val partnerCode = binding.etPartnerCode.text.toString().trim().takeIf { it.isNotEmpty() }
        if (name.isEmpty()) { binding.etName.error = "Campo obbligatorio"; return }
        if (email.isEmpty()) { binding.etEmail.error = "Campo obbligatorio"; return }
        if (password.length < 8) { binding.etPassword.error = "Minimo 8 caratteri"; return }
        if (password != confirm) { binding.etConfirmPassword.error = "Le password non coincidono"; return }
        if (!binding.cbTerms.isChecked) { Toast.makeText(this, "Devi accettare i Termini di Servizio", Toast.LENGTH_SHORT).show(); return }
        if (!binding.cbPrivacy.isChecked) { Toast.makeText(this, "Devi accettare la Privacy Policy", Toast.LENGTH_SHORT).show(); return }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = api.register(RegisterRequest(name, email, password, binding.cbTerms.isChecked, binding.cbPrivacy.isChecked, binding.cbMarketing.isChecked, selectedLanguage, partnerCode))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    when {
                        body?.needsVerification == true -> { startActivity(Intent(this@RegisterActivity, EmailVerificationActivity::class.java).apply { putExtra("email", email) }); finish() }
                        body?.token != null -> { tokenManager.saveToken(body.token); startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java)); finishAffinity() }
                        else -> Toast.makeText(this@RegisterActivity, body?.error ?: "Registrazione fallita", Toast.LENGTH_LONG).show()
                    }
                } else Toast.makeText(this@RegisterActivity, "Errore durante la registrazione", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Errore di rete: ${e.message}", Toast.LENGTH_LONG).show()
            } finally { setLoading(false) }
        }
    }

    private fun setLoading(b: Boolean) {
        binding.progressBar.visibility = if (b) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !b
    }
}
