package eu.cyben.guard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.RegisterRequest
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRegister.setOnClickListener { doRegister() }
    }

    private fun doRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (name.isEmpty()) { binding.etName.error = "Campo obbligatorio"; return }
        if (email.isEmpty()) { binding.etEmail.error = "Campo obbligatorio"; return }
        if (password.length < 8) { binding.etPassword.error = "Minimo 8 caratteri"; return }
        if (!binding.cbTerms.isChecked) { Toast.makeText(this, "Devi accettare i Termini di Servizio", Toast.LENGTH_SHORT).show(); return }
        if (!binding.cbPrivacy.isChecked) { Toast.makeText(this, "Devi accettare la Privacy Policy", Toast.LENGTH_SHORT).show(); return }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = api.register(RegisterRequest(name, email, password, binding.cbTerms.isChecked, binding.cbPrivacy.isChecked, binding.cbMarketing.isChecked))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    when {
                        body?.needsVerification == true -> { Toast.makeText(this@RegisterActivity, "Controlla la tua email per verificare l'account", Toast.LENGTH_LONG).show(); finish() }
                        body?.token != null -> { tokenManager.saveToken(body.token); startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java)); finishAffinity() }
                        else -> Toast.makeText(this@RegisterActivity, body?.error ?: "Registrazione fallita", Toast.LENGTH_LONG).show()
                    }
                } else Toast.makeText(this@RegisterActivity, "Errore durante la registrazione", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { Toast.makeText(this@RegisterActivity, "Errore di rete: ${e.message}", Toast.LENGTH_LONG).show() }
            finally { setLoading(false) }
        }
    }

    private fun setLoading(b: Boolean) { binding.progressBar.visibility = if (b) View.VISIBLE else View.GONE; binding.btnRegister.isEnabled = !b }
}
