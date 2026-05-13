package eu.cyben.guard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.EmailRequest
import eu.cyben.guard.data.api.VerifyEmailRequest
import eu.cyben.guard.databinding.ActivityEmailVerificationBinding
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmailVerificationActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityEmailVerificationBinding
    private var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        email = intent.getStringExtra("email") ?: ""
        binding.tvMessage.text = "Abbiamo inviato un codice a 6 cifre a $email. Inseriscilo qui sotto per attivare il tuo account."
        binding.btnVerified.setOnClickListener { verifyCode() }
        binding.btnResend.setOnClickListener { resendEmail() }
    }

    private fun verifyCode() {
        val code = binding.etVerificationCode.text?.toString()?.trim() ?: ""
        if (code.length != 6) {
            binding.etVerificationCode.error = "Inserisci il codice a 6 cifre"
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = api.verifyEmail(VerifyEmailRequest(email, code))
                if (resp.isSuccessful) {
                    val token = resp.body()?.token
                    if (token != null) {
                        tokenManager.saveToken(token)
                        startActivity(Intent(this@EmailVerificationActivity, DashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
                        Toast.makeText(this@EmailVerificationActivity, resp.body()?.error ?: "Verifica fallita", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@EmailVerificationActivity, "Codice non valido o scaduto", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EmailVerificationActivity, "Errore di rete: \${e.message}", Toast.LENGTH_LONG).show()
            } finally { setLoading(false) }
        }
    }

    private fun resendEmail() {
        if (email.isEmpty()) return
        lifecycleScope.launch {
            try {
                api.resendVerification(EmailRequest(email))
                Toast.makeText(this@EmailVerificationActivity, "Nuovo codice inviato a $email", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    private fun setLoading(b: Boolean) {
        binding.btnVerified.isEnabled = !b
        binding.btnResend.isEnabled = !b
        binding.etVerificationCode.isEnabled = !b
    }
}