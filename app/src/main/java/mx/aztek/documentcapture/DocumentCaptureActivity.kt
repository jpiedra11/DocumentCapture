package mx.aztek.documentcapture

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mx.aztek.documentcapture.GlobalVariables.imagePath
import mx.aztek.documentcapture.databinding.ActivityDocumentCaptureBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class DocumentCaptureActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityDocumentCaptureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()

        binding.closeButton.setOnClickListener {

            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(50)

            val resId = resources.getIdentifier("click", "raw", packageName)
            val mp = MediaPlayer.create(this, resId)
            mp.setVolume(1000F, 1000F)

            mp.setOnPreparedListener(MediaPlayer.OnPreparedListener { mp -> mp.start() })

            mp.setOnCompletionListener(MediaPlayer.OnCompletionListener { mp -> mp.stop() })

            takePhoto()
        }

        outputDirectory = getOutputDirectory()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "DocumentCapture").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraExecutor.shutdown()
    }

    /**
     * This function is executed once the user has granted or denied the missing permission
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkIfCameraPermissionIsGranted()
    }

    /**
     * This function is responsible to request the required CAMERA permission
     */
    private fun checkCameraPermission() {
        try {
            val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
            ActivityCompat.requestPermissions(this, requiredPermissions, 0)
        } catch (e: IllegalArgumentException) {
            checkIfCameraPermissionIsGranted()
        }
    }

    /**
     * This function will check if the CAMERA permission has been granted.
     * If so, it will call the function responsible to initialize the camera preview.
     * Otherwise, it will raise an alert.
     */
    private fun checkIfCameraPermissionIsGranted() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            // Permission denied
            MaterialAlertDialogBuilder(this)
                .setTitle("Permission required")
                .setMessage("This application needs to access the camera to process barcodes")
                .setPositiveButton("Ok") { _, _ ->
                    // Keep asking for permission until granted
                    checkCameraPermission()
                }
                .setCancelable(false)
                .create()
                .apply {
                    setCanceledOnTouchOutside(false)
                    show()
                }
        }
    }

    /**
     * This function is responsible for the setup of the camera preview and the image analyzer.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                val viewport = findViewById<PreviewView>(R.id.preview_view).viewPort

                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture!!)
                    .setViewPort(viewport!!)
                    .build()
                cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
            ).format(System.currentTimeMillis()) + ".png"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("TAG", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    imageCaptured(
                        photoFile.absolutePath
                    )
                }
            }
        )
    }

    fun imageCaptured(base64Image: String) {
        imagePath = base64Image
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
