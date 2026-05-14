package eu.cyben.guard.ui.dashboard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.data.api.AnalyzeRequest;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.data.api.PhoneCheckRequest;
import eu.cyben.guard.data.models.ChatMessage;
import eu.cyben.guard.data.models.GuardUser;
import eu.cyben.guard.data.models.ImageAnalyzeRequest;
import eu.cyben.guard.databinding.ActivityDashboardBinding;
import eu.cyben.guard.ui.auth.LoginActivity;
import eu.cyben.guard.ui.breach.BreachMonitorActivity;
import eu.cyben.guard.ui.settings.SettingsActivity;
import eu.cyben.guard.ui.subscription.SubscriptionActivity;
import eu.cyben.guard.ui.vpn.VPNActivity;
import eu.cyben.guard.utils.LocaleHelper;
import eu.cyben.guard.utils.TokenManager;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0080\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\r\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010)\u001a\u00020*2\u0006\u0010+\u001a\u00020,H\u0002J\u0010\u0010-\u001a\u00020*2\u0006\u0010.\u001a\u00020\u0015H\u0002J\b\u0010/\u001a\u00020*H\u0002J\b\u00100\u001a\u00020*H\u0002J\b\u00101\u001a\u00020*H\u0002J\b\u00102\u001a\u00020*H\u0002J\u0012\u00103\u001a\u00020*2\b\u00104\u001a\u0004\u0018\u000105H\u0014J\b\u00106\u001a\u00020*H\u0014J\b\u00107\u001a\u00020*H\u0014J\b\u00108\u001a\u00020*H\u0002J\u0010\u00109\u001a\u00020*2\u0006\u0010:\u001a\u00020\u0015H\u0002J\b\u0010;\u001a\u00020*H\u0002J\b\u0010<\u001a\u00020*H\u0002J\b\u0010=\u001a\u00020*H\u0002J\b\u0010>\u001a\u00020*H\u0002J\b\u0010?\u001a\u00020*H\u0002J\b\u0010@\u001a\u00020*H\u0002J\b\u0010A\u001a\u00020*H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0005\u001a\u00020\u00068\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R \u0010\u0012\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u00150\u00140\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0016\u001a\u0010\u0012\f\u0012\n \u0018*\u0004\u0018\u00010\u00150\u00150\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u001c0\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u001d\u001a\u0010\u0012\f\u0012\n \u0018*\u0004\u0018\u00010\u001e0\u001e0\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001f\u001a\u0004\u0018\u00010 X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010!\u001a\u00020\"8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b#\u0010$\"\u0004\b%\u0010&R\u0010\u0010\'\u001a\u0004\u0018\u00010(X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006B"}, d2 = {"Leu/cyben/guard/ui/dashboard/DashboardActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Leu/cyben/guard/ui/dashboard/ChatAdapter;", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "binding", "Leu/cyben/guard/databinding/ActivityDashboardBinding;", "currentUser", "Leu/cyben/guard/data/models/GuardUser;", "dailyLimit", "", "dailyUsed", "history", "", "", "", "imagePickerLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "kotlin.jvm.PlatformType", "isListening", "", "messages", "Leu/cyben/guard/data/models/ChatMessage;", "qrLauncher", "Landroid/content/Intent;", "speechRecognizer", "Landroid/speech/SpeechRecognizer;", "tokenManager", "Leu/cyben/guard/utils/TokenManager;", "getTokenManager", "()Leu/cyben/guard/utils/TokenManager;", "setTokenManager", "(Leu/cyben/guard/utils/TokenManager;)V", "tts", "Landroid/speech/tts/TextToSpeech;", "analyzeImageUri", "", "uri", "Landroid/net/Uri;", "checkPhoneNumber", "number", "initTts", "loadSecurityScore", "loadUser", "logout", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onResume", "requestRuntimePermissions", "sendMessage", "text", "setupChat", "setupNav", "setupQuickActions", "showPhoneCheckDialog", "showUpgradeDialog", "startListening", "stopListening", "app_release"})
public final class DashboardActivity extends androidx.appcompat.app.AppCompatActivity {
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
    @org.jetbrains.annotations.Nullable()
    private android.speech.tts.TextToSpeech tts;
    @org.jetbrains.annotations.Nullable()
    private android.speech.SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private int dailyUsed = 0;
    private int dailyLimit = 10;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> imagePickerLauncher = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> qrLauncher = null;
    
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
    
    private final void initTts() {
    }
    
    private final void setupChat() {
    }
    
    private final void setupQuickActions() {
    }
    
    private final void setupNav() {
    }
    
    private final void loadUser() {
    }
    
    private final void loadSecurityScore() {
    }
    
    private final void sendMessage(java.lang.String text) {
    }
    
    private final void analyzeImageUri(android.net.Uri uri) {
    }
    
    private final void showPhoneCheckDialog() {
    }
    
    private final void checkPhoneNumber(java.lang.String number) {
    }
    
    private final void startListening() {
    }
    
    private final void stopListening() {
    }
    
    private final void showUpgradeDialog() {
    }
    
    private final void requestRuntimePermissions() {
    }
    
    private final void logout() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}