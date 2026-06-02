package eu.cyben.guard.ui.prohmed

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.models.ProhmedActivateRequest
import eu.cyben.guard.data.models.ProhmedConsultRequest
import eu.cyben.guard.databinding.ActivityProhmedBinding
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ProhmedActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityProhmedBinding
    private var selectedBirthDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProhmedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        val specialties = arrayOf("Medico di base", "Cardiologo", "Dermatologo", "Ginecologo", "Ortopedico", "Pediatra", "Psicologo", "Altro")
        binding.spinnerSpecialty.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, specialties)
        val urgencies = arrayOf("Bassa", "Media", "Alta")
        binding.spinnerUrgency.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, urgencies)

        binding.etBirthDate.isFocusable = false
        binding.etBirthDate.isClickable = true
        binding.etBirthDate.hint = "Seleziona data di nascita"
        binding.etBirthDate.setOnClickListener { showDatePicker() }

        binding.btnActivate.setOnClickListener { activateProhmed() }
        binding.btnConsult.setOnClickListener { requestConsult() }
        loadStatus()
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().also { it.add(Calendar.YEAR, -30) }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedBirthDate = "%02d-%02d-%04d".format(day, month + 1, year)
                binding.etBirthDate.setText("%02d/%02d/%04d".format(day, month + 1, year))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).also { it.datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    private fun loadStatus() {
        lifecycleScope.launch {
            try {
                val resp = api.getProhmedStatus()
                if (resp.isSuccessful) {
                    val status = resp.body() ?: return@launch
                    if (status.activated) {
                        binding.layoutActivate.visibility = View.GONE
                        binding.layoutConsult.visibility = View.VISIBLE
                        binding.tvProhmedTitle.text = "Prohmed attivo"
                        binding.tvProhmedSubtitle.text = status.email ?: ""
                    } else {
                        binding.layoutActivate.visibility = View.VISIBLE
                        binding.layoutConsult.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun activateProhmed() {
        val name = binding.etName.text.toString().trim()
        val cf = binding.etFiscalCode.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        if (name.isEmpty() || cf.isEmpty() || selectedBirthDate.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show(); return
        }
        binding.progressProhmed.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.activateProhmed(ProhmedActivateRequest(name, cf, selectedBirthDate, phone))
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    Toast.makeText(this@ProhmedActivity, "Prohmed attivato con successo", Toast.LENGTH_LONG).show()
                    loadStatus()
                } else {
                    Toast.makeText(this@ProhmedActivity, resp.body()?.error ?: "Errore attivazione", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ProhmedActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressProhmed.visibility = View.GONE
            }
        }
    }

    private fun requestConsult() {
        val desc = binding.etConsultDesc.text.toString().trim()
        if (desc.isEmpty()) { Toast.makeText(this, "Descrivi il tuo problema", Toast.LENGTH_SHORT).show(); return }
        val specialty = binding.spinnerSpecialty.selectedItem.toString()
        val urgency = when (binding.spinnerUrgency.selectedItemPosition) { 2 -> "high"; 1 -> "medium"; else -> "low" }
        binding.progressProhmed.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.consultProhmed(ProhmedConsultRequest(specialty, desc, urgency))
                if (resp.isSuccessful) {
                    Toast.makeText(this@ProhmedActivity, "Consulto richiesto con successo", Toast.LENGTH_LONG).show()
                    binding.etConsultDesc.setText("")
                } else {
                    Toast.makeText(this@ProhmedActivity, "Errore richiesta consulto", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ProhmedActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressProhmed.visibility = View.GONE
            }
        }
    }
}
