package com.example.blindglassesapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.blindglassesapp.BuildConfig
import com.example.blindglassesapp.ble.BleConnectionState
import com.example.blindglassesapp.ui.theme.AppThemePreference
import com.example.blindglassesapp.viewmodel.MainViewModel

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onScanClick: () -> Unit,
    themePreference: AppThemePreference,
    onThemePreferenceChange: (AppThemePreference) -> Unit,
    onMonitorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bleState by viewModel.bleState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }
    var previousBleState by remember { mutableStateOf<BleConnectionState?>(null) }

    LaunchedEffect(bleState) {
        val prev = previousBleState
        previousBleState = bleState
        when (val s = bleState) {
            is BleConnectionState.DevicesFound -> showDeviceSheet = true
            is BleConnectionState.Error -> snackbarHostState.showSnackbar(s.message)
            is BleConnectionState.Disconnected -> {
                when (prev) {
                    is BleConnectionState.Connected ->
                        snackbarHostState.showSnackbar("已斷開連線")
                    is BleConnectionState.Connecting ->
                        snackbarHostState.showSnackbar("連線未完成或已中斷，請再試一次")
                    else -> { /* 例如舊 GATT 晚到、或與 Idle 重複，不誤導為「已斷開」 */ }
                }
            }
            else -> {}
        }
    }

    val isConnected = bleState is BleConnectionState.Connected
    val isLoading = bleState is BleConnectionState.Scanning || bleState is BleConnectionState.Connecting
    val outline = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "導盲眼鏡",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "版本 ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "連線眼鏡後可設定 Wi‑Fi；即時監看不需連線。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 16.dp),
                )

                Text(
                    text = "顯示",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedCard(
                    shape = RoundedCornerShape(4.dp),
                    border = outline,
                ) {
                    ThemePreferenceChipRow(
                        current = themePreference,
                        onChange = onThemePreferenceChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "連線",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedCard(
                    shape = RoundedCornerShape(4.dp),
                    border = outline,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (isConnected) {
                                val name = (bleState as BleConnectionState.Connected).deviceName
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "已連線",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            } else {
                                Text(
                                    text = "尚未連線",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "請先掃描並選擇裝置",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Button(
                    onClick = {
                        if (isConnected) {
                            viewModel.disconnect()
                        } else {
                            onScanClick()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = if (isConnected) "中斷連線" else "連接眼鏡",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onMonitorClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "即時監看",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (isConnected) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "裝置",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedButton(
                        onClick = { showWifiDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "設定 Wi-Fi",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            if (isLoading) {
                val msg = when (val s = bleState) {
                    is BleConnectionState.Scanning -> "掃描中..."
                    is BleConnectionState.Connecting -> "連線至 ${s.deviceName}..."
                    else -> "載入中..."
                }
                LoadingOverlay(message = msg)
            }
        }
    }

    if (showDeviceSheet && bleState is BleConnectionState.DevicesFound) {
        DeviceListSheet(
            devices = (bleState as BleConnectionState.DevicesFound).devices,
            onDeviceSelected = { device ->
                showDeviceSheet = false
                viewModel.connectDevice(device)
            },
            onDismiss = {
                showDeviceSheet = false
                viewModel.dismissDeviceListResults()
            },
        )
    }

    if (showWifiDialog) {
        WifiSettingDialog(
            viewModel = viewModel,
            onDismiss = { showWifiDialog = false },
        )
    }
}
