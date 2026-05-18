package com.example.blindglassesapp.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "blind_glasses_wifi"
private const val KEY_PROFILES = "wifi_profiles_json"
private const val MAX_PROFILES = 12

data class WifiProfile(val ssid: String, val password: String)

/**
 * 將使用者輸入的 Wi‑Fi 帳密存在本機（一般 SharedPreferences，僅本 App 可讀）。
 * 方便重開對話框時帶入、多筆已存網路切換；不包含硬體加密。
 */
class WifiProfilesStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAll(): List<WifiProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val ssid = o.optString("ssid", "").trim()
                    if (ssid.isEmpty()) continue
                    add(WifiProfile(ssid = ssid, password = o.optString("pwd", "")))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 依 SSID 新增或覆寫密碼，新筆插在列表最前。 */
    fun upsert(profile: WifiProfile) {
        val ssid = profile.ssid.trim()
        if (ssid.isEmpty()) return
        val list = loadAll().filter { it.ssid != ssid }.toMutableList()
        list.add(0, WifiProfile(ssid = ssid, password = profile.password))
        while (list.size > MAX_PROFILES) list.removeAt(list.lastIndex)
        saveList(list)
    }

    fun remove(ssid: String) {
        val key = ssid.trim()
        if (key.isEmpty()) return
        saveList(loadAll().filter { it.ssid != key })
    }

    fun clear() {
        prefs.edit().remove(KEY_PROFILES).apply()
    }

    private fun saveList(profiles: List<WifiProfile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("ssid", p.ssid)
                    put("pwd", p.password)
                },
            )
        }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }
}
