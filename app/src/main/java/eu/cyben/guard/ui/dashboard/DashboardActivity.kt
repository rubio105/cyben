package eu.cyben.guard.ui.dashboard
import androidx.activity.result.PickVisualMediaRequest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.AnalyzeRequest
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.PhoneCheckRequest
import eu.cyben.guard.data.models.ChatMessage
import eu.cyben.guard.data.models.GuardUser
import eu.cyben.guard.data.models.ImageAnalyzeRequest
import eu.cyben.guard.databinding.ActivityDashboardBinding
import eu.cyben.guard.ui.auth.LoginActivity
import eu.cyben.guard.ui.breach.BreachMonitorActivity
import eu.cyben.guard.ui.settings.SettingsActivity
import eu.cyben.guard.ui.subscription.SubscriptionActivity
import eu.cyben.guard.ui.voice.VoiceActivity
import eu.cyben.guard.ui.vpn.VPNActivity
import eu.cyben.guard.utils.LocaleHelper
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityDashboardBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val history = mutableListOf<Map<String, String>>()
    private var currentUser: GuardUser? = null
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var dailyUsed = 0
    private var dailyLimit = 10

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { analyzeImageUri(it) }
    }

    private val qrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val qrText = result.data?.getStringExtra("qr_result") ?: return@registerForActivityResult
            sendMessage("QR Code: $qrText")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupChat()
        setupQuickActions()
        setupNav()
        loadUser()
        initTts()
        requestRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        val user = currentUser ?: return
        if (!user.hasActiveSubscription) {
            startActivity(Intent(this, SubscriptionActivity::class.java).putExtra("required", true))
        }
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = LocaleHelper.ttsLocale
        }
    }

    private fun setupChat() {
        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        binding.rvChat.adapter = adapter
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) { binding.etMessage.setText(""); sendMessage(text) }
        }
        binding.btnMic.setOnClickListener {
            if (isListening) stopListening() else startListening()
        }
    }

    private fun setupQuickActions() {
        binding.btnQuickImage.setOnClickListener {
            if (currentUser?.isPremium == true) imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            else showUpgradeDialog()
        }
        binding.btnQuickQr.setOnClickListener {
            qrLauncher.launch(Intent(this, QrScanActivity::class.java))
        }
        binding.btnQuickNumber.setOnClickListener { startActivity(Intent(this, PhoneCheckActivity::class.java)) }
        binding.btnQuickVoice.setOnClickListener {
            startActivity(Intent(this, VoiceActivity::class.java))
        }
    }

    private fun setupNav() {
        binding.tabAnalizza.setOnClickListener { /* already here */ }
        binding.tabViolazioni.setOnClickListener {
            startActivity(Intent(this, BreachMonitorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabVPN.setOnClickListener {
            if (currentUser?.isPremium == true)
                startActivity(Intent(this, VPNActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            else showUpgradeDialog()
        }
        binding.tabImpostazioni.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val resp = api.getMe()
                if (resp.isSuccessful) {
                    currentUser = resp.body()
                    val name = currentUser?.name?.split(" ")?.firstOrNull() ?: "Utente"
                    if (messages.isEmpty()) {
                        messages.add(ChatMessage("Ciao $name! Sono Cyben. Sono qui per aiutarti a verificare contenuti sospetti: SMS, email, link, foto o QR code. Cosa vuoi verificare oggi?", false))
                        adapter.notifyItemInserted(0)
                    }
                    if (currentUser?.isPremium == true) binding.btnMic.visibility = View.VISIBLE
                    if (currentUser?.hasActiveSubscription == false) {
                        startActivity(Intent(this@DashboardActivity, SubscriptionActivity::class.java).putExtra("required", true))
                        return@launch
                    }
                    loadSecurityScore()
                } else if (resp.code() == 401) logout()
                else if (messages.isEmpty()) {
                    messages.add(ChatMessage("Ciao! Sono Cyben. Cosa vuoi verificare?", false))
                    adapter.notifyItemInserted(0)
                }
            } catch (_: Exception) {
                if (messages.isEmpty()) {
                    messages.add(ChatMessage("Ciao! Sono Cyben. Cosa vuoi verificare?", false))
                    adapter.notifyItemInserted(0)
                }
            }
        }
    }

    private fun loadSecurityScore() {
        lifecycleScope.launch {
            try {
                val resp = api.getAnalyses()
                if (resp.isSuccessful) {
                    val analyses = resp.body() ?: emptyList()
                    val safe = analyses.count { it.riskLevel == "safe" }
                    val suspicious = analyses.count { it.riskLevel == "suspicious" }
                    val dangerous = analyses.count { it.riskLevel == "dangerous" }
                    val total = safe + suspicious + dangerous
                    val score = if (total == 0) 100
                    else ((safe * 100.0 + suspicious * 50.0) / total).toInt().coerceIn(0, 100)
                    binding.tvScoreBadge.text = "Score $score"
                    binding.tvAnalysisCount.text = "$dailyUsed/$dailyLimit"
                }
            } catch (_: Exception) {}
        }
    }

    private fun sendMessage(text: String) {
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
        binding.progressTyping.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false
        lifecycleScope.launch {
            try {
                val resp = api.analyze(AnalyzeRequest(text, "text", history.toList()))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    dailyUsed = body?.dailyUsed ?: dailyUsed
                    dailyLimit = body?.dailyLimit ?: dailyLimit
                    binding.tvAnalysisCount.text = "$dailyUsed/$dailyLimit"
                    val reply = body?.conversationalMessage ?: body?.analysis?.explanation ?: "Analisi completata"
                    history.add(mapOf("role" to "user", "content" to text))
                    history.add(mapOf("role" to "assistant", "content" to reply))
                    messages.add(ChatMessage(reply, false))
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                    body?.analysis?.riskLevel?.let { risk ->
                        if (risk == "dangerous" || risk == "suspicious") {
                            binding.tvRiskBanner.visibility = View.VISIBLE
                            binding.tvRiskBanner.text = "Attenzione - ${body.analysis.riskLabel}: ${body.analysis.recommendation ?: ""}"
                        } else binding.tvRiskBanner.visibility = View.GONE
                    }
                    if (currentUser?.isPremium == true) tts?.speak(reply.take(200), TextToSpeech.QUEUE_FLUSH, null, null)
                } else if (resp.code() == 401) logout()
                else if (resp.code() == 402) showUpgradeDialog()
                else Toast.makeText(this@DashboardActivity, "Errore analisi", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@DashboardActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
            finally { binding.progressTyping.visibility = View.GONE; binding.btnSend.isEnabled = true }
        }
    }

    private fun analyzeImageUri(uri: Uri) {
        messages.add(ChatMessage("[Immagine inviata per analisi]", true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
        binding.progressTyping.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val resp = api.analyzeImage(ImageAnalyzeRequest(base64))
                if (resp.isSuccessful) {
                    val reply = resp.body()?.conversationalMessage ?: resp.body()?.analysis?.explanation ?: "Analisi immagine completata"
                    messages.add(ChatMessage(reply, false))
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            } catch (_: Exception) { Toast.makeText(this@DashboardActivity, "Errore analisi immagine", Toast.LENGTH_SHORT).show() }
            finally { binding.progressTyping.visibility = View.GONE }
        }
    }

    private fun showPhoneCheckDialog() {
        val input = EditText(this).apply { hint = "+39 123 456 7890"; inputType = android.text.InputType.TYPE_CLASS_PHONE; setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(this).setTitle("Verifica Numero").setMessage("Inserisci il numero da verificare").setView(input)
            .setPositiveButton("Verifica") { _, _ -> val n = input.text.toString().trim(); if (n.isNotEmpty()) checkPhoneNumber(n) }
            .setNegativeButton("Annulla", null).show()
    }

    private fun checkPhoneNumber(number: String) {
        binding.progressTyping.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.checkPhone(PhoneCheckRequest(number))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val score = body?.score ?: 100
                    val isScam = body?.isScam == true || score < 40
                    val riskLabel = when {
                        isScam || score < 40 -> "TRUFFA"
                        score < 70 -> "SOSPETTO"
                        else -> "SICURO"
                    }
                    val riskColor = when {
                        isScam || score < 40 -> "#FF1744"
                        score < 70 -> "#FF9800"
                        else -> "#4CAF50"
                    }
                    val summary = "${body?.explanation ?: "Nessun dettaglio disponibile"}"
                    messages.add(ChatMessage("Verifica numero: $number", true))
                    messages.add(ChatMessage("$riskLabel — Score: $score/100\n$summary", false))
                    adapter.notifyItemRangeInserted(messages.size - 2, 2)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                    showPhoneResultDialog(number, riskLabel, riskColor, score, body?.explanation)
                }
            } catch (_: Exception) { Toast.makeText(this@DashboardActivity, "Errore verifica numero", Toast.LENGTH_SHORT).show() }
            finally { binding.progressTyping.visibility = View.GONE }
        }
    }

    private fun showPhoneResultDialog(number: String, label: String, colorHex: String, score: Int, explanation: String?) {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(56, 32, 56, 16)
            gravity = android.view.Gravity.CENTER
        }
        val scoreView = android.widget.TextView(this).apply {
            text = "$score"
            textSize = 48f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(colorHex))
            gravity = android.view.Gravity.CENTER
        }
        val labelView = android.widget.TextView(this).apply {
            text = label
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(colorHex))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 4, 0, 12)
        }
        val descView = android.widget.TextView(this).apply {
            text = explanation ?: "Nessun dettaglio disponibile"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#B0B0C0"))
            gravity = android.view.Gravity.CENTER
        }
        container.addView(scoreView)
        container.addView(labelView)
        container.addView(descView)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Verifica: $number")
            .setView(container)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101); return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                sendMessage(text); isListening = false; binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
            }
            override fun onError(error: Int) { isListening = false; binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now) }
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
        isListening = true; binding.btnMic.setImageResource(android.R.drawable.ic_media_pause)
        Toast.makeText(this, "Sto ascoltando...", Toast.LENGTH_SHORT).show()
    }

    private fun stopListening() { speechRecognizer?.stopListening(); isListening = false; binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now) }
    private fun showUpgradeDialog() {
        AlertDialog.Builder(this).setTitle("Funzione Premium").setMessage("Questa funzione richiede un abbonamento Premium.")
            .setPositiveButton("Abbonati") { _, _ -> startActivity(Intent(this, SubscriptionActivity::class.java)) }
            .setNegativeButton("Annulla", null).show()
    }
    private fun requestRuntimePermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        if (perms.isNotEmpty()) ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
    }
    private fun logout() { tokenManager.clearToken(); startActivity(Intent(this, LoginActivity::class.java)); finishAffinity() }
    override fun onDestroy() { tts?.shutdown(); speechRecognizer?.destroy(); super.onDestroy() }
}