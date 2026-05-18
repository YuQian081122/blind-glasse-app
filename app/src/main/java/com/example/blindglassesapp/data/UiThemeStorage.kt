package com.example.blindglassesapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.blindglassesapp.ui.theme.AppThemePreference

private const val PREFS_UI = "blind_glasses_ui"
private const val KEY_THEME_MODE_STR = "theme_mode_str"
/** 舊版整數：0=系統、1=淺、2=深；僅在未寫入 [KEY_THEME_MODE_STR] 時讀一次並遷移。 */
private const val KEY_THEME_LEGACY_INT = "theme_preference"

/**
 * 淺／深色偏好。新版以字串 `light`/`dark` 儲存，避免舊版整數 1 同時代表「淺色」與新式 ordinal 的歧義。
 */
class UiThemeStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_UI, Context.MODE_PRIVATE)

    fun load(): AppThemePreference {
        val str = prefs.getString(KEY_THEME_MODE_STR, null)
        if (str == "dark") return AppThemePreference.DARK
        if (str == "light") return AppThemePreference.LIGHT

        val raw = prefs.getInt(KEY_THEME_LEGACY_INT, 0)
        val mode = when (raw) {
            2 -> AppThemePreference.DARK
            1 -> AppThemePreference.LIGHT
            else -> AppThemePreference.LIGHT
        }
        save(mode)
        return mode
    }

    fun save(mode: AppThemePreference) {
        val value = if (mode == AppThemePreference.DARK) "dark" else "light"
        prefs.edit()
            .putString(KEY_THEME_MODE_STR, value)
            .commit()
    }
}
