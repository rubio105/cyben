package eu.cyben.guard.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupChat()
        setupNav()
        loadUser()
    }

    private fun setupChat() {
        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        binding.rvChat.adapter = adapter
        messages.add(ChatMessage("Ciao! Sono il tuo assistente AI anti-phishing. Inviami un SMS sospetto, un'email, un URL o qualsiasi testo da analizzare.", false))
        adapter.notifyItemInserted(0)
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) { binding.etMessage.setText(""); sendMessage(text) }
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
                    body?.analysis?.riskLevel?.let { risk ->
                        if (risk == "dangerous" || risk == "suspicious") {
                            binding.tvRiskBanner.visibility = View.VISIBLE
                            binding.tvRiskBanner.text = "⚠️ ${body.analysis.riskLabel}: ${body.analysis.recommendation ?: ""}"
                        } else binding.tvRiskBanner.visibility = View.GONE
                    }
                } else if (resp.code() == 401) logout()
                else if (resp.code() == 402) { Toast.makeText(this@DashboardActivity, "Abbonamento richiesto", Toast.LENGTH_SHORT).show(); startActivity(Intent(this@DashboardActivity, SubscriptionActivity::class.java)) }
                else Toast.makeText(this@DashboardActivity, "Errore analisi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this@DashboardActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
            finally { binding.progressTyping.visibility = View.GONE; binding.btnSend.isEnabled = true }
        }
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val resp = api.getMe()
                if (resp.isSuccessful) {
                    currentUser = resp.body()
                    binding.tvUserName.text = currentUser?.name ?: ""
                    binding.tvPlanBadge.text = currentUser?.planLabel ?: "Gratuito"
                } else if (resp.code() == 401) logout()
            } catch (_: Exception) {}
        }
    }

    private fun setupNav() {
        binding.btnBreach.setOnClickListener { startActivity(Intent(this, BreachMonitorActivity::class.java)) }
        binding.btnVpn.setOnClickListener {
            if (currentUser?.isPremium == true) startActivity(Intent(this, VPNActivity::class.java))
            else { Toast.makeText(this, "Funzione Premium", Toast.LENGTH_SHORT).show(); startActivity(Intent(this, SubscriptionActivity::class.java)) }
        }
        binding.btnSubscription.setOnClickListener { startActivity(Intent(this, SubscriptionActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private fun logout() { tokenManager.clearToken(); startActivity(Intent(this, LoginActivity::class.java)); finishAffinity() }
}
