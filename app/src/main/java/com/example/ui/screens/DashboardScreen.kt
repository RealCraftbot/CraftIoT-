package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IoTDevice
import com.example.data.model.SensorLog
import com.example.ui.components.TelemetryChart
import com.example.ui.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.devices.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val logs by viewModel.latestLogs.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val chartLogs by viewModel.selectedDeviceSensorLogs.collectAsState(initial = emptyList())
    val selectedMetric by viewModel.selectedMetric.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()

    val onlineCount = devices.count { it.status == "ONLINE" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Banner Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_banner"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsInputAntenna,
                                contentDescription = "Ecosystem Banner",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "CRAFTIOT PLATFORM",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Smart Grid Active",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Robust offline-first SQLite synchronization engine actively running with direct REST & secure local hardware simulation loops.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Metrics Grid Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Online Devices",
                    value = "$onlineCount / ${devices.size}",
                    icon = Icons.Default.CloudQueue,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Automation Rules",
                    value = "${rules.count { it.isActive }} Active",
                    icon = Icons.Default.PlayForWork,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Latency and Simulator state
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isSimulating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, CircleShape)
                            )
                            Text(
                                text = "ESP32 HARDWARE SIMULATOR",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (isSimulating) "Broadcasting telemetry live to Room DB" else "Simulation paused (Main Thread Idle)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Button(
                        onClick = { viewModel.toggleSimulation() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimulating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp).testTag("simulator_toggle_btn")
                    ) {
                        Text(
                            text = if (isSimulating) "Pause" else "Resume",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Interactive Telemetry Hub
        item {
            Text(
                text = "TELEMETRY VISUALIZATION HUB",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        // Device chip picker row
        item {
            if (devices.isEmpty()) {
                Text("No devices found. Create or provision one first.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create visual chips to choose selected device
                    devices.forEach { device ->
                        val isSelected = selectedDevice?.id == device.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) viewModel.selectDevice(null)
                                else viewModel.selectDevice(device.id)
                            },
                            label = { Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("device_chip_${device.id}")
                        )
                    }
                }
            }
        }

        // Focused Device telemetry charts and actions
        item {
            if (selectedDevice != null) {
                val dev = selectedDevice!!
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("selected_device_panel"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dev.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${dev.typeLabel} • ${dev.id}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // Active Status indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Text(
                                    text = dev.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metric selection row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val availableMetrics = when (dev.type) {
                                "ROBOT_CAR" -> listOf("speed" to "Velocity", "battery" to "Voltage")
                                "CLIMATE_NODE" -> listOf("temp" to "Temp (°C)", "humidity" to "Hum (%)")
                                "SECURITY_CAM" -> listOf("battery" to "Charge", "bandwidth" to "Bandwidth")
                                "SMART_AGRI" -> listOf("ph" to "Soil pH", "humidity" to "Moisture")
                                else -> listOf("telemetry" to "Telemetry")
                            }

                            availableMetrics.forEach { (m, label) ->
                                val active = selectedMetric == m
                                Button(
                                    onClick = { viewModel.selectMetric(m) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Custom Chart
                        TelemetryChart(
                            logs = chartLogs,
                            metricName = selectedMetric,
                            lineColor = if (selectedMetric == "speed" || selectedMetric == "temp") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls Block
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (dev.stateFlag1) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                    contentDescription = "Relay Switch State",
                                    tint = if (dev.stateFlag1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = when (dev.type) {
                                            "ROBOT_CAR" -> "Dual DC Motor Drive"
                                            "CLIMATE_NODE" -> "Irrigation Valve"
                                            "SMART_AGRI" -> "Liquid Water Pump"
                                            else -> "Relay Toggle State"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (dev.stateFlag1) "PIN GPIO_05: ACTIVE (HIGH)" else "PIN GPIO_05: DEACTIVATED (LOW)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Switch(
                                checked = dev.stateFlag1,
                                onCheckedChange = { viewModel.toggleDeviceRelay(dev.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("relay_switch_${dev.id}")
                            )
                        }
                    }
                }
            } else {
                // Friendly Empty State Tip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Hint",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Interactive Live Feeds",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Click any of the unselected device chips above to hook live telemetry data into the custom Canvas drawing canvas & toggle physical GPIO pins.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

        // Live MQTT System Packets Stream Header
        item {
            Text(
                text = "REAL-TIME MQTT LOG FEED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        // Mini Logs list
        val displayLogs = logs.take(5)
        if (displayLogs.isEmpty()) {
            item {
                Text(
                    text = "Awaiting live packet packets from brokers...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            items(displayLogs) { log ->
                val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val timeString = formatter.format(Date(log.timestamp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (log.metric) {
                                "speed" -> Icons.Default.Speed
                                "temp" -> Icons.Default.Thermostat
                                "humidity" -> Icons.Default.WaterDrop
                                "battery" -> Icons.Default.BatteryChargingFull
                                "control_switch" -> Icons.Default.SettingsInputComponent
                                else -> Icons.Default.Memory
                            },
                            contentDescription = log.metric,
                            tint = when (log.metric) {
                                "speed", "temp" -> MaterialTheme.colorScheme.primary
                                "humidity" -> MaterialTheme.colorScheme.secondary
                                "control_switch" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Topic: devices/${log.deviceId}/${log.metric}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Payload: {\"value\": ${log.value}, \"time\": ${log.timestamp}}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Text(
                            text = timeString,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
