package eu.cyben.guard.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import kotlinx.coroutines.Dispatchers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONObject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\n\u0010\u0003\u001a\u0004\u0018\u00010\u0004H\u0002J\u0010\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016J\u0018\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u0004H\u0002\u00a8\u0006\f"}, d2 = {"Leu/cyben/guard/service/GuardCallScreeningService;", "Landroid/telecom/CallScreeningService;", "()V", "getToken", "", "onScreenCall", "", "callDetails", "Landroid/telecom/Call$Details;", "showNotification", "title", "message", "app_debug"})
public final class GuardCallScreeningService extends android.telecom.CallScreeningService {
    
    public GuardCallScreeningService() {
        super();
    }
    
    @java.lang.Override()
    public void onScreenCall(@org.jetbrains.annotations.NotNull()
    android.telecom.Call.Details callDetails) {
    }
    
    private final java.lang.String getToken() {
        return null;
    }
    
    private final void showNotification(java.lang.String title, java.lang.String message) {
    }
}