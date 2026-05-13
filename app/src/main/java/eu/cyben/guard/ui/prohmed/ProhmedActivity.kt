package eu.cyben.guard.ui.prohmed

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.models.ProhmedActivateRequest
import eu.cyben.guard.data.models.ProhmedConsult
import eu.cyben.guard.data.models.ProhmedConsultRequest
import eu.cyben.guard.data.models.ProhmedStatus
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProhmedActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService

    private lateinit var scroll: ScrollView
    private lateinit var container: LinearLayout
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF121212.toInt())
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(24, 48, 24, 24)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val btnBack = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            setBackgroundColor(0)
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { finish() }
        }
        val tvTitle = TextView(this).apply {
            text = "Prohmed Health"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(16, 0, 0, 0)
        }
        header.addView(btnBack)
        header.addView(tvTitle)
        root.addView(header)

        progress = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.gravity = android.view.Gravity.CENTER_HORIZONTAL
                it.topMargin = 32
            }
        }
        root.addView(progress)

        scroll = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 32)
        }
        scroll.addView(container)
        root.addView(scroll)

        setContentView(root)
        loadStatus()
    }

    private fun loadStatus() {
        progress.visibility = View.VISIBLE
        container.removeAllViews()
        lifecycleScope.launch {
            try {
                val resp = api.getProhmedStatus()
                progress.visibility = View.GONE
                if (resp.isSuccessful) {
                    val status = resp.body()
                    if (status != null) {
                        if (status.activated) showActivatedUI(status)
                        else showActivationUI(status)
                    }
                } else {
                    showError("Errore caricamento stato Prohmed")
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                showError("Errore di rete: ${e.message}")
            }
        }
    }

    private fun showActivationUI(status: ProhmedStatus) {
        addTitle("Attiva Prohmed Health")
        addText("Con il tuo Piano Premium Annuale hai accesso alla piattaforma sanitaria digitale Prohmed.")
        addText("")
        addBullet("Consulti medici online con specialisti")
        addBullet("Cartella clinica digitale sicura")
        addBullet("Prenotazione visite ed esami")
        addBullet("Referti digitali sempre disponibili")
        addText("")
        if (status.eligible) {
            addButton("Attiva il Servizio", 0xFF4CAF50.toInt()) { showActivationDialog() }
        } else {
            addText("Non sei ancora idoneo per attivare Prohmed. Contatta il supporto.")
        }
    }

    private fun showActivatedUI(status: ProhmedStatus) {
        addTitle("Prohmed Health")
        addTextColored("Servizio attivo", 0xFF4CAF50.toInt())
        status.remainingConsults?.let { addText("Consulti residui: $it") }
        addText("")
        addButton("Richiedi Consulto", 0xFFFF6D00.toInt()) { showConsultDialog() }
        addButton("I Miei Consulti", 0xFF1565C0.toInt()) { loadConsults() }
    }

    private fun showActivationDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etName = EditText(this).apply { hint = "Nome e Cognome" }
        val etFiscal = EditText(this).apply { hint = "Codice Fiscale" }
        val etBirth = EditText(this).apply { hint = "Data di nascita (YYYY-MM-DD)" }
        val etPhone = EditText(this).apply { hint = "Telefono" }
        layout.addView(etName)
        layout.addView(etFiscal)
        layout.addView(etBirth)
        layout.addView(etPhone)

        AlertDialog.Builder(this)
            .setTitle("Attiva Prohmed")
            .setView(layout)
            .setPositiveButton("Attiva") { _, _ ->
                val req = ProhmedActivateRequest(
                    name = etName.text.toString().trim(),
                    fiscalCode = etFiscal.text.toString().trim(),
                    birthDate = etBirth.text.toString().trim(),
                    phone = etPhone.text.toString().trim()
                )
                if (req.name.isEmpty() || req.fiscalCode.isEmpty()) {
                    Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                activate(req)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun activate(req: ProhmedActivateRequest) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.activateProhmed(req)
                progress.visibility = View.GONE
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    Toast.makeText(this@ProhmedActivity, "Prohmed attivato con successo!", Toast.LENGTH_LONG).show()
                    loadStatus()
                } else {
                    Toast.makeText(this@ProhmedActivity, resp.body()?.error ?: "Errore attivazione", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@ProhmedActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showConsultDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val spinnerSpecialty = Spinner(this).apply {
            adapter = ArrayAdapter(this@ProhmedActivity, android.R.layout.simple_spinner_item,
                listOf("Medico di base", "Cardiologo", "Dermatologo", "Ortopedico", "Neurologo", "Psicologo", "Altro")
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        val etDesc = EditText(this).apply { hint = "Descrivi il problema..."; minLines = 3 }
        val spinnerUrgency = Spinner(this).apply {
            adapter = ArrayAdapter(this@ProhmedActivity, android.R.layout.simple_spinner_item,
                listOf("Normale", "Urgente")
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        layout.addView(TextView(this).apply { text = "Specialita"; setTextColor(0xFFBDBDBD.toInt()) })
        layout.addView(spinnerSpecialty)
        layout.addView(TextView(this).apply { text = "Descrizione"; setTextColor(0xFFBDBDBD.toInt()); setPadding(0, 16, 0, 0) })
        layout.addView(etDesc)
        layout.addView(TextView(this).apply { text = "Urgenza"; setTextColor(0xFFBDBDBD.toInt()); setPadding(0, 16, 0, 0) })
        layout.addView(spinnerUrgency)

        AlertDialog.Builder(this)
            .setTitle("Richiedi Consulto")
            .setView(layout)
            .setPositiveButton("Invia") { _, _ ->
                val desc = etDesc.text.toString().trim()
                if (desc.isEmpty()) { Toast.makeText(this, "Descrivi il problema", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val urgency = if (spinnerUrgency.selectedItemPosition == 1) "urgent" else "normal"
                sendConsult(ProhmedConsultRequest(spinnerSpecialty.selectedItem.toString(), desc, urgency))
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun sendConsult(req: ProhmedConsultRequest) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.sendConsult(req)
                progress.visibility = View.GONE
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    Toast.makeText(this@ProhmedActivity, "Consulto inviato! Un medico ti risponderà presto.", Toast.LENGTH_LONG).show()
                    loadStatus()
                } else {
                    Toast.makeText(this@ProhmedActivity, resp.body()?.error ?: "Errore", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@ProhmedActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadConsults() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getConsults()
                progress.visibility = View.GONE
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    showConsultsList(list)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@ProhmedActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showConsultsList(list: List<ProhmedConsult>) {
        container.removeAllViews()
        addTitle("I Miei Consulti")
        addButton("Torna", 0xFF424242.toInt()) { loadStatus() }
        addText("")
        if (list.isEmpty()) { addText("Nessun consulto ancora."); return }
        list.forEach { consult ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF1E1E1E.toInt())
                setPadding(24, 20, 24, 20)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = 16
                layoutParams = lp
            }
            card.addView(TextView(this).apply {
                text = consult.specialty ?: "Consulto"
                textSize = 15f
                setTextColor(0xFFFFFFFF.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            card.addView(TextView(this).apply {
                text = consult.description ?: ""
                textSize = 13f
                setTextColor(0xFFBDBDBD.toInt())
            })
            val statusColor = when (consult.status) {
                "completed" -> 0xFF4CAF50.toInt()
                "pending" -> 0xFFFF9800.toInt()
                else -> 0xFF9E9E9E.toInt()
            }
            card.addView(TextView(this).apply {
                text = consult.status ?: ""
                textSize = 12f
                setTextColor(statusColor)
            })
            consult.response?.let { resp ->
                card.addView(TextView(this).apply {
                    text = "Risposta: $resp"
                    textSize = 13f
                    setTextColor(0xFF81C784.toInt())
                    setPadding(0, 8, 0, 0)
                })
            }
            container.addView(card)
        }
    }

    private fun showError(msg: String) {
        container.removeAllViews()
        addText(msg)
    }

    private fun addTitle(text: String) = container.addView(TextView(this).apply {
        this.text = text; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = 16; layoutParams = lp
    })
    private fun addText(text: String) = container.addView(TextView(this).apply {
        this.text = text; textSize = 14f; setTextColor(0xFFBDBDBD.toInt())
    })
    private fun addTextColored(text: String, color: Int) = container.addView(TextView(this).apply {
        this.text = text; textSize = 14f; setTextColor(color)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = 8; layoutParams = lp
    })
    private fun addBullet(text: String) = addText("  • $text")
    private fun addButton(label: String, color: Int, action: () -> Unit) = container.addView(Button(this).apply {
        this.text = label; setBackgroundColor(color); setTextColor(0xFFFFFFFF.toInt())
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 140)
        lp.topMargin = 12; layoutParams = lp
        setOnClickListener { action() }
    })
}
