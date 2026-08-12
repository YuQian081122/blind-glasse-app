package com.example.blindglassesapp.server

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GpsTrackerConfigurationTest {
    @Test
    fun requestsStationaryUpdatesWithoutADistanceFilter() {
        val source = File(
            "src/main/java/com/example/blindglassesapp/server/GpsTracker.kt",
        ).readText()

        assertTrue(
            "GpsTracker must request updates with a zero metre distance filter",
            source.contains("private const val MIN_DISTANCE_METERS = 0f"),
        )
    }
}
