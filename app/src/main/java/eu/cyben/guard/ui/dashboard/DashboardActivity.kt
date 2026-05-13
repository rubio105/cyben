package eu.cyben.guard.ui.dashboard

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.telecom.TelecomManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.AnalyzeRequest
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.models.ChatMessage
import eu.cyben.guard.data.models.GuardUser
import eu.cyben.guard.databinding.ActivityDashboardBinding
import eu.cyben.guard.ui.auth.LoginActivity
import eu.cyben.guard.ui.breach.BreachMonitorActivity
import eu.cyben.guard.ui.settings.SettingsActivity
import eu.cyben.guard.ui.subscription.SubscriptionActivity
import eu.cyben.guard.ui.vpn.VPNActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityDashboardBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val history = mutableListOf<Map<String, String>>()
    private var currentUser: GuardUser? = null
    private var permissionsRequested = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val qrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val qrText = result.data?.getStringExtra("qr_result") ?: return@registerForActivityResult
            sendMessage("QR Code scansionato: $qrText")
        }
    }

    private val imageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            sendImage(base64)
        } catch (e: Exception) {
            Toast.makeText(this, "Errore apertura immagine", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) offerCallScreeningSetup()
        else showPermissionDeniedDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tts = TextToSpeech(this, this)
        setupChat()
        setupNav()
        loadUser()
    }

    override fun onResume() {
        super.onResume()
        if (!permissionsRequested) {
            permissionsRequested = true
            checkAndRequestPermissions()
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ITALIAN
            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.btnMic.setColorFilter(getColor(android.R.color.holo_red_light))
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                binding.etMessage.setText(text)
                binding.btnMic.clearColorFilter()
                isListening = false
                sendMessage(text)
                binding.etMessage.setText("")
            }
            override fun onError(error: Int) {
                binding.btnMic.clearColorFilter()
                isListening = false
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun toggleListening() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
            return
        }
        if (speechRecognizer == null) setupSpeechRecognizer()
        if (isListening) {
            speechRecognizer?.stopListening()
            binding.btnMic.clearColorFilter()
            isListening = false
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        }
    }

    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()
        listOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECORD_AUDIO
        ).forEach { perm ->
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                needed.add(perm)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                needed.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isEmpty()) { offerCallScreeningSetup(); return }
        val msg = "Cyben Guard analizza SMS, chiamate e supporta comandi vocali per proteggerti da phishing e truffe."
        AlertDialog.Builder(this)
            .setTitle("Attiva la protezione")
            .setMessage(msg)
            .setPositiveButton("Consenti") { _, _ -> permissionLauncher.launch(needed.toTypedArray()) }
            .setNegativeButton("Non ora", null)
            .show()
    }

    private fun offerCallScreeningSetup() {
        val prefs = getSharedPreferences("guard_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("call_screening_offered", false)) return
        prefs.edit().putBoolean("call_screening_offered", true).apply()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        AlertDialog.Builder(this)
            .setTitle("Screening chiamate")
            .setMessage("Per rilevare chiamate truffa, imposta Cyben Guard come app di screening nelle impostazioni.")
            .setPositiveButton("Configura") { _, _ ->
                try {
                    startActivity(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                        .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                }
            }
            .setNegativeButton("Dopo", null).show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permessi necessari")
            .setMessage("Senza i permessi Cyben Guard non puo monitorare SMS e chiamate. Puoi attivarli dalle Impostazioni.")
            .setPositiveButton("Impostazioni") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Ignora", null).show()
    }

    private fun setupChat() {
        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        binding.rvChat.adapter = adapter
        val welcome = "Ciao! Sono il tuo assistente AI. Scrivi o parla: analizza SMS, email, URL o qualsiasi testo sospetto."
        messages.add(ChatMessage(welcome, false))
        adapter.notifyItemInserted(0)
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) { binding.etMessage.setText(""); sendMessage(text) }
        }
        binding.btnQr.setOnClickListener {
            startActivity(Intent(this, QrScanActivity::class.java))
            qrLauncher.launch(Intent(this, QrScanActivity::class.java))
        }
        binding.btnImage.setOnClickListener {
            if (currentUser?.isPremium == true) imageLauncher.launch("image/*")
            else { Toast.makeText(this, "Funzione Premium", Toast.LENGTH_SHORT).show(); startActivity(Intent(this, SubscriptionActivity::class.java)) }
        }
        binding.btnMic.setOnClickListener { toggleListening() }
    }

    private fun sendImage(base64: String) {
        messages.add(ChatMessage("[Immagine inviata per analisi]", true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
        binding.progressTyping.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false
        lifecycleScope.launch {
            try {
                val resp = api.analyzeImage(eu.cyben.guard.data.api.ImageAnalyzeRequest(base64))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val reply = body?.conversationalMessage ?: body?.analysis?.explanation ?: "Analisi completata"
                    messages.add(ChatMessage(reply, false))
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                    speak(reply)
                    body?.analysis?.riskLevel?.let { risk ->
                        if (risk == "dangerous" || risk == "suspicious") {
                            binding.tvRiskBanner.visibility = View.VISIBLE
                            binding.tvRiskBanner.text = "Attenzione - ${body.analysis.riskLabel}: ${body.analysis.recommendation ?: ""}"
                        } else binding.tvRiskBanner.visibility = View.GONE
                    }
                } else if (resp.code() == 402) {
                    Toast.makeText(this@DashboardActivity, "Funzione Premium", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@DashboardActivity, SubscriptionActivity::class.java))
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Errore analisi immagine", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressTyping.visibility = View.GONE
                binding.btnSend.isEnabled = true
            }
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
                    val reply = body?.conversationalMessage ?: body?.analysis?.explanation ?: "Analisi completata"
                    history.add(mapOf("role" to "user", "content" to text))
                    history.add(mapOf("role" to "assistant", "content" to reply))
                    messages.add(ChatMessage(reply, false))
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                    speak(reply)
                    body?.analysis?.riskLevel?.let { risk ->
                        if (risk == "dangerous" || risk == "suspicious") {
                            binding.tvRiskBanner.visibility = View.VISIBLE
                            binding.tvRiskBanner.text = "Attenzione - ${body.analysis.riskLabel}: ${body.analysis.recommendation ?: ""}"
                        } else binding.tvRiskBanner.visibility = View.GONE
                    }
                } else if (resp.code() == 401) logout()
                else if (resp.code() == 402) {
                    Toast.makeText(this@DashboardActivity, "Abbonamento richiesto", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@DashboardActivity, SubscriptionActivity::class.java))
                } else Toast.makeText(this@DashboardActivity, "Errore analisi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressTyping.visibility = View.GONE
                binding.btnSend.isEnabled = true
            }
        }
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val resp = api.getMe()
                if (resp.isSuccessful) {
                    currentUser = resp.body()
                    if (currentUser?.hasActiveSubscription != true) {
                        startActivity(Intent(this@DashboardActivity, SubscriptionActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("required", true)
                        })
                        return@launch
                    }
                    binding.tvUserName.text = currentUser?.name ?: ""
                    binding.tvPlanBadge.text = currentUser?.planLabel ?: ""
                } else if (resp.code() == 401) logout()
            } catch (_: Exception) {}
        }
    }

    private fun setupNav() {
        binding.btnBreach.setOnClickListener { startActivity(Intent(this, BreachMonitorActivity::class.java)) }
        binding.btnVpn.setOnClickListener { startActivity(Intent(this, VPNActivity::class.java)) }
        binding.btnSubscription.setOnClickListener { startActivity(Intent(this, SubscriptionActivity::class.java).apply { putExtra("required", false) }) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private fun logout() {
        tokenManager.clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}
