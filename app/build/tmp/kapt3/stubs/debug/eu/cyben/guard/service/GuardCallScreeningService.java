package eu.cyben.guard.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import eu.cyben.guard.R;
import kotlinx.coroutines.Dispatchers;
import org.json.JSONObject;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\u0018\u0000 \u000f2\u00020\u0001:\u0001\u000fB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J\n\u0010\u0007\u001a\u0004\u0018\u00010\u0006H\u0002J\u0010\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\nH\u0016J \u0010\u000b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u000eH\u0002\u00a8\u0006\u0010"}, d2 = {"Leu/cyben/guard/service/GuardCallScreeningService;", "Landroid/telecom/CallScreeningService;", "()V", "checkNumberAsync", "", "number", "", "getToken", "onScreenCall", "callDetails", "Landroid/telecom/Call$Details;", "showNotification", "reason", "score", "", "Companion", "app_debug"})
public final class GuardCallScreeningService extends android.telecom.CallScreeningService {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_URL = "https://cyben.eu";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHANNEL_ID = "guard_call_alerts";
    @org.jetbrains.annotations.NotNull()
    public static final eu.cyben.guard.service.GuardCallScreeningService.Companion Companion = null;
    
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
    
    private final void checkNumberAsync(java.lang.String number) {
    }
    
    private final void showNotification(java.lang.String number, java.lang.String reason, int score) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Leu/cyben/guard/service/GuardCallScreeningService$Companion;", "", "()V", "BASE_URL", "", "CHANNEL_ID", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}