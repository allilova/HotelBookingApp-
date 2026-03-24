package com.example.hotelbookingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ARViewActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arview)

        val hotelName = intent.getStringExtra("HOTEL_NAME") ?: ""
        findViewById<TextView>(R.id.arHotelName).text = hotelName

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = try {
            ProcessCameraProvider.getInstance(this)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.camera_start_failed, e.message),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val previewView = findViewById<PreviewView>(R.id.viewFinder)
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    getString(R.string.camera_start_failed, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when {
            requestCode == REQUEST_CAMERA &&
                    grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED -> startCamera()

            else -> Toast.makeText(
                this,
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}