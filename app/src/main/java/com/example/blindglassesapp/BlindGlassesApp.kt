package com.example.blindglassesapp

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blindglassesapp.ui.HomeScreen
import com.example.blindglassesapp.ui.MonitorScreen
import com.example.blindglassesapp.viewmodel.MainViewModel

@Composable
fun BlindGlassesApp(
    onRequestBleScan: () -> Unit,
) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onScanClick = onRequestBleScan,
                onMonitorClick = { navController.navigate("monitor") },
                onWifiClick = { /* handled internally by HomeScreen dialog */ },
            )
        }

        composable("monitor") {
            MonitorScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
