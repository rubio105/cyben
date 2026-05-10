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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivitySubscriptionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubscribeBase.setOnClickListener { subscribe("basic", "annual") }
        binding.btnSubscribePremium.setOnClickListener { subscribe("premium", "monthly") }
        binding.btnSubscribePremiumAnnual.setOnClickListener { subscribe("premium", "annual") }
        binding.btnManage.setOnClickListener { openBillingPortal() }
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
