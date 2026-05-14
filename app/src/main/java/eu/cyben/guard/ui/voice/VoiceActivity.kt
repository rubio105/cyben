package eu.cyben.guard.ui.voice

import android.Manifest
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import eu.cyben.guard.ui.analysis.AnalysisHistoryActivity
import eu.cyben.guard.ui.breach.BreachMonitorActivity
import eu.cyben.guard.ui.dashboard.ChatAdapter
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.ui.settings.SettingsActivity
import eu.cyben.guard.ui.vpn.VPNActivity
import eu.cyben.guard.utils.LocaleHelper
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class VoiceActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityVoiceBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val history = mutableListOf<Map<String, String>>()
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var callSessionId: String? = null
    private var isCallSession = false
    private val handler = Handler(Looper.getMainLooper())

    private val suggestedQuestions = listOf(
        "E' una truffa?", "Chi mi ha chiamato?", "E' sicuro questo link?",
        "Come mi proteggo?", "Cosa devo fare?", "E' una truffa bancaria?",
        "Verifica questo numero", "Cosa e' il phishing?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(messages)
        binding.rvCyAgentChat.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        binding.rvCyAgentChat.adapter = adapter

        addBotMessage("Ciao! Sono CyAgent, il tuo assistente AI per la sicurezza. Parla o scrivi per ricevere analisi su truffe, chiamate sospette e contenuti pericolosi.")

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = LocaleHelper.ttsLocale
        }

        setupChips()
        binding.btnBack.setOnClickListener { finish() }

        binding.btnVoiceMic.setOnClickListener { onMicTap() }
        binding.btnVoiceMic.setOnLongClickListener { onMicLongPress(); true }

        binding.btnVoiceSend.setOnClickListener {
            val text = binding.etVoiceText.text.toString().trim()
            if (text.isNotEmpty()) { binding.etVoiceText.setText(""); sendMessage(text) }
        }
        binding.etVoiceText.setOnEditorActionListener { _, _, _ ->
            binding.btnVoiceSend.performClick(); true
        }

        binding.btnEndSession.setOnClickListener { endCallSession() }
        setupNav()
    }

    private fun setupChips() {
        suggestedQuestions.forEach { q ->
            val chip = TextView(this).apply {
                text = q
                textSize = 12f
                setTextColor(Color.parseColor("#B0B8D0"))
                setTypeface(null, Typeface.NORMAL)
                setBackgroundResource(eu.cyben.guard.R.drawable.chip_bg)
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 16, 0)
                layoutParams = lp
                setPadding(24, 12, 24, 12)
                setOnClickListener { sendMessage(q) }
            }
            binding.chipContainer.addView(chip)
        }
    }

    private fun onMicTap() {
        if (isListening) stopListening()
        else startListening()
    }

    private fun onMicLongPress() {
        if (!isCallSession) startCallSession()
        else endCallSession()
    }

    private fun startCallSession() {
        callSessionId = UUID.randomUUID().toString()
        isCallSession = true
        binding.btnEndSession.visibility = View.VISIBLE
        binding.tvRiskBadge.visibility = View.VISIBLE
        binding.tvSessionStatus.text = "Sessione attiva"
        binding.tvSessionStatus.setTextColor(Color.parseColor("#FF6600"))
        binding.tvSessionState.text = "Sessione in corso - Premi per parlare"
        animateRings(true)
        addBotMessage("Sessione avviata. Parlerò con te in tempo reale durante la chiamata. Raccontami cosa sta succedendo.")
    }

    private fun endCallSession() {
        isCallSession = false
        callSessionId = null
        binding.btnEndSession.visibility = View.GONE
        binding.tvRiskBadge.visibility = View.GONE
        binding.tvSessionStatus.text = "Pronto"
        binding.tvSessionStatus.setTextColor(Color.parseColor("#66BB6A"))
        binding.tvSessionState.text = "Premi per parlare"
        animateRings(false)
        addBotMessage("Sessione terminata. Ricorda: non fornire mai dati personali, codici OTP o coordinate bancarie per telefono.")
    }

    private fun startListening() {
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
                updateMicState()
                if (!text.isNullOrBlank()) sendMessage(text)
                else if (isCallSession) handler.postDelayed({ startListening() }, 800)
            }
            override fun onError(error: Int) {
                isListening = false
                updateMicState()
                if (isCallSession && error != SpeechRecognizer.ERROR_CLIENT) {
                    handler.postDelayed({ startListening() }, 1500)
                }
            }
            override fun onReadyForSpeech(p: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: android.os.Bundle?) {}
            override fun onEvent(t: Int, p: android.os.Bundle?) {}
        })
        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleHelper.ttsLocale.toString())
            if (isCallSession) putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        })
        isListening = true
        updateMicState()
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        updateMicState()
    }

    private fun updateMicState() {
        if (isListening) {
            binding.tvMicIcon.text = "⏹"
            binding.tvSessionState.text = if (isCallSession) "In ascolto..." else "In ascolto... (premi per fermare)"
            binding.tvSessionStatus.text = "In ascolto"
            binding.tvSessionStatus.setTextColor(Color.parseColor("#FF6600"))
        } else {
            binding.tvMicIcon.text = "🎙"
            binding.tvSessionState.text = if (isCallSession) "Sessione attiva - Premi per parlare" else "Premi per parlare"
            if (!isCallSession) {
                binding.tvSessionStatus.text = "Pronto"
                binding.tvSessionStatus.setTextColor(Color.parseColor("#66BB6A"))
            }
        }
    }

    private fun sendMessage(text: String) {
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvCyAgentChat.scrollToPosition(messages.size - 1)
        binding.progressCyAgent.visibility = View.VISIBLE

        val type = if (isCallSession) "call" else "text"
        val sessionId = if (isCallSession) callSessionId else null

        lifecycleScope.launch {
            try {
                val resp = api.analyze(AnalyzeRequest(text, type, history.toList(), callSessionId = sessionId, isLive = if (isCallSession) true else null))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val reply = body?.conversationalMessage ?: body?.analysis?.explanation ?: "Analisi completata"
                    history.add(mapOf("role" to "user", "content" to text))
                    history.add(mapOf("role" to "assistant", "content" to reply))
                    addBotMessage(reply)
                    if (isCallSession) updateRiskBadge(body?.analysis?.riskLevel)
                    tts?.speak(reply.take(200), TextToSpeech.QUEUE_FLUSH, null, null)
                    if (isCallSession) handler.postDelayed({ startListening() }, 600)
                } else {
                    Toast.makeText(this@VoiceActivity, "Errore analisi", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@VoiceActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressCyAgent.visibility = View.GONE
            }
        }
    }

    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text, false))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvCyAgentChat.scrollToPosition(messages.size - 1)
    }

    private fun updateRiskBadge(riskLevel: String?) {
        val (label, color) = when (riskLevel) {
            "dangerous" -> Pair("CRITICO", "#D32F2F")
            "suspicious" -> Pair("MEDIO", "#FF6F00")
            "safe" -> Pair("BASSO", "#388E3C")
            else -> Pair("SCONOSCIUTO", "#607D8B")
        }
        binding.tvRiskLabel.text = label
        binding.tvRiskLabel.setTextColor(Color.parseColor(color))
    }

    private fun animateRings(start: Boolean) {
        listOf(binding.ringOuter, binding.ringInner).forEachIndexed { i, v ->
            if (start) {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.35f + i * 0.1f, 1f).apply {
                    duration = 1200L + i * 300; repeatCount = ObjectAnimator.INFINITE
                }
                val sy = ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.35f + i * 0.1f, 1f).apply {
                    duration = 1200L + i * 300; repeatCount = ObjectAnimator.INFINITE
                }
                val sa = ObjectAnimator.ofFloat(v, "alpha", 0.6f, 0.2f, 0.6f).apply {
                    duration = 1200L + i * 300; repeatCount = ObjectAnimator.INFINITE
                }
                sx.start(); sy.start(); sa.start()
                v.tag = listOf(sx, sy, sa)
            } else {
                @Suppress("UNCHECKED_CAST")
                (v.tag as? List<ObjectAnimator>)?.forEach { it.cancel() }
                v.scaleX = 1f; v.scaleY = 1f; v.alpha = 0.5f
            }
        }
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
        binding.tabStorico.setOnClickListener {
            startActivity(Intent(this, AnalysisHistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabImpostazioni.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
