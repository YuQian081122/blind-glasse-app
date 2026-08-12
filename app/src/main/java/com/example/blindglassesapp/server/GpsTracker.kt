package com.example.blindglassesapp.server

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.blindglassesapp.network.FamilyEndpoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Owns location callbacks and serialized phone uploads for the foreground service. */
class GpsTracker(context: Context) {
    private enum class UploadResult { SUCCESS, RETRYABLE_FAILURE, PERMANENT_FAILURE }

    companion object {
        private const val TAG = "GpsTracker"
        private const val MIN_TIME_MS = 5_000L
        private const val MIN_DISTANCE_METERS = 0f
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder().build()
    private var scopeJob: Job = SupervisorJob()
    private var scope = CoroutineScope(scopeJob + Dispatchers.IO)
    private val uploadPolicy = LocationUploadPolicy(MIN_TIME_MS)
    private val uploadInFlight = AtomicBoolean(false)
    private val pendingSample = AtomicReference<LocationSample?>(null)

    private var locationManager: LocationManager? = null
    private var isTracking = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            processLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    @Synchronized
    fun start() {
        if (isTracking) return

        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission is not granted; tracking remains stopped")
            return
        }

        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            Log.w(TAG, "LocationManager is unavailable; tracking remains stopped")
            return
        }
        locationManager = manager

        if (!scopeJob.isActive) {
            scopeJob = SupervisorJob()
            scope = CoroutineScope(scopeJob + Dispatchers.IO)
        }

        // Seed the upload path only with fresh cached fixes; stale cache must not mask a live fix.
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                manager.getLastKnownLocation(provider)?.let(::processLocation)
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to read cached $provider fix: ${exception.message}")
            }
        }

        var providerFound = false
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                if (!manager.isProviderEnabled(provider)) continue
                manager.requestLocationUpdates(
                    provider,
                    MIN_TIME_MS,
                    MIN_DISTANCE_METERS,
                    locationListener,
                )
                providerFound = true
            } catch (securityException: SecurityException) {
                Log.w(TAG, "Location permission was revoked while registering $provider")
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to register $provider: ${exception.message}")
            }
        }

        if (providerFound) {
            isTracking = true
            Log.i(TAG, "Location tracking started")
        } else {
            Log.w(TAG, "No usable location providers are enabled")
        }
    }

    @Synchronized
    fun stop() {
        val manager = locationManager
        if (manager != null) {
            try {
                manager.removeUpdates(locationListener)
            } catch (securityException: SecurityException) {
                Log.w(TAG, "Location permission was revoked while stopping tracking")
            }
        }
        locationManager = null
        client.dispatcher.cancelAll()
        pendingSample.set(null)
        scope.cancel()
        uploadInFlight.set(false)
        isTracking = false
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    @Synchronized
    private fun processLocation(location: Location) {
        val sample = location.toSample()
        if (!uploadPolicy.shouldUpload(sample)) {
            Log.d(TAG, "Skipped throttled or stale location callback")
            return
        }
        pendingSample.set(sample)
        startUploadWorker()
    }

    private fun startUploadWorker() {
        if (!uploadInFlight.compareAndSet(false, true) || !scopeJob.isActive) return
        scope.launch {
            try {
                var retryDelayMillis = 1_000L
                while (currentCoroutineContext().isActive) {
                    var sample = pendingSample.getAndSet(null) ?: break
                    while (currentCoroutineContext().isActive) {
                        when (uploadLocationOnce(sample)) {
                            UploadResult.SUCCESS -> break
                            UploadResult.PERMANENT_FAILURE -> {
                                pendingSample.set(null)
                                return@launch
                            }
                            UploadResult.RETRYABLE_FAILURE -> Unit
                        }
                        Log.w(TAG, "Location upload will retry after ${retryDelayMillis}ms")
                        delay(retryDelayMillis)
                        retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(30_000L)
                        sample = pendingSample.getAndSet(null) ?: sample
                    }
                    retryDelayMillis = 1_000L
                }
            } finally {
                uploadInFlight.set(false)
                if (pendingSample.get() != null && scopeJob.isActive) startUploadWorker()
            }
        }
    }

    private fun uploadLocationOnce(sample: LocationSample): UploadResult =
        try {
            val payload = JSONObject().apply {
                put("lat", sample.latitude)
                put("lng", sample.longitude)
                sample.altitude?.let { put("alt", it) }
                sample.accuracy?.let { put("accuracy", it) }
                sample.speed?.let { put("speed", it) }
                sample.course?.let { put("course", it) }
            }

            val request = FamilyEndpoints.authorize(Request.Builder())
                .url(FamilyEndpoints.LOCATION)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Location upload succeeded: HTTP ${response.code}")
                    UploadResult.SUCCESS
                } else {
                    Log.w(TAG, "Location upload failed: HTTP ${response.code}")
                    if (response.code in setOf(400, 401, 403, 413, 415)) {
                        UploadResult.PERMANENT_FAILURE
                    } else {
                        UploadResult.RETRYABLE_FAILURE
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Location upload error: ${exception.message}")
            UploadResult.RETRYABLE_FAILURE
        }

    private fun Location.toSample(): LocationSample = LocationSample(
        latitude = latitude,
        longitude = longitude,
        altitude = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAltitude()) altitude else null,
        accuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAccuracy()) accuracy else null,
        speed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasSpeed()) speed else null,
        course = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasBearing()) bearing else null,
        fixTimeMillis = time,
        provider = provider,
    )
}
