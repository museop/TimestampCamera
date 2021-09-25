package com.museop.timestampcamera

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionGranted()) {
            startCamera()
        } else {
            requestRequiredPermissions()
        }

        val switchButton = findViewById<Button>(R.id.buttonSwitch)
        switchButton.setOnClickListener { switchCamera() }

        val captureButton = findViewById<Button>(R.id.buttonCamera)
        captureButton.setOnClickListener { takePhoto() }

        val galleryButton = findViewById<Button>(R.id.buttonGallery)
        galleryButton.setOnClickListener { openGallery() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun switchCamera() {
        if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA)
            lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA
        else if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA)
            lensFacing = CameraSelector.DEFAULT_BACK_CAMERA

        startCamera()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val contentValues = ContentValues().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, IMAGE_SAVE_PATH)
            }
            put(MediaStore.Images.Media.DISPLAY_NAME, SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        outputFileResults.savedUri?.let { openEdit(it) }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    }
                })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val viewFinder = findViewById<PreviewView>(R.id.viewFinder)
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, lensFacing, preview, imageCapture)
            } catch (exception: Exception) {
                Log.e(TAG, "Use case binding failed", exception)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    private fun openEdit(uri: Uri?) {
        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("uri", uri)
        editResultLauncher.launch(intent)
    }

    private val editResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val returnValue = it.data?.getIntExtra("returnValue", 0)
            Log.d(TAG, "MainActivity removeImage: $returnValue")
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        galleryResultLauncher.launch(intent)
    }

    private val galleryResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data: Intent? = it.data
            val uri = data?.data
            Log.d(TAG, "gallery uri: $uri")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "TimestampCamera"
        private const val IMAGE_SAVE_PATH = "DCIM/MyCamera"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}