package eu.cyben.guard.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

    private val suggestions = listOf(
        "Ho ricevuto un SMS sospetto",
        "Verifica questo link",
        "E' una truffa?",
        "Analizza questa email"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ChatAdapter(messages)
        binding.rvVoiceChat.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        binding.rvVoiceChat.adapter = adapter
        messages.add(ChatMessage("Ciao! Sono CyAgent. Parla con me o scrivi un testo sospetto. Analizzo SMS, email, link e ti dico se e' una truffa con livello di rischio dettagliato.", false))
        adapter.notifyItemInserted(0)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = LocaleHelper.ttsLocale }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnVoiceMic.setOnClickListener { if (isListening) stopListening() else startListening() }
        binding.btnVoiceSend.setOnClickListener {
            val text = binding.etVoiceText.text.toString().trim()
            if (text.isNotEmpty()) { binding.etVoiceText.setText(""); sendMessage(text) }
        }
        setupSuggestions()
        setupNav()
    }

    private fun setupSuggestions() {
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp16 = dp8 * 2
        val dp12 = (12 * resources.displayMetrics.density).toInt()
        suggestions.forEach { suggestion ->
            val chip = TextView(this).apply {
                text = suggestion
                textSize = 12f
                setTextColor(0xFFFF6600.toInt())
                setPadding(dp16, dp8, dp16, dp8)
                background = android.graphics.drawable.GradientDrawable().also { d ->
                    d.cornerRadius = 20 * resources.displayMetrics.density
                    d.setStroke((1 * resources.displayMetrics.density).toInt(), 0xFFFF6600.toInt())
                    d.setColor(0x15FF6600.toInt())
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = dp12
                layoutParams = lp
                isClickable = true; isFocusable = true
                setOnClickListener { sendMessage(suggestion) }
            }
            binding.layoutSuggestions.addView(chip)
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101); return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                isListening = false; updateMicState(); sendMessage(text)
            }
            override fun onError(error: Int) { isListening = false; updateMicState() }
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleHelper.ttsLocale.toString())
        })
        isListening = true; updateMicState()
        Toast.makeText(this, "Sto ascoltando...", Toast.LENGTH_SHORT).show()
    }

    private fun stopListening() { speechRecognizer?.stopListening(); isListening = false; updateMicState() }

    private fun updateMicState() {
        if (isListening) {
            binding.tvMicIcon.text = "⏹"
            binding.tvListeningLabel.text = "Sto ascoltando..."
            binding.tvVoiceStatus.text = "In ascolto"
            binding.tvVoiceStatus.setTextColor(0xFFFF6600.toInt())
        } else {
            binding.tvMicIcon.text = "🎤"
            binding.tvListeningLabel.text = "Premi per parlare"
            binding.tvVoiceStatus.text = "Pronto"
            binding.tvVoiceStatus.setTextColor(0xFF66BB6A.toInt())
        }
    }

    private fun sendMessage(text: String) {
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvVoiceChat.scrollToPosition(messages.size - 1)
        binding.progressVoice.visibility = View.VISIBLE
        binding.layoutSuggestions.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val resp = api.analyze(AnalyzeRequest(text, "text", history.toList()))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val analysis = body?.analysis
                    val reply = body?.conversationalMessage ?: analysis?.explanation ?: "Analisi completata"
                    history.add(mapOf("role" to "user", "content" to text))
                    history.add(mapOf("role" to "assistant", "content" to reply))
                    messages.add(ChatMessage(reply, false))
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.rvVoiceChat.scrollToPosition(messages.size - 1)
                    analysis?.let { showRiskCard(it.riskLevel ?: "safe", it.riskScore ?: 0, it.recommendation) }
                    tts?.speak(reply.take(200), TextToSpeech.QUEUE_FLUSH, null, null)
                } else Toast.makeText(this@VoiceActivity, "Errore analisi", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@VoiceActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
            finally { binding.progressVoice.visibility = View.GONE }
        }
    }

    private fun showRiskCard(riskLevel: String, riskScore: Int, recommendation: String?) {
        val (bgColor, textColor, label, icon) = when (riskLevel) {
            "dangerous" -> listOf(0x33FF1744, 0xFFFF1744, "PERICOLOSO", "!")
            "suspicious" -> listOf(0x33FF9800, 0xFFFF9800, "SOSPETTO", "?")
            else         -> listOf(0x3366BB6A, 0xFF66BB6A, "SICURO", "✓")
        }
        binding.cardRisk.visibility = View.VISIBLE
        binding.tvRiskIcon.text = icon as String
        binding.tvRiskIcon.setTextColor(textColor as Int)
        binding.tvRiskLabel.text = label as String
        binding.tvRiskLabel.setTextColor(textColor)
        binding.tvRiskScore.text = "$riskScore%"
        binding.tvRiskScore.setTextColor(textColor)
        binding.cardRisk.setBackgroundColor(bgColor as Int)
        if (!recommendation.isNullOrEmpty()) {
            binding.tvRiskRecommendation.visibility = View.VISIBLE
            binding.tvRiskRecommendation.text = recommendation
        } else {
            binding.tvRiskRecommendation.visibility = View.GONE
        }
    }

    private fun setupNav() {
        binding.tabAnalizza.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        binding.tabViolazioni.setOnClickListener { startActivity(Intent(this, BreachMonitorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        binding.tabVPN.setOnClickListener { startActivity(Intent(this, VPNActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        binding.tabImpostazioni.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
    }

    override fun onDestroy() { tts?.shutdown(); speechRecognizer?.destroy(); super.onDestroy() }
}