package eu.cyben.guard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.EmailRequest
import eu.cyben.guard.data.api.LoginRequest
import eu.cyben.guard.databinding.ActivityLoginBinding
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (tokenManager.isLoggedIn()) { goToDashboard(); return }
        binding.btnLogin.setOnClickListener { doLogin() }
        binding.btnRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) { binding.etEmail.error = "Inserisci la tua email"; return@setOnClickListener }
            lifecycleScope.launch {
                try { api.forgotPassword(EmailRequest(email)); Toast.makeText(this@LoginActivity, "Email di recupero inviata", Toast.LENGTH_SHORT).show() }
                catch (e: Exception) { Toast.makeText(this@LoginActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (email.isEmpty()) { binding.etEmail.error = "Campo obbligatorio"; return }
        if (password.isEmpty()) { binding.etPassword.error = "Campo obbligatorio"; return }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = api.login(LoginRequest(email, password))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    when {
                        body?.needsVerification == true -> Toast.makeText(this@LoginActivity, "Verifica la tua email prima di accedere", Toast.LENGTH_LONG).show()
                        body?.token != null -> { tokenManager.saveToken(body.token); goToDashboard() }
                        else -> Toast.makeText(this@LoginActivity, body?.error ?: "Login fallito", Toast.LENGTH_LONG).show()
                    }
                } else Toast.makeText(this@LoginActivity, "Credenziali non valide", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { Toast.makeText(this@LoginActivity, "Errore di rete: ${e.message}", Toast.LENGTH_LONG).show() }
            finally { setLoading(false) }
        }
    }

    private fun goToDashboard() { startActivity(Intent(this, DashboardActivity::class.java)); finish() }
    private fun setLoading(b: Boolean) { binding.progressBar.visibility = if (b) View.VISIBLE else View.GONE; binding.btnLogin.isEnabled = !b }
}
