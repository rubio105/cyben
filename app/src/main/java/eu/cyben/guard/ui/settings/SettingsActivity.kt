package eu.cyben.guard.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.SOSRequest
import eu.cyben.guard.databinding.ActivitySettingsBinding
import eu.cyben.guard.ui.auth.LoginActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSos.setOnClickListener { showSOSDialog() }
        binding.btnLogout.setOnClickListener { logout() }
        loadUser()
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val resp = api.getMe()
                if (resp.isSuccessful) {
                    val user = resp.body()
                    binding.tvName.text = user?.name ?: ""
                    binding.tvEmail.text = user?.email ?: ""
                    binding.tvPlan.text = "Piano: ${user?.planLabel ?: "Gratuito"}"
                }
            } catch (_: Exception) {}
        }
    }

    private fun showSOSDialog() {
        val input = android.widget.EditText(this).apply { hint = "Descrivi l'incidente..."; setPadding(48, 32, 48, 32); minLines = 3 }
        AlertDialog.Builder(this)
            .setTitle("🚨 SOS Incidente Critico")
            .setMessage("Descrivi l'incidente. Un esperto risponderà entro 4 ore.")
            .setView(input)
            .setPositiveButton("Invia SOS") { _, _ ->
                val desc = input.text.toString().trim()
                if (desc.isNotEmpty()) sendSOS(desc)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun sendSOS(description: String) {
        lifecycleScope.launch {
            try {
                val resp = api.sendSOS(SOSRequest(description))
                if (resp.isSuccessful) Toast.makeText(this@SettingsActivity, "SOS inviato. Un esperto ti contatterà presto.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { Toast.makeText(this@SettingsActivity, "Errore invio SOS", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun logout() {
        tokenManager.clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}
