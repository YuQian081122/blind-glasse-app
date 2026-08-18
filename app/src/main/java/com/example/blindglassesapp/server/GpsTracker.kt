package com.example.blindglassesapp.server

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.blindglassesapp.network.FamilyEndpoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GpsTracker(private val context: Context) {

    companion object {
        private const val TAG = "GpsTracker"
        private const val MIN_TIME_MS: Long = 5000 // Update every 5 seconds
        private const val MIN_DISTANCE_M: Float = 2f // Or when moved 2 meters
    }

    private var locationManager: LocationManager? = null
    private val client = OkHttpClient.Builder().build()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var isTracking = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            uploadLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun start() {
        if (isTracking) return
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing location permissions, cannot start GPS tracking.")
            return
        }

        try {
            // Try requesting updates from both GPS and Network providers
            var providerFound = false
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener
                )
                providerFound = true
            }
            
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener
                )
                providerFound = true
            }

            if (!providerFound) {
                Log.w(TAG, "No location providers available.")
            } else {
                isTracking = true
                Log.d(TAG, "GPS Tracking started.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates", e)
        }
    }

    fun stop() {
        if (!isTracking) return
        locationManager?.removeUpdates(locationListener)
        isTracking = false
        Log.d(TAG, "GPS Tracking stopped.")
    }

    private fun uploadLocation(location: Location) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("lat", location.latitude)
                    put("lng", location.longitude)
                    put("alt", location.altitude)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${FamilyEndpoints.BASE}/api/family/location")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.v(TAG, "Successfully uploaded GPS: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.w(TAG, "Failed to upload GPS, HTTP ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading GPS", e)
            }
        }
    }
}
