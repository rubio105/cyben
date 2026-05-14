package eu.cyben.guard.ui.subscription;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.billing.BillingManager;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.data.api.SubscribeRequest;
import eu.cyben.guard.databinding.ActivitySubscriptionBinding;
import eu.cyben.guard.ui.dashboard.DashboardActivity;
import eu.cyben.guard.ui.auth.LoginActivity;
import eu.cyben.guard.utils.TokenManager;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u0002J\b\u0010\u001c\u001a\u00020\u0019H\u0002J\u0012\u0010\u001d\u001a\u00020\u00192\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0014J\b\u0010 \u001a\u00020\u0019H\u0014J\b\u0010!\u001a\u00020\u0019H\u0014J\b\u0010\"\u001a\u00020\u0019H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0005\u001a\u00020\u00068\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0012\u001a\u00020\u00138\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\u0015\"\u0004\b\u0016\u0010\u0017\u00a8\u0006#"}, d2 = {"Leu/cyben/guard/ui/subscription/SubscriptionActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "annualProduct", "Lcom/android/billingclient/api/ProductDetails;", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "billingManager", "Leu/cyben/guard/billing/BillingManager;", "binding", "Leu/cyben/guard/databinding/ActivitySubscriptionBinding;", "monthlyProduct", "required", "", "tokenManager", "Leu/cyben/guard/utils/TokenManager;", "getTokenManager", "()Leu/cyben/guard/utils/TokenManager;", "setTokenManager", "(Leu/cyben/guard/utils/TokenManager;)V", "handlePurchase", "", "purchase", "Lcom/android/billingclient/api/Purchase;", "loadProducts", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onResume", "syncSubscription", "app_debug"})
public final class SubscriptionActivity extends androidx.appcompat.app.AppCompatActivity {
    @javax.inject.Inject()
    public eu.cyben.guard.data.api.ApiService api;
    @javax.inject.Inject()
    public eu.cyben.guard.utils.TokenManager tokenManager;
    private eu.cyben.guard.databinding.ActivitySubscriptionBinding binding;
    private eu.cyben.guard.billing.BillingManager billingManager;
    @org.jetbrains.annotations.Nullable()
    private com.android.billingclient.api.ProductDetails monthlyProduct;
    @org.jetbrains.annotations.Nullable()
    private com.android.billingclient.api.ProductDetails annualProduct;
    private boolean required = false;
    
    public SubscriptionActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final eu.cyben.guard.data.api.ApiService getApi() {
        return null;
    }
    
    public final void setApi(@org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.ApiService p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final eu.cyben.guard.utils.TokenManager getTokenManager() {
        return null;
    }
    
    public final void setTokenManager(@org.jetbrains.annotations.NotNull()
    eu.cyben.guard.utils.TokenManager p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void loadProducts() {
    }
    
    private final void handlePurchase(com.android.billingclient.api.Purchase purchase) {
    }
    
    private final void syncSubscription() {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}