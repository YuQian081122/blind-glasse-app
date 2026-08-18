package com.example.blindglassesapp.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blindglassesapp.ble.BleConnectionState
import com.example.blindglassesapp.ble.BleManager
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.blindglassesapp.server.AppBackgroundService
import com.example.blindglassesapp.network.EmergencyRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 緊急求助的狀態。 */
enum class EmergencyState {
    IDLE,    // 尚未觸發
    SENDING, // 發送中
    SENT,    // 已成功通知家屬
    FAILED   // 發送失敗
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = BleManager(application)
    private val emergencyRepository = EmergencyRepository()
    private val prefs = application.getSharedPreferences("blind_glasses_prefs", android.content.Context.MODE_PRIVATE)

    private val _isStandaloneMode = MutableStateFlow(false)
    val isStandaloneMode: StateFlow<Boolean> = _isStandaloneMode

    fun setStandaloneMode(active: Boolean) {
        _isStandaloneMode.value = active
    }

    val bleState: StateFlow<BleConnectionState> = combine(
        bleManager.state,
        _isStandaloneMode
    ) { state, standalone ->
        if (standalone) {
            BleConnectionState.Connected("模擬導盲眼鏡(單機)")
        } else {
            state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BleConnectionState.Disconnected
    )

    private val _isGlassesOnline = MutableStateFlow(false)
    val isGlassesOnline: StateFlow<Boolean> = _isGlassesOnline

    init {
        // 定期檢查眼鏡是否已連上外網 (透過詢問電量狀態確認)
        viewModelScope.launch {
            while (true) {
                if (bleState.value is BleConnectionState.Connected) {
                    if (_isStandaloneMode.value) {
                        _isGlassesOnline.value = true
                    } else {
                        val battery = emergencyRepository.getBatteryLevel()
                        _isGlassesOnline.value = (battery != "未知")
                    }
                } else {
                    _isGlassesOnline.value = false
                }
                kotlinx.coroutines.delay(3000)
            }
        }

        // 當眼鏡成功連線後，自動將上次儲存的音量同步給硬體
        viewModelScope.launch {
            bleState.collect { state ->
                if (state is BleConnectionState.Connected && !_isStandaloneMode.value) {
                    bleManager.writeVolume((_currentVolume.value * 2).toInt())
                }
            }
        }

        viewModelScope.launch {
            bleState.collect { state ->
                val intent = Intent(getApplication(), AppBackgroundService::class.java)
                if (state is BleConnectionState.Connected) {
                    val hasFine = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine) {
                        try {
                            getApplication<Application>().startService(intent)
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to start GPS tracking service", e)
                        }
                    }
                } else {
                    getApplication<Application>().stopService(intent)
                }
            }
        }
        
        startLocationTracking()
    }

    private var locationListenerRegistered = false

    fun startLocationTracking() {
        if (locationListenerRegistered) return
        
        try {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                
                // 1. 先抓最後一次已知位置上傳 (避免 GPS 還沒定位前網頁空空如也)
                val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    viewModelScope.launch {
                        emergencyRepository.uploadLocation(loc.latitude, loc.longitude)
                    }
                }

                // 2. 註冊即時監聽器 (不管現在有沒有開，先註冊再說)
                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(newLoc: android.location.Location) {
                        viewModelScope.launch {
                            emergencyRepository.uploadLocation(newLoc.latitude, newLoc.longitude)
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                
                try {
                    lm.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 5000L, 2f, listener)
                    lm.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 5000L, 2f, listener)
                    locationListenerRegistered = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val writeResult: StateFlow<BleManager.WriteResult?> = bleManager.writeResult

    val isBluetoothSupported: Boolean
        get() = bleManager.isBluetoothSupported

    val isBluetoothEnabled: Boolean
        get() = bleManager.isBluetoothEnabled

    val isConnected: Boolean
        get() = bleState.value is BleConnectionState.Connected

    fun startScan() = bleManager.startScan()

    fun stopScan() = bleManager.stopScan()

    fun connectDevice(device: BluetoothDevice) = bleManager.connect(device)

    fun disconnect() = bleManager.disconnect()

    /** 清除「掃描完成、裝置清單」狀態，使返回首頁時不會再自動打開底部表。 */
    fun dismissDeviceListResults() = bleManager.clearDevicesFoundState()

    fun writeWifiCredentials(ssid: String, password: String): Boolean {
        return bleManager.writeWifiCredentials(ssid, password)
    }

    fun clearWriteResult() = bleManager.clearWriteResult()

    // 音量狀態，範圍 0.0-10.0 (對應 BLE 端 0-20)
    private val _currentVolume = kotlinx.coroutines.flow.MutableStateFlow(prefs.getFloat("last_volume", 7.5f))
    val currentVolume: StateFlow<Float> = _currentVolume

    // 是否正在音量調整模式（直接透過手機實體音量鍵調整眼鏡音量）
    private val _isVolumeAdjustmentActive = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isVolumeAdjustmentActive: StateFlow<Boolean> = _isVolumeAdjustmentActive

    fun setVolumeAdjustmentActive(active: Boolean) {
        _isVolumeAdjustmentActive.value = active
    }

    /**
     * 觸發找眼鏡，眼鏡會播放提示聲。
     * @return true = BLE 寫入指令已發送。
     */
    fun sendFindMe(): Boolean {
        return bleManager.writeFindMe()
    }

    /**
     * 音量加一（0.5 階），上限 10.0。
     * @return true = BLE 寫入指令已發送。
     */
    fun increaseVolume(): Boolean {
        val next = (_currentVolume.value + 0.5f).coerceAtMost(10.0f)
        val sent = if (_isStandaloneMode.value) true else bleManager.writeVolume((next * 2).toInt())
        if (sent) {
            _currentVolume.value = next
            prefs.edit().putFloat("last_volume", next).apply()
        }
        return sent
    }

    /**
     * 音量減一（0.5 階），下限 0.0。
     * @return true = BLE 寫入指令已發送。
     */
    fun decreaseVolume(): Boolean {
        val next = (_currentVolume.value - 0.5f).coerceAtLeast(0.0f)
        val sent = if (_isStandaloneMode.value) true else bleManager.writeVolume((next * 2).toInt())
        if (sent) {
            _currentVolume.value = next
            prefs.edit().putFloat("last_volume", next).apply()
        }
        return sent
    }

    // ── 盲人模式狀態與事件 ──
    private val _isAccessibilityModeActive = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAccessibilityModeActive: StateFlow<Boolean> = _isAccessibilityModeActive

    fun setAccessibilityModeActive(active: Boolean) {
        _isAccessibilityModeActive.value = active
    }

    private val _openAccessibilityEvent = MutableSharedFlow<Unit>()
    val openAccessibilityEvent: SharedFlow<Unit> = _openAccessibilityEvent

    fun triggerAccessibilityMode() {
        viewModelScope.launch { _openAccessibilityEvent.emit(Unit) }
    }

    private val _closeAccessibilityEvent = MutableSharedFlow<Unit>()
    val closeAccessibilityEvent: SharedFlow<Unit> = _closeAccessibilityEvent

    fun triggerCloseAccessibilityMode() {
        viewModelScope.launch { _closeAccessibilityEvent.emit(Unit) }
    }

    // ── 緊急求助 ──
    private val _emergencyState = MutableStateFlow(EmergencyState.IDLE)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState

    /**
     * 發送緊急求助到伺服器（伺服器會 LINE 推播家屬 + GPS 位置）。
     * UI 層可透過 [emergencyState] 觀察結果，提供 TTS 回饋。
     */
    fun sendEmergency() {
        if (_emergencyState.value == EmergencyState.SENDING) return // 防止重複發送
        _emergencyState.value = EmergencyState.SENDING

        var lat: Double? = null
        var lng: Double? = null
        try {
            val context = getApplication<Application>()
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                lat = loc?.latitude
                lng = loc?.longitude
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModelScope.launch {
            val ok = emergencyRepository.sendEmergency(lat = lat, lng = lng)
            _emergencyState.value = if (ok) EmergencyState.SENT else EmergencyState.FAILED
        }
    }

    /** 重設緊急狀態為 IDLE（允許再次觸發）。 */
    fun resetEmergencyState() {
        _emergencyState.value = EmergencyState.IDLE
    }

    // ── 導航回家 ──
    fun navigateHome(context: android.content.Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (_isStandaloneMode.value) {
                // 模擬模式：直接回報成功 (或者也可以模擬啟動本地地圖)
                onResult(true)
            } else {
                val success = emergencyRepository.triggerNavigateHome()
                onResult(success)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.release()
    }

    /**
     * 向伺服器查詢目前的眼鏡電量並回傳字串。
     */
    fun checkBatteryLevel(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val batteryStr = emergencyRepository.getBatteryLevel()
            onResult(batteryStr)
        }
    }
}
