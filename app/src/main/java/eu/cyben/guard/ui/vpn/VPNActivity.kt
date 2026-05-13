package eu.cyben.guard.ui.vpn

import android.net.Ikev2VpnProfile
import android.net.VpnManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.R
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.databinding.ActivityVpnBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VPNActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityVpnBinding
    private var vpnConnected = false

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
        else Toast.makeText(this, "Permesso VPN negato", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVpnBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnConnect.setOnClickListener { if (vpnConnected) disconnectVpn() else connectVpn() }
        setConnectedState(false)
        loadVpnStats()
    }

    private fun connectVpn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(this, "La VPN IKEv2 richiede Android 12+. Configura manualmente nelle impostazioni VPN.", Toast.LENGTH_LONG).show()
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getVPNCredentials()
                if (resp.isSuccessful && resp.body() != null) {
                    val creds = resp.body()!!
                    provisionAndConnect(creds.username, creds.password)
                } else {
                    Toast.makeText(this@VPNActivity, "Errore caricamento credenziali VPN", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VPNActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun provisionAndConnect(username: String, password: String) {
        try {
            val mgr = getSystemService(VpnManager::class.java)
            val profile = Ikev2VpnProfile.Builder(VPN_SERVER, username)
                .setAuthUsernamePassword(username, password, null)
                .build()
            val intent = mgr.provisionVpnProfile(profile)
            if (intent != null) vpnPermissionLauncher.launch(intent) else startVpn()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore configurazione VPN: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startVpn() {
        try {
            getSystemService(VpnManager::class.java).startProvisionedVpnProfile()
            setConnectedState(true)
        } catch (e: Exception) {
            Toast.makeText(this, "Errore avvio VPN: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disconnectVpn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { getSystemService(VpnManager::class.java).stopProvisionedVpnProfile() } catch (_: Exception) {}
        }
        setConnectedState(false)
    }

    private fun setConnectedState(connected: Boolean) {
        vpnConnected = connected
        if (connected) {
            binding.tvVpnStatus.text = "VPN Attiva"
            binding.tvVpnSubtitle.text = "Connesso a $VPN_SERVER"
            binding.vStatusDot.setBackgroundResource(R.drawable.dot_green)
            binding.btnConnect.text = "Disconnetti"
        } else {
            binding.tvVpnStatus.text = "VPN Non connessa"
            binding.tvVpnSubtitle.text = "Tocca il pulsante per connetterti"
            binding.vStatusDot.setBackgroundResource(R.drawable.dot_red)
            binding.btnConnect.text = "Connetti VPN"
        }
    }

    private fun loadVpnStats() {
        lifecycleScope.launch {
            try {
                val resp = api.getVPNDnsStats()
                if (resp.isSuccessful) {
                    val s = resp.body()
                    binding.tvBlockedDomains.text = "Domini bloccati: ${s?.blockedDomains ?: "--"}"
                    binding.tvDnsServer.text = "DNS Server: ${s?.dnsServer ?: "--"}"
                    binding.tvDnsStatus.text = if (s?.active == true) "DNS Protetto Attivo" else "DNS Inattivo"
                }
            } catch (_: Exception) {}
        }
    }

    companion object { private const val VPN_SERVER = "vpn.cyben.eu" }
}
