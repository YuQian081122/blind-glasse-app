package com.example.blindglassesapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.blindglassesapp.ble.BleManager
import com.example.blindglassesapp.viewmodel.MainViewModel

@Composable
fun WifiSettingDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val writeResult by viewModel.writeResult.collectAsState()

    LaunchedEffect(writeResult) {
        when (writeResult) {
            is BleManager.WriteResult.Success -> {
                isSending = false
                viewModel.clearWriteResult()
                onDismiss()
            }
            is BleManager.WriteResult.Failure -> {
                isSending = false
                viewModel.clearWriteResult()
            }
            null -> {}
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text("設定 Wi-Fi") },
        text = {
            Column(modifier = modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("WiFi 名稱 (SSID)") },
                    singleLine = true,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("WiFi 密碼") },
                    singleLine = true,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isSending) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (ssid.isNotBlank()) {
                        isSending = true
                        val ok = viewModel.writeWifiCredentials(ssid, password)
                        if (!ok) isSending = false
                    }
                },
                enabled = ssid.isNotBlank() && !isSending,
            ) {
                Text("傳送")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSending,
            ) {
                Text("取消")
            }
        },
    )
}
