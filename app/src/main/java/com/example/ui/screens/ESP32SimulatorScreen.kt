package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dashboard.DashboardViewModel

@Composable
fun ESP32SimulatorScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val serialLogs by viewModel.serialConsoleLogs.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll serial console to bottom on new log
    LaunchedEffect(serialLogs.size) {
        if (serialLogs.isNotEmpty()) {
            listState.animateScrollToItem(serialLogs.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("esp32_simulator_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        item {
            Column {
                Text(
                    text = "ESP32 HW Simulator & Console",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Visualize live system packets, CPU clock telemetry, and dynamic IO registers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Selected Node Quick Config Panel
        item {
            if (selectedDevice != null) {
                val dev = selectedDevice!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Memory, "Chip", tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Active Simulation Core", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("ID: ${dev.id} (${dev.typeLabel})", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }

                            // Signal level indicators
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(5) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 3.dp, height = (4 + (i * 3)).dp)
                                            .background(
                                                if (i < 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render ESP32 PCB Layout directly on canvas!
                        PCBLayoutCanvas(
                            blinkLed = isSimulating && (System.currentTimeMillis() % 2000 < 500),
                            relayState = dev.stateFlag1
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live sliders to tweak telemetry manually
                        Text(
                            text = "SIMULATE ADC PHYSICAL TRANSDUCERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        when (dev.type) {
                            "ROBOT_CAR" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Simulated Potentiometer (Speed Target: ${dev.sensorValue1.toInt()} km/h)", fontSize = 12.sp)
                                    Slider(
                                        value = dev.sensorValue1,
                                        onValueChange = { newVal ->
                                            viewModel.updateDeviceTelemetry(dev.id, newVal, dev.sensorValue2)
                                        },
                                        valueRange = 0f..120f,
                                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            "CLIMATE_NODE" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Ambient Temperature Thermistor (${String.format("%.1f", dev.sensorValue1)} °C)", fontSize = 12.sp)
                                    Slider(
                                        value = dev.sensorValue1,
                                        onValueChange = { newVal ->
                                            viewModel.updateDeviceTelemetry(dev.id, newVal, dev.sensorValue2)
                                            viewModel.addSensorLog(com.example.data.model.SensorLog(deviceId = dev.id, metric = "temp", value = newVal))
                                        },
                                        valueRange = 15f..45f,
                                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary)
                                    )
                                }
                            }
                            "SMART_AGRI" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Soil Moisture ADC Transducer (${dev.sensorValue2.toInt()} cb Moisture)", fontSize = 12.sp)
                                    Slider(
                                        value = dev.sensorValue2,
                                        onValueChange = { newVal ->
                                            viewModel.updateDeviceTelemetry(dev.id, dev.sensorValue1, newVal)
                                        },
                                        valueRange = 100f..800f,
                                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary, activeTrackColor = MaterialTheme.colorScheme.tertiary)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.DeveloperBoard, "Board", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Simulation Core Hooked", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Hook into any device in the Control Center Dashboard first to inspect its physical registers and flash serial outputs.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Live scrolling terminal console
        item {
            Text(
                text = "GPIO / MQTT SERIAL MONITOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)) // Night slate dark console
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(serialLogs) { log ->
                            Text(
                                text = log,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    log.contains("ALERT") || log.contains("🚨") || log.contains("Error") -> Color(0xFFF87171) // red
                                    log.contains("MQTT PUBLISH") -> Color(0xFF38BDF8) // sky blue
                                    log.contains("SUCCESS") || log.contains("ONLINE") || log.contains("🎉") -> Color(0xFF34D399) // green
                                    log.contains("AI ASSISTANT") -> Color(0xFFFBBF24) // amber
                                    else -> Color(0xFFE2E8F0) // off-white
                                }
                            )
                        }
                    }

                    // Floating clear log action
                    IconButton(
                        onClick = { viewModel.clearSerialLogs() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.ClearAll, "Clear Terminal", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PCBLayoutCanvas(
    blinkLed: Boolean,
    relayState: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color(0xFF065F46), RoundedCornerShape(8.dp)) // PCB Green
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw ESP32 Main Core Chip (Obsidian Slate with silver metal pins)
            val chipW = w * 0.35f
            val chipH = h * 0.75f
            val chipX = (w - chipW) / 2f
            val chipY = (h - chipH) / 2f

            // Draw microcontroller shield
            drawRect(
                color = Color(0xFFD1D5DB), // Silver metal
                topLeft = Offset(chipX, chipY),
                size = Size(chipW, chipH)
            )

            // Draw core silicon plate
            drawRect(
                color = Color(0xFF1F2937), // Dark Gray
                topLeft = Offset(chipX + 4.dp.toPx(), chipY + 4.dp.toPx()),
                size = Size(chipW - 8.dp.toPx(), chipH - 8.dp.toPx())
            )

            // 2. Draw gold-plated antenna lines
            val antX = chipX + 8.dp.toPx()
            val antY = chipY + 8.dp.toPx()
            val antW = chipW - 16.dp.toPx()
            val antH = chipH * 0.2f

            drawRect(
                color = Color(0xFFD97706), // Gold lines
                topLeft = Offset(antX, antY),
                size = Size(antW, antH)
            )

            // 3. Draw GPIO Pin gold-plated tracks extending from chip out to PCB edges
            val pinsCount = 8
            val pinW = 6.dp.toPx()
            val pinH = 3.dp.toPx()

            for (i in 0 until pinsCount) {
                val yOffset = chipY + (chipH * 0.25f) + (i * 8.dp.toPx())
                // Left Pins
                drawRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset(chipX - pinW, yOffset),
                    size = Size(pinW, pinH)
                )
                // Right Pins
                drawRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset(chipX + chipW, yOffset),
                    size = Size(pinW, pinH)
                )
            }

            // 4. Draw PCB copper tracks
            drawLine(
                color = Color(0xFF047857),
                start = Offset(10.dp.toPx(), h * 0.5f),
                end = Offset(chipX - pinW, h * 0.5f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0xFF047857),
                start = Offset(w - 10.dp.toPx(), h * 0.4f),
                end = Offset(chipX + chipW + pinW, h * 0.4f),
                strokeWidth = 2.dp.toPx()
            )

            // 5. Draw LED GPIO2 (Blue WiFi Status indicator)
            val ledX = chipX + (chipW * 0.25f)
            val ledY = chipY + (chipH * 0.5f)
            drawCircle(
                color = if (blinkLed) Color(0xFF38BDF8) else Color(0xFF0369A1), // Glowing Azure LED
                radius = 4.dp.toPx(),
                center = Offset(ledX, ledY)
            )

            // 6. Draw LED GPIO5 (Relay state indicator)
            val relLedX = chipX + (chipW * 0.75f)
            val relLedY = chipY + (chipH * 0.5f)
            drawCircle(
                color = if (relayState) Color(0xFF34D399) else Color(0xFF065F46), // Glowing Green LED for relay
                radius = 4.dp.toPx(),
                center = Offset(relLedX, relLedY)
            )
        }

        // Text overlays inside the box
        Text(
            text = "ESP-WROOM-32D",
            color = Color.White.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("GPIO_02 Status: BLINK", color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 8.sp)
            Text(if (relayState) "GPIO_05 Drive: ACTIVE" else "GPIO_05 Drive: LOW", color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 8.sp)
        }
    }
}
