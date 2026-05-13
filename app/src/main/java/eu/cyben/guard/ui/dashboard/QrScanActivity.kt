package eu.cyben.guard.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QrScanActivity : AppCompatActivity() {

    private var scanned = false

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else { Toast.makeText(this, "Camera necessaria", Toast.LENGTH_SHORT).show(); finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preview = PreviewView(this).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
        setContentView(preview)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            startCamera(preview)
        else
            cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera(previewView: PreviewView? = null) {
        val view = previewView ?: (window.decorView.rootView as? PreviewView) ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            val scanner = BarcodeScanning.getClient()
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                if (!scanned) {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { it.rawValue != null }?.let { barcode ->
                                    if (!scanned) {
                                        scanned = true
                                        val result = barcode.rawValue ?: ""
                                        val intent = Intent().apply { putExtra("qr_result", result) }
                                        setResult(Activity.RESULT_OK, intent)
                                        finish()
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else imageProxy.close()
                } else imageProxy.close()
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }
}
