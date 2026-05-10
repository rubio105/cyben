package eu.cyben.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import eu.cyben.mobile.R
import eu.cyben.mobile.api.CybenApi
import eu.cyben.mobile.databinding.ActivityQrScannerBinding
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class QRScannerActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var cybenApi: CybenApi

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnFlash.setOnClickListener { toggleFlash() }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrContent ->
                        if (!isProcessing) {
                            isProcessing = true
                            runOnUiThread {
                                analyzeQRContent(qrContent)
                            }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, R.string.camera_error, Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlash() {
        // Flash toggle implementation
    }

    private fun analyzeQRContent(content: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.analyzing_url)

        if (content.startsWith("http://") || content.startsWith("https://")) {
            checkUrlSafety(content)
        } else {
            showResult(content, isSafe = true, isUrl = false)
        }
    }

    private fun checkUrlSafety(url: String) {
        scope.launch {
            try {
                val request = eu.cyben.mobile.api.CheckUrlsRequest(urls = listOf(url))
                val response = withContext(Dispatchers.IO) {
                    cybenApi.checkUrls(request)
                }

                val result = response.body()?.results?.firstOrNull()
                val isSafe = result?.let { !it.isPhishing && !it.isMalware } ?: true
                val threat = result?.threatType

                showResult(url, isSafe, isUrl = true, threatType = threat)

            } catch (e: Exception) {
                showResult(url, isSafe = true, isUrl = true, error = e.message)
            }
        }
    }

    private fun showResult(
        content: String,
        isSafe: Boolean,
        isUrl: Boolean,
        threatType: String? = null,
        error: String? = null
    ) {
        binding.progressBar.visibility = View.GONE

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isSafe) R.string.qr_safe else R.string.qr_danger)
            .setCancelable(false)

        if (error != null) {
            dialog.setMessage(getString(R.string.qr_check_error, content))
                .setIcon(R.drawable.ic_shield_warning)
        } else if (!isSafe) {
            dialog.setMessage(getString(R.string.qr_threat_detected, content, threatType ?: "Unknown"))
                .setIcon(R.drawable.ic_shield_warning)
        } else if (isUrl) {
            dialog.setMessage(getString(R.string.qr_url_safe, content))
                .setIcon(R.drawable.ic_shield_check)
        } else {
            dialog.setMessage(getString(R.string.qr_content, content))
                .setIcon(R.drawable.ic_qr_code)
        }

        if (isUrl && isSafe) {
            dialog.setPositiveButton(R.string.open_url) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                startActivity(intent)
                finish()
            }
        }

        dialog.setNegativeButton(R.string.scan_another) { _, _ ->
            isProcessing = false
            binding.tvStatus.text = getString(R.string.point_camera_at_qr)
        }

        dialog.setNeutralButton(R.string.close) { _, _ ->
            finish()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        scope.cancel()
    }

    private class QRCodeAnalyzer(private val onQRCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_URL || 
                                barcode.valueType == Barcode.TYPE_TEXT) {
                                barcode.rawValue?.let { onQRCodeDetected(it) }
                                break
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
