package com.example.hotelbookingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ARViewActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA = 200
        private const val REQUEST_LOCATION = 201
    }

    private val locationViewModel: LocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arview)

        val hotelName = intent.getStringExtra("HOTEL_NAME") ?: ""
        val hotelLat  = intent.getDoubleExtra("HOTEL_LAT", 0.0)
        val hotelLon  = intent.getDoubleExtra("HOTEL_LON", 0.0)

        findViewById<TextView>(R.id.arHotelName).text = hotelName

        // ── Camera ────────────────────────────────────────────────────
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA
            )
        }

        // ── Location ──────────────────────────────────────────────────
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationViewModel.startTracking(this)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
        }

        // ── Observe live distance ─────────────────────────────────────
        val tvDistance = findViewById<TextView>(R.id.arDistance)
        lifecycleScope.launch {
            locationViewModel.location.collect { userLoc ->
                if (userLoc == null) return@collect
                if (hotelLat == 0.0 && hotelLon == 0.0) return@collect
                val km = locationViewModel.distanceKm(
                    userLoc.latitude, userLoc.longitude, hotelLat, hotelLon
                )
                tvDistance.text = getString(R.string.distance_from_you, km)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = try {
            ProcessCameraProvider.getInstance(this)
        } catch (e: Exception) {
            Toast.makeText(this,
                getString(R.string.camera_start_failed, e.message),
                Toast.LENGTH_SHORT).show()
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
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview
                )
            } catch (e: Exception) {
                Toast.makeText(this,
                    getString(R.string.camera_start_failed, e.message),
                    Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationViewModel.startTracking(this)
        }
    }

    override fun onStop() {
        super.onStop()
        locationViewModel.stopTracking()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA ->
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) startCamera()
                else Toast.makeText(this,
                    getString(R.string.camera_permission_denied),
                    Toast.LENGTH_SHORT).show()

            REQUEST_LOCATION ->
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    locationViewModel.startTracking(this)
        }
    }
}