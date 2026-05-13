package eu.cyben.guard.ui.breach

import android.os.Bundle
import android.security.keystore.KeyProperties
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.AddEmailRequest
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.HibpCheckRequest
import eu.cyben.guard.databinding.ActivityBreachMonitorBinding
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@AndroidEntryPoint
class BreachMonitorActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityBreachMonitorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreachMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddEmail.setOnClickListener { showAddEmailDialog() }
        binding.btnHibp.setOnClickListener { showHibpDialog() }
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val resp = api.getMonitoredEmails()
                if (resp.isSuccessful) {
                    val emails = resp.body() ?: emptyList()
                    binding.tvEmailLabel.text = "Email monitorate (${emails.size}/2)"
                    binding.tvBreachSubheader.text = "${emails.size}/2 email monitorate · verifica password disponibile"
                    val breached = emails.sumOf { it.breachCount ?: 0 }
                    if (breached > 0) {
                        binding.tvBreachStatus.text = "$breached violazioni trovate"
                        binding.tvBreachStatus.setTextColor(0xFFFF1744.toInt())
                    } else {
                        binding.tvBreachStatus.text = "Nessuna rilevata"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun showAddEmailDialog() {
        val input = EditText(this).apply { hint = "email@esempio.com"; inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(this).setTitle("Aggiungi email").setView(input)
            .setPositiveButton("Aggiungi") { _, _ ->
                val email = input.text.toString().trim()
                if (email.contains("@")) addEmail(email)
                else Toast.makeText(this, "Email non valida", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun addEmail(email: String) {
        binding.progressBreach.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.addMonitoredEmail(AddEmailRequest(email, null))
                if (resp.isSuccessful) { Toast.makeText(this@BreachMonitorActivity, "Email aggiunta", Toast.LENGTH_SHORT).show(); loadData() }
                else Toast.makeText(this@BreachMonitorActivity, "Errore aggiunta email", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@BreachMonitorActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
            finally { binding.progressBreach.visibility = View.GONE }
        }
    }

    private fun showHibpDialog() {
        val input = EditText(this).apply {
            hint = "Inserisci la password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this).setTitle("Verifica password")
            .setMessage("La password non verra' inviata in chiaro. Viene usato SHA-1 k-anonymity.")
            .setView(input)
            .setPositiveButton("Verifica") { _, _ ->
                val pwd = input.text.toString()
                if (pwd.isNotEmpty()) checkPassword(pwd)
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun checkPassword(password: String) {
        binding.progressBreach.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val sha1 = sha1(password).uppercase()
                val prefix = sha1.take(5)
                val suffix = sha1.drop(5)
                val resp = okhttp3.OkHttpClient().newCall(
                    okhttp3.Request.Builder().url("https://api.pwnedpasswords.com/range/$prefix").build()
                ).execute()
                val body = resp.body?.string() ?: ""
                val count = body.lines().find { it.startsWith(suffix) }?.split(":")?.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                if (count > 0) {
                    AlertDialog.Builder(this@BreachMonitorActivity)
                        .setTitle("Password compromessa")
                        .setMessage("Questa password e' apparsa $count volte in violazioni note. Cambiala subito.")
                        .setPositiveButton("OK", null).show()
                } else {
                    Toast.makeText(this@BreachMonitorActivity, "Password non trovata in violazioni note", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) { Toast.makeText(this@BreachMonitorActivity, "Errore verifica", Toast.LENGTH_SHORT).show() }
            finally { binding.progressBreach.visibility = View.GONE }
        }
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}