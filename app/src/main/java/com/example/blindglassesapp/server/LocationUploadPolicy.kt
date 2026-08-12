package com.example.blindglassesapp.server

/** A provider-neutral location snapshot used by the upload throttle. */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val course: Float? = null,
    val fixTimeMillis: Long = 0L,
    val provider: String? = null,
)

/**
 * Serializes callbacks from multiple providers into at most one upload per interval.
 * Cached fixes older than two minutes are rejected so navigation never receives a stale
 * startup coordinate.
 */
class LocationUploadPolicy(
    private val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS,
    private val maxSampleAgeMillis: Long = DEFAULT_MAX_SAMPLE_AGE_MILLIS,
) {
    private data class LastUpload(val sample: LocationSample, val timestampMillis: Long)

    @Volatile
    private var lastUpload: LastUpload? = null

    @Synchronized
    fun shouldUpload(sample: LocationSample, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (sample.fixTimeMillis > 0L && nowMillis - sample.fixTimeMillis > maxSampleAgeMillis) {
            return false
        }

        val previous = lastUpload
        if (previous != null && nowMillis - previous.timestampMillis < minIntervalMillis) {
            return false
        }

        lastUpload = LastUpload(sample, nowMillis)
        return true
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MILLIS: Long = 5_000L
        const val DEFAULT_MAX_SAMPLE_AGE_MILLIS: Long = 120_000L
    }
}
