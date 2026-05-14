package eu.cyben.guard.ui.vpn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.ui.analysis.AnalysisHistoryActivity
import eu.cyben.guard.databinding.ActivityVpnBinding
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.ui.breach.BreachMonitorActivity
import eu.cyben.guard.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VPNActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityVpnBinding
    private var connected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVpnBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnConnectVpn.setOnClickListener { if (connected) disconnect() else connect() }
        setupNav()
        loadCredentials()
        loadDnsStats()
    }

    private fun loadCredentials() {
        binding.progressVpn.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getVPNCredentials()
                if (resp.isSuccessful) {
                    val creds = resp.body()
                    if (creds != null) {
                        binding.tvVpnCredentials.visibility = View.VISIBLE
                        binding.tvVpnCredentials.text = "Server: vpn.cyben.eu  User: ${creds.username}"
                    }
                }
            } catch (_: Exception) {}
            finally { binding.progressVpn.visibility = View.GONE }
        }
    }

    private fun loadDnsStats() {
        lifecycleScope.launch {
            try {
                val resp = api.getVPNDnsStats()
                if (resp.isSuccessful) {
                    val stats = resp.body() ?: return@launch
                    binding.cardDnsStats.visibility = View.VISIBLE
                    binding.tvDnsActive.text = if (stats.active) "Filtro attivo" else "Filtro inattivo"
                    binding.tvDnsBlocked.text = "%,d".format(stats.blockedDomains)
                    binding.tvDnsServer.text = stats.dnsServer
                    if (!stats.lastUpdated.isNullOrBlank()) {
                        binding.tvDnsLastUpdated.text = "Aggiornato: ${stats.lastUpdated}"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun connect() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vpn://"))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Configura la VPN nelle impostazioni di sistema: server vpn.cyben.eu, protocollo IKEv2", Toast.LENGTH_LONG).show()
        }
        connected = true
        updateStatus()
    }

    private fun disconnect() {
        connected = false
        updateStatus()
    }

    private fun updateStatus() {
        if (connected) {
            binding.tvVpnStatus.text = "Connesso"
            binding.tvVpnStatusDesc.text = "VPN attiva - Germania, Francoforte"
            binding.tvConnectLabel.text = "Disconnetti VPN"
            binding.tvVpnStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            binding.tvVpnStatus.text = "Disconnesso"
            binding.tvVpnStatusDesc.text = "Nessuna connessione VPN attiva"
            binding.tvConnectLabel.text = "Connetti VPN"
            binding.tvVpnStatus.setTextColor(0xFFB0B0C0.toInt())
        }
    }

    private fun setupNav() {
        binding.tabAnalizza.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabViolazioni.setOnClickListener {
            startActivity(Intent(this, BreachMonitorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabVPN.setOnClickListener { /* already here */ }
        binding.tabImpostazioni.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

}