package com.example.notionbookshelfbarcodereader
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.notionbookshelfbarcodereader.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.common.Barcode

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var codeScanner: CodeScanner
    private val launcher = registerForActivityResult(
        CameraPermission.RequestContract(), ::onPermissionResult
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        codeScanner = CodeScanner(this, binding.previewView, ::onDetectCode)

        if (CameraPermission.hasPermission(this)) {
            start()
        } else {
            launcher.launch(Unit)
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            start()
        } else {
            finish()
        }
    }

    private fun start() {
        codeScanner.start()
    }

    private fun onDetectCode(codes: List<Barcode>) {
        codes.forEach {
            Toast.makeText(this, it.rawValue, Toast.LENGTH_LONG).show()
        }
    }
}