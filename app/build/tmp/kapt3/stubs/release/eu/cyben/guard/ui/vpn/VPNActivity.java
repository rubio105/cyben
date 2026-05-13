package eu.cyben.guard.ui.vpn;

import android.net.Ikev2VpnProfile;
import android.net.VpnManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.R;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.databinding.ActivityVpnBinding;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0007\u0018\u0000 \u001f2\u00020\u0001:\u0001\u001fB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0011\u001a\u00020\u0012H\u0002J\b\u0010\u0013\u001a\u00020\u0012H\u0002J\b\u0010\u0014\u001a\u00020\u0012H\u0002J\u0012\u0010\u0015\u001a\u00020\u00122\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0014J\u0018\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001aH\u0003J\u0010\u0010\u001c\u001a\u00020\u00122\u0006\u0010\u001d\u001a\u00020\fH\u0002J\b\u0010\u001e\u001a\u00020\u0012H\u0003R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\r\u001a\u0010\u0012\f\u0012\n \u0010*\u0004\u0018\u00010\u000f0\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Leu/cyben/guard/ui/vpn/VPNActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "binding", "Leu/cyben/guard/databinding/ActivityVpnBinding;", "vpnConnected", "", "vpnPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "Landroid/content/Intent;", "kotlin.jvm.PlatformType", "connectVpn", "", "disconnectVpn", "loadVpnStats", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "provisionAndConnect", "username", "", "password", "setConnectedState", "connected", "startVpn", "Companion", "app_release"})
public final class VPNActivity extends androidx.appcompat.app.AppCompatActivity {
    @javax.inject.Inject()
    public eu.cyben.guard.data.api.ApiService api;
    private eu.cyben.guard.databinding.ActivityVpnBinding binding;
    private boolean vpnConnected = false;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> vpnPermissionLauncher = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String VPN_SERVER = "vpn.cyben.eu";
    @org.jetbrains.annotations.NotNull()
    public static final eu.cyben.guard.ui.vpn.VPNActivity.Companion Companion = null;
    
    public VPNActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final eu.cyben.guard.data.api.ApiService getApi() {
        return null;
    }
    
    public final void setApi(@org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.ApiService p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void connectVpn() {
    }
    
    @androidx.annotation.RequiresApi(value = android.os.Build.VERSION_CODES.S)
    private final void provisionAndConnect(java.lang.String username, java.lang.String password) {
    }
    
    @androidx.annotation.RequiresApi(value = android.os.Build.VERSION_CODES.S)
    private final void startVpn() {
    }
    
    private final void disconnectVpn() {
    }
    
    private final void setConnectedState(boolean connected) {
    }
    
    private final void loadVpnStats() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Leu/cyben/guard/ui/vpn/VPNActivity$Companion;", "", "()V", "VPN_SERVER", "", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}