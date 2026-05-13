package eu.cyben.guard.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import eu.cyben.guard.R;
import kotlinx.coroutines.Dispatchers;
import org.json.JSONObject;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\u0018\u0000 \u00122\u00020\u0001:\u0001\u0012B\u0005\u00a2\u0006\u0002\u0010\u0002J \u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\bH\u0002J\u0012\u0010\n\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J\u0018\u0010\u000b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\rH\u0016J(\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\bH\u0002\u00a8\u0006\u0013"}, d2 = {"Leu/cyben/guard/receiver/SmsReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "analyzeAsync", "", "context", "Landroid/content/Context;", "sender", "", "body", "getToken", "onReceive", "intent", "Landroid/content/Intent;", "showNotification", "score", "", "reason", "Companion", "app_release"})
public final class SmsReceiver extends android.content.BroadcastReceiver {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_URL = "https://cyben.eu";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHANNEL_ID = "guard_sms_alerts";
    @org.jetbrains.annotations.NotNull()
    public static final eu.cyben.guard.receiver.SmsReceiver.Companion Companion = null;
    
    public SmsReceiver() {
        super();
    }
    
    @java.lang.Override()
    public void onReceive(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
    }
    
    private final java.lang.String getToken(android.content.Context context) {
        return null;
    }
    
    private final void analyzeAsync(android.content.Context context, java.lang.String sender, java.lang.String body) {
    }
    
    private final void showNotification(android.content.Context context, java.lang.String sender, int score, java.lang.String reason) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Leu/cyben/guard/receiver/SmsReceiver$Companion;", "", "()V", "BASE_URL", "", "CHANNEL_ID", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}