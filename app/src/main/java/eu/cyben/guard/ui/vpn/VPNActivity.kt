package eu.cyben.guard.ui.vpn

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.databinding.ActivityVpnBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VPNActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityVpnBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVpnBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        loadVPNStats()
    }

    private fun loadVPNStats() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getVPNDnsStats()
                if (resp.isSuccessful) {
                    val stats = resp.body()
                    binding.tvVpnStatus.text = if (stats?.active == true) "🟢 VPN Attiva" else "🔴 VPN Non connessa"
                    binding.tvBlockedDomains.text = "Domini bloccati: ${stats?.blockedDomains ?: 0}"
                    binding.tvDnsServer.text = "DNS: ${stats?.dnsServer ?: "-"}"
                }
            } catch (e: Exception) { Toast.makeText(this@VPNActivity, "Errore caricamento VPN", Toast.LENGTH_SHORT).show() }
            finally { binding.progressBar.visibility = View.GONE }
        }
    }
}
