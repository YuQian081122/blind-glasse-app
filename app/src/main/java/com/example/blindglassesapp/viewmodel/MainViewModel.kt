package com.example.blindglassesapp.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.example.blindglassesapp.ble.BleConnectionState
import com.example.blindglassesapp.ble.BleManager
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = BleManager(application)

    val bleState: StateFlow<BleConnectionState> = bleManager.state
    val writeResult: StateFlow<BleManager.WriteResult?> = bleManager.writeResult

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

    override fun onCleared() {
        super.onCleared()
        bleManager.release()
    }
}
