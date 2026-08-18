package com.example.blindglassesapp.network

/** 正式站基底網址（監看頁寫死使用此常數）。 */
object FamilyEndpoints {
    const val BASE = "https://www.blind-glasses.org"
    // 與 ESP32 韌體端 config.h 預設相同的 Token
    const val DEVICE_API_TOKEN = "0QchQE-fzMg5yg-1GHu-3-J7tfgqtsDA2J-pKPcMBu4"

    const val FRAME = "$BASE/api/monitor/frame"
    const val STATE = "$BASE/api/monitor/state"
    const val LOCATION = "$BASE/api/family/location"
    const val STREAM = "$BASE/stream"
    const val MONITOR_PAGE = "$BASE/monitor"
    const val HEALTH = "$BASE/health"
    const val EMERGENCY = "$BASE/api/family/emergency"
    const val HOME_LOCATION = "$BASE/api/family/home_location"
    const val TRIGGER_NAVIGATE_HOME = "$BASE/api/navigation/home"

    const val WS_VIEWER = "wss://www.blind-glasses.org/ws/viewer"
    const val WS_UI = "wss://www.blind-glasses.org/ws_ui"
}
