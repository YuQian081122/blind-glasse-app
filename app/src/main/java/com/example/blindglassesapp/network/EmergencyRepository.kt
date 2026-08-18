package com.example.blindglassesapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 緊急求助網路呼叫層。
 * 負責向伺服器 POST /api/family/emergency 發送求助請求，
 * 伺服器會透過 LINE Bot 推播通知家屬並附帶 GPS 位置。
 */
class EmergencyRepository {

    companion object {
        private const val TAG = "EmergencyRepository"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 發送緊急求助到伺服器。
     * @param note 附加說明（例如觸發來源）
     * @return true 表示伺服器成功接收並已嘗試推播家屬
     */
    suspend fun sendEmergency(note: String = "app_sos_button", lat: Double? = null, lng: Double? = null): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("note", note)
                    if (lat != null && lng != null) {
                        put("lat", lat)
                        put("lng", lng)
                    }
                }.toString().toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder()
                    .url(FamilyEndpoints.EMERGENCY)
                    .header("X-Mobile-App-Token", FamilyEndpoints.DEVICE_API_TOKEN)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                response.close()

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val ok = json.optBoolean("ok", false)
                    Log.i(TAG, "Emergency sent: ok=$ok, sent=${json.optBoolean("sent")}")
                    ok
                } else {
                    Log.w(TAG, "Emergency failed: HTTP ${response.code}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Emergency request error: ${e.message}", e)
                false
            }
        }

    /**
     * 持續上傳目前手機的 GPS 位置給伺服器
     */
    suspend fun uploadLocation(lat: Double, lng: Double): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("lat", lat)
                    put("lng", lng)
                }.toString().toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder()
                    .url(FamilyEndpoints.LOCATION)
                    .header("X-Mobile-App-Token", FamilyEndpoints.DEVICE_API_TOKEN)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val ok = response.isSuccessful
                response.close()
                ok
            } catch (e: Exception) {
                false
            }
        }

    /**
     * 向伺服器取得家屬設定的住家 GPS 座標。
     * @return Pair<Lat, Lng> 或 null
     */
    suspend fun getHomeLocation(): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(FamilyEndpoints.HOME_LOCATION)
                    .header("X-Mobile-App-Token", FamilyEndpoints.DEVICE_API_TOKEN)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                response.close()

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("ok", false) && json.has("lat") && json.has("lng")) {
                        return@withContext Pair(json.getDouble("lat"), json.getDouble("lng"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getHomeLocation error: ${e.message}", e)
            }
            return@withContext null
        }

    /**
     * 向伺服器發送啟動導航回家的請求，由眼鏡播放語音。
     */
    suspend fun triggerNavigateHome(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(FamilyEndpoints.TRIGGER_NAVIGATE_HOME)
                    .header("X-Mobile-App-Token", FamilyEndpoints.DEVICE_API_TOKEN)
                    .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                response.close()

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val ok = json.optBoolean("ok", false)
                    val started = json.optBoolean("navigation_started", false)
                    return@withContext ok && started
                }
            } catch (e: Exception) {
                Log.e(TAG, "triggerNavigateHome error: ${e.message}", e)
            }
            return@withContext false
        }

    /**
     * 從伺服器的狀態端點抓取目前最新回報的眼鏡電量。
     */
    suspend fun getBatteryLevel(): String =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(FamilyEndpoints.STATE)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                response.close()

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val json = JSONObject(responseBody)
                    val family = json.optJSONObject("family")
                    val lastBattery = family?.optJSONObject("last_battery")
                    if (lastBattery != null && lastBattery.has("percent")) {
                        val percent = lastBattery.optDouble("percent", Double.NaN)
                        if (!percent.isNaN()) {
                            return@withContext "${percent.toInt()} %"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getBatteryLevel error: ${e.message}", e)
            }
            return@withContext "未知"
        }
}
