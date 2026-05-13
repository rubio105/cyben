package eu.cyben.guard.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.EmailRequest
import eu.cyben.guard.data.api.HumanRequest
import eu.cyben.guard.data.api.SOSRequest
import eu.cyben.guard.data.models.GuardUser
import eu.cyben.guard.databinding.ActivitySettingsBinding
import eu.cyben.guard.ui.auth.LoginActivity
import eu.cyben.guard.ui.subscription.SubscriptionActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivitySettingsBinding
    private var userEmail = ""
    private var currentUser: GuardUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnPasswordCheck.setOnClickListener { showPasswordCheckDialog() }
        binding.btnChangePassword.setOnClickListener { changePassword() }
        binding.btnProhmed.setOnClickListener { handleProhmed() }
        binding.btnHistory.setOnClickListener { startActivity(Intent(this, AnalysisHistoryActivity::class.java)) }
        binding.btnHumanRequest.setOnClickListener { showHumanRequestDialog() }
        binding.btnSos.setOnClickListener { showSOSDialog() }
        binding.btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }
        binding.btnLogout.setOnClickListener { logout() }
        loadUser()
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val resp = api.getMe()
                if (resp.isSuccessful) {
                    currentUser = resp.body()
                    userEmail = currentUser?.email ?: ""
                    binding.tvName.text = currentUser?.name ?: ""
                    binding.tvEmail.text = currentUser?.email ?: ""
                    binding.tvPlan.text = "Piano: ${currentUser?.planLabel ?: ""}"
                    updateProhmedButton()
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateProhmedButton() {
        val user = currentUser ?: return
        if (user.isProhmedEnabled) {
            binding.btnProhmed.text = "Apri Prohmed Health"
            binding.btnProhmed.backgroundTintList =
                android.content.res.ColorStateList.valueOf(getColor(android.R.color.holo_green_dark))
        } else {
            binding.btnProhmed.text = "Prohmed Health (Piano Annuale)"
            binding.btnProhmed.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF424242.toInt())
        }
    }

    private fun handleProhmed() {
        if (currentUser?.isProhmedEnabled == true) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://prohmed.it")))
        } else if (currentUser?.isPremium == true) {
            AlertDialog.Builder(this)
                .setTitle("Prohmed Health")
                .setMessage("Prohmed Health e incluso nel Piano Premium Annuale. Passa al piano annuale per accedere.")
                .setPositiveButton("Passa ad Annuale") { _, _ -> startActivity(Intent(this, SubscriptionActivity::class.java)) }
                .setNegativeButton("Annulla", null).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Prohmed Health")
                .setMessage("Prohmed Health e incluso nel Piano Premium Annuale. Abbonati per accedere a consulti medici online, cartella clinica digitale e molto altro.")
                .setPositiveButton("Scopri Premium") { _, _ -> startActivity(Intent(this, SubscriptionActivity::class.java)) }
                .setNegativeButton("Annulla", null).show()
        }
    }

    private fun showPasswordCheckDialog() {
        val input = EditText(this).apply {
            hint = "Password da verificare"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Verifica Password (HIBP)")
            .setMessage("Controlla se la password e stata compromessa in breach noti.")
            .setView(input)
            .setPositiveButton("Verifica") { _, _ ->
                val pw = input.text.toString()
                if (pw.isNotEmpty()) checkPasswordHIBP(pw)
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun checkPasswordHIBP(password: String) {
        lifecycleScope.launch {
            try {
                val hash = withContext(Dispatchers.Default) {
                    MessageDigest.getInstance("SHA-1")
                        .digest(password.toByteArray())
                        .joinToString("") { "%02X".format(it) }
                }
                val prefix = hash.take(5)
                val suffix = hash.drop(5)
                val response = withContext(Dispatchers.IO) {
                    URL("https://api.pwnedpasswords.com/range/$prefix").readText()
                }
                val matchLine = response.lines().firstOrNull { it.startsWith(suffix, ignoreCase = true) }
                val count = matchLine?.split(":")?.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                if (matchLine != null) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Password Compromessa")
                        .setMessage("Trovata in $count breach. Cambiala subito!")
                        .setPositiveButton("Cambia Password") { _, _ -> changePassword() }
                        .setNegativeButton("Ignora", null).show()
                } else {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Password Sicura")
                        .setMessage("Nessun breach noto trovato per questa password.")
                        .setPositiveButton("OK", null).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Errore verifica: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePassword() {
        if (userEmail.isEmpty()) return
        lifecycleScope.launch {
            try {
                api.forgotPassword(EmailRequest(userEmail))
                Toast.makeText(this@SettingsActivity, "Email di reset inviata a $userEmail", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showHumanRequestDialog() {
        val input = EditText(this).apply { hint = "Descrivi la tua richiesta..."; setPadding(48, 32, 48, 32); minLines = 3 }
        AlertDialog.Builder(this)
            .setTitle("Parla con un Esperto")
            .setMessage("Un esperto ti risponderà entro 4 ore.")
            .setView(input)
            .setPositiveButton("Invia") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) lifecycleScope.launch {
                    try {
                        val resp = api.requestHuman(HumanRequest(text, null))
                        if (resp.isSuccessful) Toast.makeText(this@SettingsActivity, "Richiesta inviata.", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun showSOSDialog() {
        val input = EditText(this).apply { hint = "Descrivi l incidente..."; setPadding(48, 32, 48, 32); minLines = 3 }
        AlertDialog.Builder(this)
            .setTitle("SOS Incidente Critico")
            .setMessage("Un esperto risponderà entro 4 ore.")
            .setView(input)
            .setPositiveButton("Invia SOS") { _, _ ->
                val desc = input.text.toString().trim()
                if (desc.isNotEmpty()) lifecycleScope.launch {
                    try {
                        api.sendSOS(SOSRequest(desc))
                        Toast.makeText(this@SettingsActivity, "SOS inviato.", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Elimina Account")
            .setMessage("Sei sicuro? Tutti i tuoi dati, abbonamento e storico verranno eliminati definitivamente. Questa azione non puo essere annullata.")
            .setPositiveButton("Elimina") { _, _ -> deleteAccount() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun deleteAccount() {
        lifecycleScope.launch {
            try {
                val resp = api.deleteAccount()
                if (resp.isSuccessful) {
                    tokenManager.clearToken()
                    Toast.makeText(this@SettingsActivity, "Account eliminato.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    Toast.makeText(this@SettingsActivity, "Errore eliminazione account", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        tokenManager.clearToken()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
