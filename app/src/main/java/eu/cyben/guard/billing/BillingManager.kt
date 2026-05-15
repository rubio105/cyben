package eu.cyben.guard.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(private val context: Context) {

    companion object {
        const val PRODUCT_MONTHLY = "cyben_premium_monthly"
        const val PRODUCT_ANNUAL = "cyben_premium_yearly"
    }

    private var billingClient: BillingClient? = null
    var purchaseListener: ((Purchase) -> Unit)? = null

    fun init() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    purchases.forEach { purchaseListener?.invoke(it) }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
    }

    suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() {
                if (cont.isActive) cont.resume(false)
            }
        })
    }

    suspend fun getProducts(): List<ProductDetails> {
        val client = billingClient ?: return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_MONTHLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ANNUAL)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ))
            .build()
        return client.queryProductDetails(params).productDetailsList ?: emptyList()
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val client = billingClient ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE).build()
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            ))
            .build()
        return client.launchBillingFlow(activity, params)
    }

    suspend fun acknowledgePurchase(purchaseToken: String) {
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        client.acknowledgePurchase(params)
    }

    fun disconnect() { billingClient?.endConnection() }
}