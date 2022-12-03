package com.example.notionbookshelfbarcodereader
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CodeScanner(
    private val activity: ComponentActivity,
    private val previewView: PreviewView,
    callback: (List<Barcode>) -> Unit
) {
    private val workerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()
    private val analyzer: CodeAnalyzer = CodeAnalyzer(scanner, callback)

    init {
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Event.ON_DESTROY) {
                    workerExecutor.shutdown()
                    scanner.close()
                }
            }
        )
    }

    fun start() {
        ProcessCameraProvider.getInstance(activity)
            .let {
                it.addListener({
                    setUp(it.get())
                }, ContextCompat.getMainExecutor(activity))
            }
    }

    private fun setUp(provider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(workerExecutor, analyzer)
            }

        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(
                activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }
    }
}

