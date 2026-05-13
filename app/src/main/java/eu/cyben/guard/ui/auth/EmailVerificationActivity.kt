package eu.cyben.guard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.EmailRequest
import eu.cyben.guard.databinding.ActivityEmailVerificationBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmailVerificationActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityEmailVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val email = intent.getStringExtra("email") ?: ""
        binding.tvMessage.text = "Abbiamo inviato un link di verifica a $email. Clicca il link e poi torna qui per accedere."
        binding.btnResend.setOnClickListener {
            if (email.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        api.resendVerification(EmailRequest(email))
                        Toast.makeText(this@EmailVerificationActivity, "Email reinviata a $email", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
            }
        }
        binding.btnVerified.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                putExtra("email", email)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }
    }
}
