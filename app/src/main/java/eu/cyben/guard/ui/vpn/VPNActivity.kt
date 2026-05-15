package eu.cyben.guard.ui.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnManager
import android.net.VpnProfileState
import android.net.ipsec.ike.Ikev2VpnProfile
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
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

    private var vpnUsername: String? = null
    private var vpnPassword: String? = null
    private var connected = false

    companion object {
        private const val REQ_VPN_CONSENT = 1001
        private const val VPN_SERVER = "de.vpn.cyben.eu"
    }

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

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            syncVpnState()
        }
    }

    private fun loadCredentials() {
        binding.progressVpn.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getVPNCredentials()
                if (resp.isSuccessful) {
                    val creds = resp.body()
                    if (creds != null) {
                        vpnUsername = creds.username
                        vpnPassword = creds.password
                        binding.tvVpnCredentials.visibility = View.VISIBLE
                        binding.tvVpnCredentials.text = "Server: $VPN_SERVER  Utente: ${creds.username}"
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
        val username = vpnUsername
        val password = vpnPassword
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            Toast.makeText(this, "Attendi il caricamento delle credenziali", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectIkev2(username, password)
        } else {
            Toast.makeText(this, "Vai in Impostazioni → VPN e aggiungi: server $VPN_SERVER, tipo IKEv2, utente $username", Toast.LENGTH_LONG).show()
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun connectIkev2(username: String, password: String) {
        try {
            val profile = Ikev2VpnProfile.Builder(VPN_SERVER, VPN_SERVER)
                .setAuthUsernamePassword(username, password, null)
                .build()
            val vpnManager = getSystemService(VpnManager::class.java)
            val consentIntent = vpnManager.provisionVpnProfile(profile)
            if (consentIntent != null) {
                @Suppress("DEPRECATION")
                startActivityForResult(consentIntent, REQ_VPN_CONSENT)
            } else {
                startVpnProfile()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Errore configurazione VPN: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun startVpnProfile() {
        try {
            getSystemService(VpnManager::class.java).startProvisionedVpnProfile()
            connected = true
            updateStatus()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore avvio VPN: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun syncVpnState() {
        val state = getSystemService(VpnManager::class.java).provisionedVpnProfileState
        connected = state?.state == VpnProfileState.STATE_CONNECTED
        updateStatus()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN_CONSENT && resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startVpnProfile()
            }
        }
    }

    private fun disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                getSystemService(VpnManager::class.java).stopProvisionedVpnProfile()
            } catch (_: Exception) {}
        }
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
