package eu.cyben.guard.ui.analysis;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.data.models.GuardAnalysis;
import eu.cyben.guard.databinding.ActivityAnalysisHistoryBinding;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001:\u0001\u0010B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000b\u001a\u00020\fH\u0002J\u0012\u0010\r\u001a\u00020\f2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0014R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Leu/cyben/guard/ui/analysis/AnalysisHistoryActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "api", "Leu/cyben/guard/data/api/ApiService;", "getApi", "()Leu/cyben/guard/data/api/ApiService;", "setApi", "(Leu/cyben/guard/data/api/ApiService;)V", "binding", "Leu/cyben/guard/databinding/ActivityAnalysisHistoryBinding;", "loadHistory", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "AnalysisAdapter", "app_debug"})
public final class AnalysisHistoryActivity extends androidx.appcompat.app.AppCompatActivity {
    @javax.inject.Inject()
    public eu.cyben.guard.data.api.ApiService api;
    private eu.cyben.guard.databinding.ActivityAnalysisHistoryBinding binding;
    
    public AnalysisHistoryActivity() {
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
    
    private final void loadHistory() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0004\u0018\u00002\u0010\u0012\f\u0012\n0\u0002R\u00060\u0000R\u00020\u00030\u0001:\u0001\u0012B\u0013\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\u0002\u0010\u0007J\b\u0010\b\u001a\u00020\tH\u0016J \u0010\n\u001a\u00020\u000b2\u000e\u0010\f\u001a\n0\u0002R\u00060\u0000R\u00020\u00032\u0006\u0010\r\u001a\u00020\tH\u0016J \u0010\u000e\u001a\n0\u0002R\u00060\u0000R\u00020\u00032\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\tH\u0016R\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Leu/cyben/guard/ui/analysis/AnalysisHistoryActivity$AnalysisAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Leu/cyben/guard/ui/analysis/AnalysisHistoryActivity$AnalysisAdapter$VH;", "Leu/cyben/guard/ui/analysis/AnalysisHistoryActivity;", "items", "", "Leu/cyben/guard/data/models/GuardAnalysis;", "(Leu/cyben/guard/ui/analysis/AnalysisHistoryActivity;Ljava/util/List;)V", "getItemCount", "", "onBindViewHolder", "", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "VH", "app_debug"})
    public final class AnalysisAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<eu.cyben.guard.ui.analysis.AnalysisHistoryActivity.AnalysisAdapter.VH> {
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<eu.cyben.guard.data.models.GuardAnalysis> items = null;
        
        public AnalysisAdapter(@org.jetbrains.annotations.NotNull()
        java.util.List<eu.cyben.guard.data.models.GuardAnalysis> items) {
            super();
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public eu.cyben.guard.ui.analysis.AnalysisHistoryActivity.AnalysisAdapter.VH onCreateViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.ViewGroup parent, int viewType) {
            return null;
        }
        
        @java.lang.Override()
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
        eu.cyben.guard.ui.analysis.AnalysisHistoryActivity.AnalysisAdapter.VH holder, int position) {
        }
        
        @java.lang.Override()
        public int getItemCount() {
            return 0;
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Leu/cyben/guard/ui/analysis/AnalysisHistoryActivity$AnalysisAdapter$VH;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "layout", "Landroid/widget/LinearLayout;", "(Leu/cyben/guard/ui/analysis/AnalysisHistoryActivity$AnalysisAdapter;Landroid/widget/LinearLayout;)V", "getLayout", "()Landroid/widget/LinearLayout;", "app_debug"})
        public final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            @org.jetbrains.annotations.NotNull()
            private final android.widget.LinearLayout layout = null;
            
            public VH(@org.jetbrains.annotations.NotNull()
            android.widget.LinearLayout layout) {
                super(null);
            }
            
            @org.jetbrains.annotations.NotNull()
            public final android.widget.LinearLayout getLayout() {
                return null;
            }
        }
    }
}