package eu.cyben.guard.ui.breach

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.AddEmailRequest
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.databinding.ActivityBreachMonitorBinding
import kotlinx.coroutines.launch
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
        loadEmails()
    }

    private fun loadEmails() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getMonitoredEmails()
                if (resp.isSuccessful) {
                    val emails = resp.body() ?: emptyList()
                    if (emails.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmailList.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.tvEmailList.visibility = View.VISIBLE
                        binding.tvEmailList.text = emails.joinToString("\n\n") {
                            "📧 ${it.email}\n   Violazioni: ${it.breachCount ?: 0}"
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@BreachMonitorActivity, "Errore caricamento", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showAddEmailDialog() {
        val input = EditText(this).apply { hint = "email@esempio.com"; setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(this)
            .setTitle("Monitora email")
            .setView(input)
            .setPositiveButton("Aggiungi") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) addEmail(email)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun addEmail(email: String) {
        lifecycleScope.launch {
            try {
                val resp = api.addMonitoredEmail(AddEmailRequest(email, null))
                if (resp.isSuccessful) {
                    Toast.makeText(this@BreachMonitorActivity, "Email aggiunta", Toast.LENGTH_SHORT).show()
                    loadEmails()
                } else {
                    Toast.makeText(this@BreachMonitorActivity, "Errore aggiunta email", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BreachMonitorActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
