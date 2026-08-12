package com.example.blindglassesapp.network

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyEndpointsTest {
    @Test
    fun authorizeAddsTheMobileAppTokenHeaderWithoutEmbeddingASecret() {
        val request = FamilyEndpoints.authorize(
            Request.Builder().url("https://example.test/api/family/location"),
            token = "deployment-token",
        ).build()

        assertEquals(
            "deployment-token",
            request.header(FamilyEndpoints.MOBILE_APP_TOKEN_HEADER),
        )
    }
}
