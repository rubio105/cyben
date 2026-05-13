package eu.cyben.guard.ui.dashboard;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import eu.cyben.guard.R;
import eu.cyben.guard.data.models.ChatMessage;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\f\u0012\b\u0012\u00060\u0002R\u00020\u00000\u0001:\u0001\u0011B\u0013\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\u0002\u0010\u0006J\b\u0010\u0007\u001a\u00020\bH\u0016J\u001c\u0010\t\u001a\u00020\n2\n\u0010\u000b\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\f\u001a\u00020\bH\u0016J\u001c\u0010\r\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\bH\u0016R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Leu/cyben/guard/ui/dashboard/ChatAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Leu/cyben/guard/ui/dashboard/ChatAdapter$VH;", "messages", "", "Leu/cyben/guard/data/models/ChatMessage;", "(Ljava/util/List;)V", "getItemCount", "", "onBindViewHolder", "", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "VH", "app_debug"})
public final class ChatAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<eu.cyben.guard.ui.dashboard.ChatAdapter.VH> {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<eu.cyben.guard.data.models.ChatMessage> messages = null;
    
    public ChatAdapter(@org.jetbrains.annotations.NotNull()
    java.util.List<eu.cyben.guard.data.models.ChatMessage> messages) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public eu.cyben.guard.ui.dashboard.ChatAdapter.VH onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    eu.cyben.guard.ui.dashboard.ChatAdapter.VH holder, int position) {
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0086\u0004\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u000b"}, d2 = {"Leu/cyben/guard/ui/dashboard/ChatAdapter$VH;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "tv", "Landroid/widget/TextView;", "container", "Landroid/widget/LinearLayout;", "(Leu/cyben/guard/ui/dashboard/ChatAdapter;Landroid/widget/TextView;Landroid/widget/LinearLayout;)V", "getContainer", "()Landroid/widget/LinearLayout;", "getTv", "()Landroid/widget/TextView;", "app_debug"})
    public final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView tv = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.LinearLayout container = null;
        
        public VH(@org.jetbrains.annotations.NotNull()
        android.widget.TextView tv, @org.jetbrains.annotations.NotNull()
        android.widget.LinearLayout container) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getTv() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.LinearLayout getContainer() {
            return null;
        }
    }
}