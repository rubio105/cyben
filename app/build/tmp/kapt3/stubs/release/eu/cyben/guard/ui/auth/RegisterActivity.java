package eu.cyben.guard.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.R;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.data.api.RegisterRequest;
import eu.cyben.guard.databinding.ActivityRegisterBinding;
import eu.cyben.guard.ui.dashboard.DashboardActivity;
import eu.cyben.guard.utils.TokenManager;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0013\u001a\u00020\u0014H\u0002J\u0012\u0010\u0015\u001a\u00020\u00142\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0014J\u0010\u0010\u0018\u001a\u00020\u00142\u0006\u0010\u0019\u001a\u00020\fH\u0002J\b\u0010\u001a\u001a\u00020\u0014H\u0002J\u0010\u0010\u001b\u001a\u00020\u00142\u0006\u0010\u001c\u001a\u00020\u001dH\u0002R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\r\u001a\u00020\u000e8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012\u00a8\u0006\u001e"}, d2 = {"Leu/cyben/guard/ui/auth/RegisterActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "binding", "Leu/cyben/guard/databinding/ActivityRegisterBinding;", "passwordVisible", "", "tokenManager", "Leu/cyben/guard/utils/TokenManager;", "getTokenManager", "()Leu/cyben/guard/utils/TokenManager;", "setTokenManager", "(Leu/cyben/guard/utils/TokenManager;)V", "doRegister", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "setLoading", "b", "togglePassword", "updateStrength", "pwd", "", "app_release"})
public final class RegisterActivity extends androidx.appcompat.app.AppCompatActivity {
    @javax.inject.Inject()
    public eu.cyben.guard.data.api.ApiService api;
    @javax.inject.Inject()
    public eu.cyben.guard.utils.TokenManager tokenManager;
    private eu.cyben.guard.databinding.ActivityRegisterBinding binding;
    private boolean passwordVisible = false;
    
    public RegisterActivity() {
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
    
    private final void togglePassword() {
    }
    
    private final void updateStrength(java.lang.String pwd) {
    }
    
    private final void doRegister() {
    }
    
    private final void setLoading(boolean b) {
    }
}