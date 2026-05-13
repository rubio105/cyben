package eu.cyben.guard.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import eu.cyben.guard.databinding.ActivityQrScanBinding;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0003J\u0012\u0010\r\u001a\u00020\n2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0014J\b\u0010\u0010\u001a\u00020\nH\u0014J\b\u0010\u0011\u001a\u00020\nH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Leu/cyben/guard/ui/dashboard/QrScanActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Leu/cyben/guard/databinding/ActivityQrScanBinding;", "cameraExecutor", "Ljava/util/concurrent/ExecutorService;", "scanned", "", "analyzeImage", "", "imageProxy", "Landroidx/camera/core/ImageProxy;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "startCamera", "app_debug"})
public final class QrScanActivity extends androidx.appcompat.app.AppCompatActivity {
    private eu.cyben.guard.databinding.ActivityQrScanBinding binding;
    private java.util.concurrent.ExecutorService cameraExecutor;
    private boolean scanned = false;
    
    public QrScanActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void startCamera() {
    }
    
    @androidx.annotation.OptIn(markerClass = {androidx.camera.core.ExperimentalGetImage.class})
    private final void analyzeImage(androidx.camera.core.ImageProxy imageProxy) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}