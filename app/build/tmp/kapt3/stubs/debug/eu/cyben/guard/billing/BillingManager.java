package eu.cyben.guard.billing;

import android.app.Activity;
import android.content.Context;
import com.android.billingclient.api.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000  2\u00020\u0001:\u0001 B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u000f\u001a\u00020\n2\u0006\u0010\u0010\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u000e\u0010\u0013\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u0006\u0010\u0016\u001a\u00020\nJ\u0014\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00190\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u0006\u0010\u001a\u001a\u00020\nJ\u0016\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020\u0019R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R(\u0010\u0007\u001a\u0010\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\n\u0018\u00010\bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e\u00a8\u0006!"}, d2 = {"Leu/cyben/guard/billing/BillingManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "billingClient", "Lcom/android/billingclient/api/BillingClient;", "purchaseListener", "Lkotlin/Function1;", "Lcom/android/billingclient/api/Purchase;", "", "getPurchaseListener", "()Lkotlin/jvm/functions/Function1;", "setPurchaseListener", "(Lkotlin/jvm/functions/Function1;)V", "acknowledgePurchase", "purchaseToken", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "connect", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "disconnect", "getProducts", "", "Lcom/android/billingclient/api/ProductDetails;", "init", "launchBillingFlow", "Lcom/android/billingclient/api/BillingResult;", "activity", "Landroid/app/Activity;", "productDetails", "Companion", "app_debug"})
public final class BillingManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String PRODUCT_MONTHLY = "cyben_guard_monthly";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String PRODUCT_ANNUAL = "cyben_guard_annual";
    @org.jetbrains.annotations.Nullable()
    private com.android.billingclient.api.BillingClient billingClient;
    @org.jetbrains.annotations.Nullable()
    private kotlin.jvm.functions.Function1<? super com.android.billingclient.api.Purchase, kotlin.Unit> purchaseListener;
    @org.jetbrains.annotations.NotNull()
    public static final eu.cyben.guard.billing.BillingManager.Companion Companion = null;
    
    public BillingManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.jvm.functions.Function1<com.android.billingclient.api.Purchase, kotlin.Unit> getPurchaseListener() {
        return null;
    }
    
    public final void setPurchaseListener(@org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super com.android.billingclient.api.Purchase, kotlin.Unit> p0) {
    }
    
    public final void init() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object connect(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getProducts(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.android.billingclient.api.ProductDetails>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.android.billingclient.api.BillingResult launchBillingFlow(@org.jetbrains.annotations.NotNull()
    android.app.Activity activity, @org.jetbrains.annotations.NotNull()
    com.android.billingclient.api.ProductDetails productDetails) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object acknowledgePurchase(@org.jetbrains.annotations.NotNull()
    java.lang.String purchaseToken, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void disconnect() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Leu/cyben/guard/billing/BillingManager$Companion;", "", "()V", "PRODUCT_ANNUAL", "", "PRODUCT_MONTHLY", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}