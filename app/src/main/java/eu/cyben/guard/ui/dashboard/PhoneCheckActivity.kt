package eu.cyben.guard.ui.dashboard

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.PhoneCheckRequest
import eu.cyben.guard.databinding.ActivityPhoneCheckBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhoneCheckActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityPhoneCheckBinding

    private val categories = listOf("Spam", "Truffa", "Phishing", "Telemarketing", "Altro")
    private var selectedCategory = "Spam"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener { finish() }
        setupChips()

        binding.btnCheckPhone.setOnClickListener {
            val number = binding.etPhoneNumber.text.toString().trim()
            if (number.isEmpty()) {
                binding.etPhoneNumber.error = "Inserisci un numero"
                return@setOnClickListener
            }
            checkNumber(number)
        }

        binding.btnSendReport.setOnClickListener {
            val number = binding.etPhoneNumber.text.toString().trim()
            val desc = binding.etReportDesc.text.toString().trim()
            if (number.isEmpty()) {
                binding.etPhoneNumber.error = "Inserisci un numero da segnalare"
                return@setOnClickListener
            }
            sendReport(number, desc)
        }
    }

    private fun setupChips() {
        binding.chipGroup.removeAllViews()
        categories.forEach { cat ->
            val chip = TextView(this).apply {
                text = cat
                textSize = 13f
                setPadding(32, 14, 32, 14)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 10
                layoutParams = lp
                setOnClickListener { selectedCategory = cat; setupChips() }
            }
            if (cat == selectedCategory) {
                chip.setTextColor(Color.WHITE)
                chip.setTypeface(null, Typeface.BOLD)
                chip.setBackgroundResource(eu.cyben.guard.R.drawable.chip_selected_bg)
            } else {
                chip.setTextColor(Color.parseColor("#8899AA"))
                chip.setBackgroundResource(eu.cyben.guard.R.drawable.chip_bg)
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun checkNumber(number: String) {
        binding.progressPhone.visibility = View.VISIBLE
        binding.tvCheckLabel.text = "Controllo..."
        binding.btnCheckPhone.isClickable = false
        lifecycleScope.launch {
            try {
                val resp = api.checkPhone(PhoneCheckRequest(number))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val score = body?.score ?: 0
                    val isScam = body?.isScam == true
                    val (label, colorHex) = when {
                        score >= 70 || isScam -> Pair("NUMERO PERICOLOSO", "#FF3B30")
                        score >= 40 -> Pair("SOSPETTO", "#FF9500")
                        else -> Pair("NUMERO SICURO", "#34C759")
                    }
                    binding.cardResult.visibility = View.VISIBLE
                    binding.tvResultScore.text = "$score"
                    binding.tvResultScore.setTextColor(Color.parseColor(colorHex))
                    binding.tvResultLabel.text = label
                    binding.tvResultLabel.setTextColor(Color.parseColor(colorHex))
                    binding.tvResultNumber.text = number
                    binding.tvResultExplanation.text = body?.explanation ?: "Nessun dettaglio disponibile"
                } else {
                    Toast.makeText(this@PhoneCheckActivity, "Errore verifica numero", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@PhoneCheckActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressPhone.visibility = View.GONE
                binding.tvCheckLabel.text = "Controlla"
                binding.btnCheckPhone.isClickable = true
            }
        }
    }

    private fun sendReport(number: String, description: String) {
        lifecycleScope.launch {
            try {
                api.requestHuman(eu.cyben.guard.data.api.HumanRequest(
                    text = "Segnalazione numero: $number | Categoria: $selectedCategory | Descrizione: $description",
                    analysisId = null
                ))
                Toast.makeText(this@PhoneCheckActivity, "Segnalazione inviata, grazie!", Toast.LENGTH_SHORT).show()
                binding.etReportDesc.setText("")
            } catch (_: Exception) {
                Toast.makeText(this@PhoneCheckActivity, "Errore invio segnalazione", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
