package eu.cyben.guard.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.data.api.EmailRequest;
import eu.cyben.guard.databinding.ActivityEmailVerificationBinding;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000eH\u0014R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Leu/cyben/guard/ui/auth/EmailVerificationActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "binding", "Leu/cyben/guard/databinding/ActivityEmailVerificationBinding;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "app_release"})
public final class EmailVerificationActivity extends androidx.appcompat.app.AppCompatActivity {
    @javax.inject.Inject()
    public eu.cyben.guard.data.api.ApiService api;
    private eu.cyben.guard.databinding.ActivityEmailVerificationBinding binding;
    
    public EmailVerificationActivity() {
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
}