package com.example.blindglassesapp.server

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUploadPolicyTest {
    @Test
    fun acceptsFirstFixAndThrottlesProviderDuplicateBurst() {
        val policy = LocationUploadPolicy(minIntervalMillis = 5_000L)
        val gps = LocationSample(latitude = 25.033, longitude = 121.565, provider = "gps")
        val network = gps.copy(provider = "network")

        assertTrue(policy.shouldUpload(gps, nowMillis = 0L))
        assertFalse(policy.shouldUpload(network, nowMillis = 100L))
    }

    @Test
    fun acceptsAChangedFixAfterTheFiveSecondThrottleWindow() {
        val policy = LocationUploadPolicy(minIntervalMillis = 5_000L)
        val first = LocationSample(latitude = 25.033, longitude = 121.565, provider = "gps")
        val moved = first.copy(latitude = 25.034)

        assertTrue(policy.shouldUpload(first, nowMillis = 0L))
        assertFalse(policy.shouldUpload(moved, nowMillis = 4_999L))
        assertTrue(policy.shouldUpload(moved, nowMillis = 5_000L))
    }

    @Test
    fun rejectsAStaleLastKnownFix() {
        val policy = LocationUploadPolicy(
            minIntervalMillis = 5_000L,
            maxSampleAgeMillis = 120_000L,
        )
        val stale = LocationSample(
            latitude = 25.033,
            longitude = 121.565,
            fixTimeMillis = 1_000L,
            provider = "gps",
        )

        assertFalse(policy.shouldUpload(stale, nowMillis = 121_001L))
    }

    @Test
    fun acceptsAFreshLastKnownFix() {
        val policy = LocationUploadPolicy(
            minIntervalMillis = 5_000L,
            maxSampleAgeMillis = 120_000L,
        )
        val fresh = LocationSample(
            latitude = 25.033,
            longitude = 121.565,
            fixTimeMillis = 1_000L,
            provider = "gps",
        )

        assertTrue(policy.shouldUpload(fresh, nowMillis = 121_000L))
    }
}
