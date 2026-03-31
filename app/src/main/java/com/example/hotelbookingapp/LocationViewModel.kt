package com.example.hotelbookingapp

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationViewModel(app: Application) : AndroidViewModel(app) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(app)

    // Fallback plain LocationManager (used only if Fused is unavailable)
    private val locationManager =
        app.getSystemService(LocationManager::class.java)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    // ── Fused callback ────────────────────────────────────────────────
    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { _location.value = it }
        }
    }

    // ── Plain LocationManager fallback ────────────────────────────────
    private val legacyListener = LocationListener { loc ->
        _location.value = loc
    }

    private var usingFused = false

    /**
     * Call from onStart() (after permission check).
     * Tries FusedLocationProviderClient first; falls back to LocationManager.
     */
    fun startTracking(context: android.content.Context) {
        if (!hasPermission(context)) return

        viewModelScope.launch {
            try {
                // Emit last known immediately so UI doesn't wait for first fix
                val last = fusedClient.lastLocation.await()
                if (last != null) _location.value = last

                // Request live updates every 5 s, high accuracy
                val request = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 5_000L
                )
                    .setMinUpdateDistanceMeters(10f)
                    .build()

                fusedClient.requestLocationUpdates(
                    request, fusedCallback, Looper.getMainLooper()
                )
                usingFused = true

            } catch (e: Exception) {
                // FusedClient not available (no Play Services) — use legacy
                startLegacyTracking(context)
            }
        }
    }

    private fun startLegacyTracking(context: android.content.Context) {
        if (!hasPermission(context)) return
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                -> LocationManager.NETWORK_PROVIDER
            else -> return
        }
        locationManager.getLastKnownLocation(provider)?.let { _location.value = it }
        locationManager.requestLocationUpdates(
            provider, 5_000L, 10f, legacyListener
        )
    }

    fun stopTracking() {
        if (usingFused) {
            fusedClient.removeLocationUpdates(fusedCallback)
        } else {
            locationManager.removeUpdates(legacyListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }

    private fun hasPermission(context: android.content.Context) =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    /**
     * Haversine formula — returns distance in km, rounded to 1 decimal place.
     */
    fun distanceKm(
        userLat: Double, userLon: Double,
        hotelLat: Double, hotelLon: Double
    ): Double {
        val R = 6371.0
        val dLat = Math.toRadians(hotelLat - userLat)
        val dLon = Math.toRadians(hotelLon - userLon)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(userLat)) *
                Math.cos(Math.toRadians(hotelLat)) *
                Math.sin(dLon / 2).let { it * it }
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val raw = R * c
        return Math.round(raw * 10) / 10.0
    }
}