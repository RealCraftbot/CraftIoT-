package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.screens.AIAssistantScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceCatalogScreen
import com.example.ui.screens.ESP32SimulatorScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.theme.CraftIoTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CraftIoTTheme {
                val viewModel: DashboardViewModel = viewModel()
                var currentTab by remember { mutableStateOf(Tab.Dashboard) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == Tab.Dashboard,
                                onClick = { currentTab = Tab.Dashboard },
                                icon = { Icon(Icons.Default.Dashboard, "Control") },
                                label = { Text("Grid") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == Tab.Devices,
                                onClick = { currentTab = Tab.Devices },
                                icon = { Icon(Icons.Default.DeveloperBoard, "Devices") },
                                label = { Text("Nodes") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == Tab.Simulator,
                                onClick = { currentTab = Tab.Simulator },
                                icon = { Icon(Icons.Default.Memory, "Simulator") },
                                label = { Text("HW Sim") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == Tab.Assistant,
                                onClick = { currentTab = Tab.Assistant },
                                icon = { Icon(Icons.Default.AutoAwesome, "AI CoPilot") },
                                label = { Text("AI Agent") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == Tab.Marketplace,
                                onClick = { currentTab = Tab.Marketplace },
                                icon = { Icon(Icons.Default.Widgets, "OTA") },
                                label = { Text("OTA") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        when (currentTab) {
                            Tab.Dashboard -> DashboardScreen(viewModel = viewModel)
                            Tab.Devices -> DeviceCatalogScreen(viewModel = viewModel)
                            Tab.Simulator -> ESP32SimulatorScreen(viewModel = viewModel)
                            Tab.Assistant -> AIAssistantScreen(viewModel = viewModel)
                            Tab.Marketplace -> MarketplaceScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

enum class Tab {
    Dashboard,
    Devices,
    Simulator,
    Assistant,
    Marketplace
}
