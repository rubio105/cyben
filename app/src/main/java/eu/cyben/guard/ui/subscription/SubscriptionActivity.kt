package eu.cyben.guard.ui.subscription

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.SubscribeRequest
import eu.cyben.guard.databinding.ActivitySubscriptionBinding
import eu.cyben.guard.ui.auth.LoginActivity
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivitySubscriptionBinding
    private var required = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        required = intent.getBooleanExtra("required", false)
        if (required) {
            binding.btnBack.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            binding.btnManage.visibility = View.GONE
        } else {
            binding.btnManage.visibility = View.VISIBLE
        }
        binding.btnBack.setOnClickListener {
            if (required) { tokenManager.clearToken(); startActivity(Intent(this, LoginActivity::class.java)); finishAffinity() }
            else finish()
        }
        binding.btnSubscribePremium.setOnClickListener { subscribe("premium", "month") }
        binding.btnSubscribePremiumAnnual.setOnClickListener { subscribe("premium", "year") }
        binding.btnManage.setOnClickListener { openBillingPortal() }
    }

    override fun onResume() {
        super.onResume()
        if (required) {
            lifecycleScope.launch {
                try {
                    val resp = api.getMe()
                    if (resp.isSuccessful && resp.body()?.hasActiveSubscription == true) {
                        startActivity(Intent(this@SubscriptionActivity, DashboardActivity::class.java))
                        finishAffinity()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun subscribe(plan: String, period: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.subscribe(SubscribeRequest(plan, period))
                if (resp.isSuccessful) {
                    val url = resp.body()?.url
                    if (url != null) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    else Toast.makeText(this@SubscriptionActivity, resp.body()?.error ?: "Errore", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) { Toast.makeText(this@SubscriptionActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show() }
            finally { binding.progressBar.visibility = View.GONE }
        }
    }

    private fun openBillingPortal() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getBillingPortal()
                val url = resp.body()?.url
                if (url != null) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) { Toast.makeText(this@SubscriptionActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show() }
            finally { binding.progressBar.visibility = View.GONE }
        }
    }
}