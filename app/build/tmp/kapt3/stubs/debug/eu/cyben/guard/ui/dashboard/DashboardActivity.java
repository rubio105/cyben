package eu.cyben.guard.ui.dashboard;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telecom.TelecomManager;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.data.api.AnalyzeRequest;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.data.models.ChatMessage;
import eu.cyben.guard.data.models.GuardUser;
import eu.cyben.guard.databinding.ActivityDashboardBinding;
import eu.cyben.guard.ui.auth.LoginActivity;
import eu.cyben.guard.ui.breach.BreachMonitorActivity;
import eu.cyben.guard.ui.settings.SettingsActivity;
import eu.cyben.guard.ui.subscription.SubscriptionActivity;
import eu.cyben.guard.ui.vpn.VPNActivity;
import eu.cyben.guard.utils.TokenManager;
import java.util.Locale;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000~\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\b\u0010\'\u001a\u00020(H\u0002J\b\u0010)\u001a\u00020(H\u0002J\b\u0010*\u001a\u00020(H\u0002J\b\u0010+\u001a\u00020(H\u0002J\u0012\u0010,\u001a\u00020(2\b\u0010-\u001a\u0004\u0018\u00010.H\u0014J\b\u0010/\u001a\u00020(H\u0014J\u0010\u00100\u001a\u00020(2\u0006\u00101\u001a\u000202H\u0016J\b\u00103\u001a\u00020(H\u0014J\u0010\u00104\u001a\u00020(2\u0006\u00105\u001a\u00020\u0013H\u0002J\b\u00106\u001a\u00020(H\u0002J\b\u00107\u001a\u00020(H\u0002J\b\u00108\u001a\u00020(H\u0002J\b\u00109\u001a\u00020(H\u0002J\u0010\u0010:\u001a\u00020(2\u0006\u00105\u001a\u00020\u0013H\u0002J\b\u0010;\u001a\u00020(H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0006\u001a\u00020\u00078\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u000e\u0010\f\u001a\u00020\rX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R \u0010\u0010\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00130\u00120\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00130\u001a0\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u001dX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u001e\u001a\u00020\u001f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b \u0010!\"\u0004\b\"\u0010#R\u0010\u0010$\u001a\u0004\u0018\u00010%X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010&\u001a\u00020\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006<"}, d2 = {"Leu/cyben/guard/ui/dashboard/DashboardActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "Landroid/speech/tts/TextToSpeech$OnInitListener;", "()V", "adapter", "Leu/cyben/guard/ui/dashboard/ChatAdapter;", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "binding", "Leu/cyben/guard/databinding/ActivityDashboardBinding;", "currentUser", "Leu/cyben/guard/data/models/GuardUser;", "history", "", "", "", "isListening", "", "messages", "Leu/cyben/guard/data/models/ChatMessage;", "permissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "permissionsRequested", "speechRecognizer", "Landroid/speech/SpeechRecognizer;", "tokenManager", "Leu/cyben/guard/utils/TokenManager;", "getTokenManager", "()Leu/cyben/guard/utils/TokenManager;", "setTokenManager", "(Leu/cyben/guard/utils/TokenManager;)V", "tts", "Landroid/speech/tts/TextToSpeech;", "ttsReady", "checkAndRequestPermissions", "", "loadUser", "logout", "offerCallScreeningSetup", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onInit", "status", "", "onResume", "sendMessage", "text", "setupChat", "setupNav", "setupSpeechRecognizer", "showPermissionDeniedDialog", "speak", "toggleListening", "app_debug"})
public final class DashboardActivity extends androidx.appcompat.app.AppCompatActivity implements android.speech.tts.TextToSpeech.OnInitListener {
    @javax.inject.Inject()
    public eu.cyben.guard.data.api.ApiService api;
    @javax.inject.Inject()
    public eu.cyben.guard.utils.TokenManager tokenManager;
    private eu.cyben.guard.databinding.ActivityDashboardBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<eu.cyben.guard.data.models.ChatMessage> messages = null;
    private eu.cyben.guard.ui.dashboard.ChatAdapter adapter;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.util.Map<java.lang.String, java.lang.String>> history = null;
    @org.jetbrains.annotations.Nullable()
    private eu.cyben.guard.data.models.GuardUser currentUser;
    private boolean permissionsRequested = false;
    @org.jetbrains.annotations.Nullable()
    private android.speech.tts.TextToSpeech tts;
    private boolean ttsReady = false;
    @org.jetbrains.annotations.Nullable()
    private android.speech.SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> permissionLauncher = null;
    
    public DashboardActivity() {
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
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @java.lang.Override()
    public void onInit(int status) {
    }
    
    private final void speak(java.lang.String text) {
    }
    
    private final void setupSpeechRecognizer() {
    }
    
    private final void toggleListening() {
    }
    
    private final void checkAndRequestPermissions() {
    }
    
    private final void offerCallScreeningSetup() {
    }
    
    private final void showPermissionDeniedDialog() {
    }
    
    private final void setupChat() {
    }
    
    private final void sendMessage(java.lang.String text) {
    }
    
    private final void loadUser() {
    }
    
    private final void setupNav() {
    }
    
    private final void logout() {
    }
}