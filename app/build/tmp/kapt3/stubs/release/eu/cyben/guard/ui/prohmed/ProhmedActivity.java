package eu.cyben.guard.ui.prohmed;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.data.models.ProhmedActivateRequest;
import eu.cyben.guard.data.models.ProhmedConsult;
import eu.cyben.guard.data.models.ProhmedConsultRequest;
import eu.cyben.guard.data.models.ProhmedStatus;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0002J\u0010\u0010\u0013\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u0015H\u0002J&\u0010\u0016\u001a\u00020\u00102\u0006\u0010\u0017\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u00192\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00100\u001bH\u0002J\u0010\u0010\u001c\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u0015H\u0002J\u0018\u0010\u001d\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u0019H\u0002J\u0010\u0010\u001e\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u0015H\u0002J\b\u0010\u001f\u001a\u00020\u0010H\u0002J\b\u0010 \u001a\u00020\u0010H\u0002J\u0012\u0010!\u001a\u00020\u00102\b\u0010\"\u001a\u0004\u0018\u00010#H\u0014J\u0010\u0010$\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020%H\u0002J\u0010\u0010&\u001a\u00020\u00102\u0006\u0010\'\u001a\u00020(H\u0002J\b\u0010)\u001a\u00020\u0010H\u0002J\u0010\u0010*\u001a\u00020\u00102\u0006\u0010\'\u001a\u00020(H\u0002J\b\u0010+\u001a\u00020\u0010H\u0002J\u0016\u0010,\u001a\u00020\u00102\f\u0010-\u001a\b\u0012\u0004\u0012\u00020/0.H\u0002J\u0010\u00100\u001a\u00020\u00102\u0006\u00101\u001a\u00020\u0015H\u0002R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u00062"}, d2 = {"Leu/cyben/guard/ui/prohmed/ProhmedActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "container", "Landroid/widget/LinearLayout;", "progress", "Landroid/widget/ProgressBar;", "scroll", "Landroid/widget/ScrollView;", "activate", "", "req", "Leu/cyben/guard/data/models/ProhmedActivateRequest;", "addBullet", "text", "", "addButton", "label", "color", "", "action", "Lkotlin/Function0;", "addText", "addTextColored", "addTitle", "loadConsults", "loadStatus", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "sendConsult", "Leu/cyben/guard/data/models/ProhmedConsultRequest;", "showActivatedUI", "status", "Leu/cyben/guard/data/models/ProhmedStatus;", "showActivationDialog", "showActivationUI", "showConsultDialog", "showConsultsList", "list", "", "Leu/cyben/guard/data/models/ProhmedConsult;", "showError", "msg", "app_release"})
public final class ProhmedActivity extends androidx.appcompat.app.AppCompatActivity {
    @javax.inject.Inject()
    public eu.cyben.guard.data.api.ApiService api;
    private android.widget.ScrollView scroll;
    private android.widget.LinearLayout container;
    private android.widget.ProgressBar progress;
    
    public ProhmedActivity() {
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
    
    private final void loadStatus() {
    }
    
    private final void showActivationUI(eu.cyben.guard.data.models.ProhmedStatus status) {
    }
    
    private final void showActivatedUI(eu.cyben.guard.data.models.ProhmedStatus status) {
    }
    
    private final void showActivationDialog() {
    }
    
    private final void activate(eu.cyben.guard.data.models.ProhmedActivateRequest req) {
    }
    
    private final void showConsultDialog() {
    }
    
    private final void sendConsult(eu.cyben.guard.data.models.ProhmedConsultRequest req) {
    }
    
    private final void loadConsults() {
    }
    
    private final void showConsultsList(java.util.List<eu.cyben.guard.data.models.ProhmedConsult> list) {
    }
    
    private final void showError(java.lang.String msg) {
    }
    
    private final void addTitle(java.lang.String text) {
    }
    
    private final void addText(java.lang.String text) {
    }
    
    private final void addTextColored(java.lang.String text, int color) {
    }
    
    private final void addBullet(java.lang.String text) {
    }
    
    private final void addButton(java.lang.String label, int color, kotlin.jvm.functions.Function0<kotlin.Unit> action) {
    }
}