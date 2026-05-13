package eu.cyben.guard.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
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
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.ui.dashboard.ChatAdapter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ChatAdapter(messages)
        binding.rvVoiceChat.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        binding.rvVoiceChat.adapter = adapter
        messages.add(ChatMessage("Ciao! Sono CyAgent. Parla con me o scrivi per ricevere aiuto su truffe, sicurezza e contenuti sospetti.", false))
        adapter.notifyItemInserted(0)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = LocaleHelper.ttsLocale }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnVoiceMic.setOnClickListener { if (isListening) stopListening() else startListening() }
        binding.btnVoiceSend.setOnClickListener {
            val text = binding.etVoiceText.text.toString().trim()
            if (text.isNotEmpty()) { binding.etVoiceText.setText(""); sendMessage(text) }
        }
        setupNav()
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
            binding.tvListeningLabel.text = "Sto ascoltando... (premi per fermare)"
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
        lifecycleScope.launch {
            try {
                val resp = api.analyze(AnalyzeRequest(text, "text", history.toList()))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val reply = body?.conversationalMessage ?: body?.analysis?.explanation ?: "Analisi completata"
                    history.add(mapOf("role" to "user", "content" to text))
                    history.add(mapOf("role" to "assistant", "content" to reply))
                    messages.add(ChatMessage(reply, false))
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.rvVoiceChat.scrollToPosition(messages.size - 1)
                    tts?.speak(reply.take(250), TextToSpeech.QUEUE_FLUSH, null, null)
                } else Toast.makeText(this@VoiceActivity, "Errore analisi", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@VoiceActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
            finally { binding.progressVoice.visibility = View.GONE }
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