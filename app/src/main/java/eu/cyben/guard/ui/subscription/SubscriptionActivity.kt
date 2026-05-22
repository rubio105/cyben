package eu.cyben.guard.ui.subscription

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.billing.BillingManager
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.SubscribeRequest
import eu.cyben.guard.databinding.ActivitySubscriptionBinding
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.ui.auth.LoginActivity
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var billingManager: BillingManager
    private var monthlyProduct: ProductDetails? = null
    private var annualProduct: ProductDetails? = null
    private var required = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        required = intent.getBooleanExtra("required", false)
        binding.btnBack.setOnClickListener { finish() }

        billingManager = BillingManager(this)
        billingManager.init()
        billingManager.purchaseListener = { purchase -> handlePurchase(purchase) }

        if (required) {
            binding.btnManage.visibility = View.GONE
            onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    tokenManager.clearToken()
                    startActivity(Intent(this@SubscriptionActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            })
        }

        binding.btnSubscribePremium?.setOnClickListener {
            monthlyProduct?.let { billingManager.launchBillingFlow(this, it) }
                ?: Toast.makeText(this, "Prodotto non disponibile", Toast.LENGTH_SHORT).show()
        }
        binding.btnSubscribePremiumAnnual?.setOnClickListener {
            annualProduct?.let { billingManager.launchBillingFlow(this, it) }
                ?: Toast.makeText(this, "Prodotto non disponibile", Toast.LENGTH_SHORT).show()
        }
        binding.btnManage.setOnClickListener { syncSubscription() }

        loadProducts()
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            val connected = billingManager.connect()
            if (!connected) return@launch
            val products = billingManager.getProducts()
            for (p in products) {
                when (p.productId) {
                    BillingManager.PRODUCT_MONTHLY -> monthlyProduct = p
                    BillingManager.PRODUCT_ANNUAL -> annualProduct = p
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return
        val plan = if (productId == BillingManager.PRODUCT_ANNUAL) "annual" else "monthly"
        val interval = if (productId == BillingManager.PRODUCT_ANNUAL) "year" else "month"
        lifecycleScope.launch {
            try {
                val resp = api.subscribe(SubscribeRequest(
                    plan = "premium",
                    billingPeriod = interval,
                    purchaseToken = purchase.purchaseToken,
                    productId = productId
                ))
                if (resp.isSuccessful) {
                    billingManager.acknowledgePurchase(purchase.purchaseToken)
                    startActivity(Intent(this@SubscriptionActivity, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    Toast.makeText(this@SubscriptionActivity, "Errore attivazione abbonamento", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SubscriptionActivity, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun syncSubscription() {
        lifecycleScope.launch {
            try {
                val resp = api.syncSubscription()
                if (resp.isSuccessful && (resp.body()?.subscriptionStatus == "active" || resp.body()?.subscriptionStatus == "trialing")) {
                    startActivity(Intent(this@SubscriptionActivity, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            } catch (_: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        if (required) {
            lifecycleScope.launch {
                try {
                    val resp = api.getMe()
                    if (resp.isSuccessful && (resp.body()?.subscriptionStatus == "active" || resp.body()?.subscriptionStatus == "trialing")) {
                        startActivity(Intent(this@SubscriptionActivity, DashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.disconnect()
    }
}