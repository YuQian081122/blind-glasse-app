package com.example.blindglassesapp.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListSheet(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showUnnamedDevices by remember { mutableStateOf(false) }

    LaunchedEffect(devices) {
        showUnnamedDevices = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "選擇導盲眼鏡",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // 預設只顯示有廣播名稱；無名稱區以「進階／收合」切換；新掃描結果會重設為收合。
            val withBroadcastName = devices
                .filter { !it.name.isNullOrBlank() }
                .sortedWith(compareBy({ it.name }, { it.address }))
            val withoutBroadcastName = devices
                .filter { it.name.isNullOrBlank() }
                .sortedBy { it.address }

            LazyColumn {
                if (withBroadcastName.isNotEmpty()) {
                    item(key = "section_named") {
                        Text(
                            text = "有廣播名稱",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                    }
                    items(withBroadcastName, key = { it.address }) { device ->
                        DeviceRow(
                            name = device.name.orEmpty(),
                            address = device.address,
                            onClick = { onDeviceSelected(device) },
                        )
                        HorizontalDivider()
                    }
                }
                if (withoutBroadcastName.isNotEmpty()) {
                    if (!showUnnamedDevices) {
                        item(key = "advanced_toggle") {
                            OutlinedButton(
                                onClick = { showUnnamedDevices = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                            ) {
                                Text("進階")
                            }
                        }
                    } else {
                        item(key = "collapse_unnamed") {
                            OutlinedButton(
                                onClick = { showUnnamedDevices = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                            ) {
                                Text("收合")
                            }
                        }
                        item(key = "section_unnamed") {
                            Text(
                                text = "無廣播名稱（請以 MAC 辨識眼鏡）",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            )
                        }
                        items(withoutBroadcastName, key = { it.address }) { device ->
                            DeviceRow(
                                name = "無廣播名稱（請以 MAC 辨識）",
                                address = device.address,
                                onClick = { onDeviceSelected(device) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DeviceRow(
    name: String,
    address: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "連線",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
