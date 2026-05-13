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
import eu.cyben.guard.data.models.ChangePasswordRequest
import eu.cyben.guard.data.api.SOSRequest
import eu.cyben.guard.data.models.GuardUser
import eu.cyben.guard.databinding.ActivitySettingsBinding
import eu.cyben.guard.ui.analysis.AnalysisHistoryActivity
import eu.cyben.guard.ui.auth.LoginActivity
import eu.cyben.guard.ui.prohmed.ProhmedActivity
import eu.cyben.guard.ui.subscription.SubscriptionActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivitySettingsBinding
    private var currentUser: GuardUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.cardSubscription.setOnClickListener { startActivity(Intent(this, SubscriptionActivity::class.java)) }
        binding.cardHistory.setOnClickListener { startActivity(Intent(this, AnalysisHistoryActivity::class.java)) }
        binding.cardProtection.setOnClickListener { showProtectionInfo() }
        binding.cardSos.setOnClickListener { showSOSDialog() }
        binding.rowChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.rowDeleteAccount.setOnClickListener { confirmDeleteAccount() }
        binding.rowPrivacy.setOnClickListener { openUrl("https://cyben.eu/privacy") }
        binding.rowTerms.setOnClickListener { openUrl("https://cyben.eu/terms") }
        binding.cardLogout.setOnClickListener { logout() }
        binding.btnProhmedConsult.setOnClickListener { startActivity(Intent(this, ProhmedActivity::class.java)) }
        loadUser()
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val resp = api.getMe()
                if (resp.isSuccessful) {
                    currentUser = resp.body()
                    val user = currentUser ?: return@launch
                    val initial = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    binding.tvAvatar.text = initial
                    binding.tvName.text = user.name
                    binding.tvEmail.text = user.email
                    binding.tvPlanBadge.text = "✦ ${user.planLabel}"
                    binding.tvSubTitle.text = "Piano ${user.planLabel}"
                    binding.tvSubDesc.text = if (user.isPremiumAnnual) "Tutte le funzioni Premium attive, incluso Health Prohmed" else "Tutte le funzioni Premium attive"
                    if (user.isProhmedEnabled) {
                        loadProhmedStatus()
                    }
                } else if (resp.code() == 401) logout()
            } catch (_: Exception) {}
        }
    }

    private fun loadProhmedStatus() {
        lifecycleScope.launch {
            try {
                val resp = api.getProhmedStatus()
                if (resp.isSuccessful) {
                    val status = resp.body() ?: return@launch
                    binding.labelProhmed.visibility = android.view.View.VISIBLE
                    binding.cardProhmed.visibility = android.view.View.VISIBLE
                    if (status.activated) {
                        binding.tvProhmedStatus.text = "Profilo attivo"
                        binding.tvProhmedEmail.text = status.email ?: ""
                        val res = status.residui ?: 0
                        binding.tvProhmedResidui.text = "$res residui"
                        binding.tvProhmedUsati.text = "0/$res usati"
                    } else {
                        binding.tvProhmedStatus.text = "Profilo non attivato"
                        binding.btnProhmedConsult.text = "Attiva Prohmed"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun showProtectionInfo() {
        AlertDialog.Builder(this)
            .setTitle("Protezione immediata")
            .setMessage("SMS e chiamate vengono analizzati automaticamente in background quando i permessi sono concessi. La protezione WhatsApp richiede di condividere manualmente i messaggi sospetti tramite la funzione Condividi.")
            .setPositiveButton("OK", null).show()
    }

    private fun showSOSDialog() {
        val input = EditText(this).apply { hint = "Descrivi l'incidente..."; setPadding(48, 32, 48, 32); minLines = 3 }
        AlertDialog.Builder(this).setTitle("SOS Incidente Critico")
            .setMessage("Un esperto risponderà entro 4 ore.")
            .setView(input)
            .setPositiveButton("Invia SOS") { _, _ ->
                val desc = input.text.toString().trim()
                if (desc.isNotEmpty()) sendSOS(desc)
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun sendSOS(description: String) {
        lifecycleScope.launch {
            try {
                val resp = api.sendSOS(SOSRequest(description))
                if (resp.isSuccessful) Toast.makeText(this@SettingsActivity, "SOS inviato. Un esperto ti contatterà presto.", Toast.LENGTH_LONG).show()
                else Toast.makeText(this@SettingsActivity, "Errore invio SOS", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@SettingsActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showChangePasswordDialog() {
        val layout = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(48, 16, 48, 8) }
        val etCurrent = EditText(this).apply { hint = "Password attuale"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val etNew = EditText(this).apply { hint = "Nuova password"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; setPadding(0, 8, 0, 0) }
        layout.addView(etCurrent); layout.addView(etNew)
        AlertDialog.Builder(this).setTitle("Cambia password").setView(layout)
            .setPositiveButton("Aggiorna") { _, _ ->
                val cur = etCurrent.text.toString().trim()
                val nw = etNew.text.toString().trim()
                if (cur.isNotEmpty() && nw.length >= 8) changePassword(cur, nw)
                else Toast.makeText(this, "La nuova password deve essere di almeno 8 caratteri", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun changePassword(current: String, new: String) {
        lifecycleScope.launch {
            try {
                val resp = api.changePassword(ChangePasswordRequest(current, new))
                if (resp.isSuccessful) Toast.makeText(this@SettingsActivity, "Password aggiornata", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@SettingsActivity, "Password attuale errata", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@SettingsActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this).setTitle("Elimina account")
            .setMessage("Questa azione e' irreversibile. Tutti i tuoi dati verranno cancellati definitivamente.")
            .setPositiveButton("Elimina") { _, _ ->
                AlertDialog.Builder(this).setTitle("Sei sicuro?")
                    .setMessage("Confermi l'eliminazione definitiva del tuo account?")
                    .setPositiveButton("Elimina definitivamente") { _, _ -> deleteAccount() }
                    .setNegativeButton("Annulla", null).show()
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun deleteAccount() {
        lifecycleScope.launch {
            try {
                val resp = api.deleteAccount()
                if (resp.isSuccessful || resp.code() == 204) {
                    tokenManager.clearToken()
                    Toast.makeText(this@SettingsActivity, "Account eliminato", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SettingsActivity, LoginActivity::class.java))
                    finishAffinity()
                } else Toast.makeText(this@SettingsActivity, "Errore eliminazione account", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@SettingsActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun openUrl(url: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    private fun logout() {
        tokenManager.clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}