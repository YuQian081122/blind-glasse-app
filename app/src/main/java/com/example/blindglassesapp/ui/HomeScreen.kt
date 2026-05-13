package com.example.blindglassesapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.blindglassesapp.ble.BleConnectionState
import com.example.blindglassesapp.viewmodel.MainViewModel

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onScanClick: () -> Unit,
    onMonitorClick: () -> Unit,
    onWifiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bleState by viewModel.bleState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bleState) {
        when (val s = bleState) {
            is BleConnectionState.DevicesFound -> showDeviceSheet = true
            is BleConnectionState.Error -> snackbarHostState.showSnackbar(s.message)
            is BleConnectionState.Disconnected -> snackbarHostState.showSnackbar("已斷開連線")
            else -> {}
        }
    }

    val isConnected = bleState is BleConnectionState.Connected
    val isLoading = bleState is BleConnectionState.Scanning || bleState is BleConnectionState.Connecting

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(48.dp))

                    Text(
                        text = "導盲眼鏡",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    if (isConnected) {
                        val name = (bleState as BleConnectionState.Connected).deviceName
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "已連線：$name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50),
                            )
                        }
                    } else {
                        Text(
                            text = "尚未連線裝置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // --- Primary actions (always visible) ---
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
                            .height(56.dp),
                    ) {
                        Text(
                            text = if (isConnected) "中斷連線" else "連接眼鏡",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onMonitorClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(
                            text = "即時監看",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    // --- Settings (only when connected) ---
                    if (isConnected) {
                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = "裝置設定",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        )

                        Button(
                            onClick = { showWifiDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text("設定 Wi-Fi")
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }

            // Loading overlay
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

    // Device list bottom sheet
    if (showDeviceSheet && bleState is BleConnectionState.DevicesFound) {
        DeviceListSheet(
            devices = (bleState as BleConnectionState.DevicesFound).devices,
            onDeviceSelected = { device ->
                showDeviceSheet = false
                viewModel.connectDevice(device)
            },
            onDismiss = {
                showDeviceSheet = false
            },
        )
    }

    // WiFi setting dialog
    if (showWifiDialog) {
        WifiSettingDialog(
            viewModel = viewModel,
            onDismiss = { showWifiDialog = false },
        )
    }
}
