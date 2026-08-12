package com.example.blindglassesapp.network

import com.example.blindglassesapp.BuildConfig
import okhttp3.Request

/** Public server endpoints; deployment settings are injected at build time. */
object FamilyEndpoints {
    const val MOBILE_APP_TOKEN_HEADER = "X-Mobile-App-Token"

    val BASE: String = BuildConfig.SERVER_BASE_URL.trimEnd('/')

    val FRAME = "$BASE/api/monitor/frame"
    val STATE = "$BASE/api/monitor/state"
    val LOCATION = "$BASE/api/family/location"
    val STREAM = "$BASE/stream"
    val MONITOR_PAGE = "$BASE/monitor"
    val HEALTH = "$BASE/health"

    private val WEBSOCKET_BASE: String = when {
        BASE.startsWith("https://") -> "wss://${BASE.removePrefix("https://")}"
        BASE.startsWith("http://") -> "ws://${BASE.removePrefix("http://")}"
        else -> BASE
    }

    val WS_VIEWER = "$WEBSOCKET_BASE/ws/viewer"
    val WS_UI = "$WEBSOCKET_BASE/ws_ui"

    internal fun authorize(
        builder: Request.Builder,
        token: String = BuildConfig.MOBILE_APP_TOKEN,
    ): Request.Builder {
        val cleanToken = token.trim()
        if (cleanToken.isNotEmpty()) {
            builder.header(MOBILE_APP_TOKEN_HEADER, cleanToken)
        }
        return builder
    }
}
