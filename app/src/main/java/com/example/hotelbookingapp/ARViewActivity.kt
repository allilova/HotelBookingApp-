package com.example.hotelbookingapp

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageView
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ARViewActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val REQUEST_CAMERA   = 200
        private const val REQUEST_LOCATION = 201
    }

    private val locationViewModel: LocationViewModel by viewModels()

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null


    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)


    private var hotelLat = 0.0
    private var hotelLon = 0.0

    // Views
    private lateinit var tvDistance: TextView
    private lateinit var tvBearing: TextView
    private lateinit var ivArrow: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arview)

        val hotelName = intent.getStringExtra("HOTEL_NAME") ?: ""
        hotelLat      = intent.getDoubleExtra("HOTEL_LAT", 0.0)
        hotelLon      = intent.getDoubleExtra("HOTEL_LON", 0.0)

        findViewById<TextView>(R.id.arHotelName).text = hotelName
        tvDistance = findViewById(R.id.arDistance)
        tvBearing  = findViewById(R.id.arBearing)
        ivArrow    = findViewById(R.id.arArrow)

        // ── Sensor setup ──────────────────────────────────────────────
        sensorManager  = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

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

        // ── Observe live distance + arrow rotation ─────────────────────
        lifecycleScope.launch {
            locationViewModel.location.collect { userLoc ->
                if (userLoc == null || (hotelLat == 0.0 && hotelLon == 0.0)) return@collect

                val km = locationViewModel.distanceKm(
                    userLoc.latitude, userLoc.longitude, hotelLat, hotelLon
                )
                tvDistance.text = getString(R.string.distance_from_you, km)


                userBearingToHotel = bearingTo(
                    userLoc.latitude, userLoc.longitude, hotelLat, hotelLon
                )
                updateArrow()
            }
        }
    }

    // ── Compass ───────────────────────────────────────────────────────


    private var userBearingToHotel = 0f


    private var deviceHeading = 0f

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        // orientationAngles[0] = azimuth in radians
        deviceHeading = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            .let { if (it < 0) it + 360f else it }
        updateArrow()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit


    private fun updateArrow() {
        val relative = (userBearingToHotel - deviceHeading + 360f) % 360f
        ivArrow.rotation = relative


        tvBearing.text = getString(R.string.ar_bearing, cardinalDirection(relative))
    }


    private fun bearingTo(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x)).toFloat()
        return (bearing + 360f) % 360f
    }

    private fun cardinalDirection(degrees: Float): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((degrees + 22.5f) / 45f).toInt() % 8]
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationViewModel.startTracking(this)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        locationViewModel.stopTracking()
    }

    // ── Camera ────────────────────────────────────────────────────────

    private fun startCamera() {
        val future = try {
            ProcessCameraProvider.getInstance(this)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.camera_start_failed, e.message),
                Toast.LENGTH_SHORT).show()
            return
        }
        future.addListener({
            try {
                val provider  = future.get()
                val previewView = findViewById<PreviewView>(R.id.viewFinder)
                val preview   = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.camera_start_failed, e.message),
                    Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA ->
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) startCamera()
                else Toast.makeText(this,
                    getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
            REQUEST_LOCATION ->
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    locationViewModel.startTracking(this)
        }
    }
}