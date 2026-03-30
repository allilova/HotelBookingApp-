// app/src/main/java/com/example/hotelbookingapp/LocationViewModel.kt
package com.example.hotelbookingapp

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationViewModel(app: Application) : AndroidViewModel(app) {

    private val locationManager =
        app.getSystemService(LocationManager::class.java)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private val listener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            _location.value = loc
        }
        // Required on API < 29
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    fun startTracking(context: android.content.Context) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return


        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)     -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return
        }


        locationManager.getLastKnownLocation(provider)?.let {
            _location.value = it
        }


        locationManager.requestLocationUpdates(
            provider,
            5_000L,
            10f,
            listener
        )
    }

    fun stopTracking() {
        locationManager.removeUpdates(listener)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }


    fun distanceKm(userLat: Double, userLon: Double,
                   hotelLat: Double, hotelLon: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(hotelLat - userLat)
        val dLon = Math.toRadians(hotelLon - userLon)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(userLat)) *
                Math.cos(Math.toRadians(hotelLat)) *
                Math.sin(dLon / 2).let { it * it }
        return (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10).let {
            Math.round(it) / 10.0
        }
    }
}