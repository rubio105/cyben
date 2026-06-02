package eu.cyben.guard.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.AnalyzeRequest
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.models.ChatMessage
import eu.cyben.guard.databinding.ActivityVoiceBinding
import eu.cyben.guard.ui.breach.BreachMonitorActivity
import eu.cyben.guard.ui.dashboard.ChatAdapter
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.ui.settings.SettingsActivity
import eu.cyben.guard.ui.vpn.VPNActivity
import eu.cyben.guard.utils.LocaleHelper
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val SESSION_DURATION_MS = 120_000L

private const val CYAGENT_SYSTEM_PROMPT = """Sei CYAGENT, assistente anti-vishing. Analizza questa possibile chiamata sospetta.

Istruzioni:
- valuta il rischio in modo pragmatico;
- considera sempre anche l'ipotesi che la conversazione sia autentica o innocua;
- non classificare come truffa senza segnali concreti di manipolazione, urgenza, pressione o richiesta anomala di dati;
- se gli elementi sono insufficienti, dillo chiaramente e mantieni una valutazione prudente;
- se emergono segnali tipici di truffa, dillo chiaramente;
- proponi domande precise che l'utente dovrebbe fare subito al chiamante;
- indica cosa non deve condividere;
- chiudi con un summarize operativo breve per dashboard e storico.

Sei CYAGENT in modalita' live durante una chiamata.
Valuta il rischio in modo prudente.
Considera sempre anche la possibilita' che la conversazione sia autentica.
Non indicare truffa se non emergono segnali concreti.
Se non hai abbastanza elementi, mantieni rischio basso o medio e chiedi verifiche mirate."""

@AndroidEntryPoint
class VoiceActivity : AppCompatActivity() {

    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityVoiceBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val history = mutableListOf<Map<String, String>>()
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isSessionActive = false
    private var callSessionId: String? = null
    private var lastRiskLevel: String? = null
    private var lastExplanation: String? = null
    private var lastRiskScore: Int = 0
    private var sessionTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(messages)
        binding.rvCyAgentChat.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        binding.rvCyAgentChat.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAvvia.setOnClickListener {
            if (isSessionActive) stopSession() else startSession()
        }
        binding.btnAnalizza.setOnClickListener { analyzeNow() }

        setupNav()
    }

    private fun startSession() {
        callSessionId = UUID.randomUUID().toString()
        isSessionActive = true

        binding.cardInfo.visibility = View.GONE
        binding.cardRisk.visibility = View.VISIBLE
        binding.scrollChips.visibility = View.VISIBLE
        binding.tvSessionStatus.visibility = View.VISIBLE
        binding.tvSessionStatus.text = "Sessione attiva"
        binding.tvSessionStatus.setTextColor(Color.parseColor("#FF6600"))

        binding.tvAvviaIcon.text = "⏹"
        binding.tvAvviaLabel.text = "Stop ascolto"
        binding.btnAvvia.setBackgroundResource(eu.cyben.guard.R.drawable.btn_red)

        binding.tvRiskScore.text = "--"
        binding.tvRiskLabel.text = "IN ASCOLTO"
        binding.tvRiskLabel.setTextColor(Color.parseColor("#66BB6A"))
        binding.tvRiskDesc.text = "Analisi in corso..."

        binding.tvTranscript.text = "In ascolto... metti la chiamata in vivavoce."
        binding.tvTranscript.setTextColor(Color.parseColor("#66BB6A"))

        startTimer()
        startListeningLoop()
    }

    private fun stopSession() {
        isSessionActive = false
        sessionTimer?.cancel()
        speechRecognizer?.stopListening()
        isListening = false

        binding.tvSessionStatus.text = "Sessione terminata"
        binding.tvSessionStatus.setTextColor(Color.parseColor("#66BB6A"))
        binding.tvAvviaIcon.text = "🎙"
        binding.tvAvviaLabel.text = "Start ascolto"
        binding.btnAvvia.setBackgroundResource(eu.cyben.guard.R.drawable.btn_blue)
        binding.tvTimer.visibility = View.GONE
        binding.tvTranscript.text = "Premi Start ascolto, metti la chiamata in vivavoce e lascia parlare il chiamante."
        binding.tvTranscript.setTextColor(resources.getColor(eu.cyben.guard.R.color.text_secondary, null))

        showReport()
    }

    private fun startTimer() {
        binding.tvTimer.visibility = View.VISIBLE
        sessionTimer?.cancel()
        sessionTimer = object : CountDownTimer(SESSION_DURATION_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                binding.tvTimer.text = "%02d:%02d".format(sec / 60, sec % 60)
            }
            override fun onFinish() {
                binding.tvTimer.text = "00:00"
                if (isSessionActive) stopSession()
            }
        }.start()
    }

    private fun analyzeNow() {
        val context = binding.etContext.text.toString().trim()
        val text = if (context.isNotEmpty()) context else "(Analisi manuale richiesta)"
        binding.progressAnalisi.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val resp = api.analyzeLive(AnalyzeRequest(
                    text = text, type = "call", chatHistory = history.toList(),
                    callSessionId = callSessionId, isLive = true, mode = "live",
                    systemPrompt = CYAGENT_SYSTEM_PROMPT
                ))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val reply = body?.messaggio_live ?: body?.motivo_breve ?: "Analisi completata"
                    history.add(mapOf("role" to "user", "content" to text))
                    history.add(mapOf("role" to "assistant", "content" to reply))
                    addBotMessage(reply)
                    val riskLevel = when (body?.rischio_attuale) {
                        "alto" -> "dangerous"
                        "medio" -> "suspicious"
                        "basso" -> "safe"
                        else -> null
                    }
                    updateRiskFromResponse(riskLevel, null, body?.motivo_breve)
                    body?.domande_subito?.let { showChips(it) }
                } else {
                    Toast.makeText(this@VoiceActivity, "Errore analisi", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@VoiceActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressAnalisi.visibility = View.GONE
            }
        }
    }

    private fun startListeningLoop() {
        if (!isSessionActive) return
        startListeningOnce()
    }

    private fun startListeningOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                isListening = false
                if (!text.isNullOrBlank()) {
                    binding.tvTranscript.text = text
                    binding.tvTranscript.setTextColor(Color.parseColor("#B0B8D0"))
                    sendTranscript(text)
                } else if (isSessionActive) {
                    handler.postDelayed({ startListeningLoop() }, 800)
                }
            }
            override fun onError(error: Int) {
                isListening = false
                if (isSessionActive && error != SpeechRecognizer.ERROR_CLIENT) {
                    handler.postDelayed({ startListeningLoop() }, 1500)
                }
            }
            override fun onPartialResults(p: android.os.Bundle?) {
                val partial = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    binding.tvTranscript.text = partial
                    binding.tvTranscript.setTextColor(Color.parseColor("#66BB6A"))
                }
            }
            override fun onReadyForSpeech(p: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, p: android.os.Bundle?) {}
        })
        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleHelper.ttsLocale.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        })
        isListening = true
    }

    private fun sendTranscript(text: String) {
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvCyAgentChat.scrollToPosition(messages.size - 1)

        lifecycleScope.launch {
            try {
                val resp = api.analyzeLive(AnalyzeRequest(
                    text = text, type = "call", chatHistory = history.toList(),
                    callSessionId = callSessionId, isLive = true, mode = "live",
                    systemPrompt = CYAGENT_SYSTEM_PROMPT
                ))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val reply = body?.messaggio_live ?: body?.motivo_breve ?: "Ok"
                    history.add(mapOf("role" to "user", "content" to text))
                    history.add(mapOf("role" to "assistant", "content" to reply))
                    addBotMessage(reply)
                    val riskLevel = when (body?.rischio_attuale) {
                        "alto" -> "dangerous"
                        "medio" -> "suspicious"
                        "basso" -> "safe"
                        else -> null
                    }
                    updateRiskFromResponse(riskLevel, null, body?.motivo_breve)
                    body?.domande_subito?.let { showChips(it) }
                }
            } catch (_: Exception) { }
            if (isSessionActive) handler.postDelayed({ startListeningLoop() }, 800)
        }
    }

    private fun updateRiskFromResponse(riskLevel: String?, score: Int?, explanation: String?) {
        lastRiskLevel = riskLevel
        if (explanation != null) lastExplanation = explanation
        if (score != null) lastRiskScore = score

        val (label, color, desc) = when (riskLevel) {
            "dangerous" -> Triple("ALTO", "#D32F2F", explanation ?: "Rischio elevato rilevato")
            "suspicious" -> Triple("MEDIO", "#FF6F00", explanation ?: "Elementi sospetti rilevati")
            "safe" -> Triple("BASSO", "#388E3C", explanation ?: "Nessun rischio evidente")
            else -> Triple("IN ASCOLTO", "#66BB6A", "Analisi in corso...")
        }
        binding.tvRiskScore.text = if (score != null && score > 0) "$score" else "--"
        binding.tvRiskLabel.text = label
        binding.tvRiskLabel.setTextColor(Color.parseColor(color))
        binding.tvRiskDesc.text = desc
    }

    private fun showChips(questions: List<String>) {
        if (questions.isEmpty()) return
        binding.chipContainer.removeAllViews()
        questions.take(4).forEach { q ->
            val chip = TextView(this).apply {
                text = q; textSize = 12f
                setTextColor(Color.parseColor("#B0B8D0"))
                setBackgroundResource(eu.cyben.guard.R.drawable.chip_bg)
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 12, 0); layoutParams = lp
                setPadding(24, 12, 24, 12)
                setOnClickListener { sendTranscript(q) }
            }
            binding.chipContainer.addView(chip)
        }
    }

    private fun showReport() {
        val riskEmoji = when (lastRiskLevel) {
            "dangerous" -> "🔴 ALTO RISCHIO"
            "suspicious" -> "🟠 RISCHIO MEDIO"
            "safe" -> "🟢 BASSO RISCHIO"
            else -> "⚪ Nessun dato"
        }
        val msgCount = messages.count { it.isUser }
        val summary = lastExplanation ?: "Nessuna analisi effettuata durante la sessione."

        AlertDialog.Builder(this)
            .setTitle("Report sessione")
            .setMessage("$riskEmoji\n\n$summary\n\nMessaggi inviati: $msgCount")
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()

        addBotMessage("Sessione terminata. Non fornire mai dati personali, codici OTP o coordinate bancarie per telefono.")
        binding.cardRisk.visibility = View.GONE
        binding.scrollChips.visibility = View.GONE
        binding.chipContainer.removeAllViews()
        binding.cardInfo.visibility = View.VISIBLE
    }

    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text, false))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvCyAgentChat.scrollToPosition(messages.size - 1)
    }

    private fun setupNav() {
        binding.tabAnalizza.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabViolazioni.setOnClickListener {
            startActivity(Intent(this, BreachMonitorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabVPN.setOnClickListener {
            startActivity(Intent(this, VPNActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabImpostazioni.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sessionTimer?.cancel()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
