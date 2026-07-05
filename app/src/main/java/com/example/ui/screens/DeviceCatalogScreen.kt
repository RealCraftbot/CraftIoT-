package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IoTDevice
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.ProvisioningStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCatalogScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.devices.collectAsState()
    val provisioningStep by viewModel.provisioningStep.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Forms for provisioning
    var newDeviceName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ROBOT_CAR") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().testTag("device_catalog_root")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "IoT Ecosystem Catalog",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Manage unlimited physical & simulated nodes.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = {
                            newDeviceName = ""
                            selectedType = "ROBOT_CAR"
                            wifiSsid = ""
                            wifiPassword = ""
                            showAddDialog = true
                            viewModel.resetProvisioning()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.testTag("add_device_btn")
                    ) {
                        Icon(Icons.Default.Add, "Add Device", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deploy Node", fontSize = 12.sp)
                    }
                }
            }

            if (devices.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperBoardOff,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Connected Nodes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Click Deploy Node at the top to configure your first smart device.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                items(devices) { device ->
                    DeviceRowItem(
                        device = device,
                        onDelete = { viewModel.deleteDevice(device) },
                        onSelect = { viewModel.selectDevice(device.id) }
                    )
                }
            }
        }

        // Provisioning Dialogue Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CellTower, "Wizard", tint = MaterialTheme.colorScheme.primary)
                        Text("Smart Provisioning Wizard", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when (provisioningStep) {
                            is ProvisioningStep.Idle -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Deploy and bind a new edge hardware module. This simulates scanning for nearby bluetooth advertising packets and mounting to Wi-Fi gateways.", fontSize = 12.sp)
                                    
                                    OutlinedTextField(
                                        value = newDeviceName,
                                        onValueChange = { newDeviceName = it },
                                        label = { Text("Device Display Name") },
                                        placeholder = { Text("e.g. Living Room Fan, Smart Rover") },
                                        modifier = Modifier.fillMaxWidth().testTag("device_name_field"),
                                        singleLine = true
                                    )

                                    Text("Select Module Template:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val types = listOf(
                                            "ROBOT_CAR" to "Rover",
                                            "CLIMATE_NODE" to "Climate",
                                            "SECURITY_CAM" to "Camera",
                                            "SMART_AGRI" to "Agri"
                                        )
                                        types.forEach { (t, label) ->
                                            val isSel = selectedType == t
                                            Button(
                                                onClick = { selectedType = t },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (newDeviceName.isNotBlank()) {
                                                viewModel.startProvisioning(newDeviceName, selectedType)
                                            }
                                        },
                                        enabled = newDeviceName.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth().testTag("start_prov_btn")
                                    ) {
                                        Text("Scan BLE Advertisement")
                                    }
                                }
                            }
                            is ProvisioningStep.BleScanning -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Scanning for BLE Advertisements...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Searching localized hardware slots (Dual LX6 channels)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            is ProvisioningStep.BleConnected -> {
                                val mac = (provisioningStep as ProvisioningStep.BleConnected).mac
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Bluetooth, "BLE", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("BLE GATTS Connection Established!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Connected securely to MAC: $mac", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            is ProvisioningStep.WifiCredentialsInput -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("ESP32 GATTS channel secure. Pass local wireless gateway credentials to enable MQTT/HTTP routing.", fontSize = 12.sp)

                                    OutlinedTextField(
                                        value = wifiSsid,
                                        onValueChange = { wifiSsid = it },
                                        label = { Text("Wi-Fi SSID") },
                                        placeholder = { Text("MyHomeWifi") },
                                        modifier = Modifier.fillMaxWidth().testTag("wifi_ssid_field"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = wifiPassword,
                                        onValueChange = { wifiPassword = it },
                                        label = { Text("Wi-Fi Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().testTag("wifi_pass_field"),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (wifiSsid.isNotBlank()) {
                                                viewModel.submitProvisioningCredentials(wifiSsid, newDeviceName, selectedType)
                                            }
                                        },
                                        enabled = wifiSsid.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth().testTag("provision_submit_btn")
                                    ) {
                                        Text("Mount Device to Network")
                                    }
                                }
                            }
                            is ProvisioningStep.ConfiguringWifi -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Transmitting Wi-Fi Configurations...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("ESP32 attempting handshake with router on channel 6...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            is ProvisioningStep.VerifyingCloudSync -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Syncing with CraftIoT Broker...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Registering MAC & acquiring MQTT dynamic SSL leases...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            is ProvisioningStep.Success -> {
                                val dev = (provisioningStep as ProvisioningStep.Success).device
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CheckCircle, "Success", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(54.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Device Fully Connected!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Name: ${dev.name}", fontSize = 12.sp)
                                    Text("IP: ${dev.ipAddress}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showAddDialog = false },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Go to Catalog")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    if (provisioningStep is ProvisioningStep.Idle || provisioningStep is ProvisioningStep.Success) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DeviceRowItem(
    device: IoTDevice,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("device_card_${device.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when (device.type) {
                                    "ROBOT_CAR" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    "CLIMATE_NODE" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                    "SECURITY_CAM" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (device.type) {
                                "ROBOT_CAR" -> Icons.Default.DirectionsCar
                                "CLIMATE_NODE" -> Icons.Default.Thermostat
                                "SECURITY_CAM" -> Icons.Default.Videocam
                                "SMART_AGRI" -> Icons.Default.WaterDrop
                                else -> Icons.Default.Memory
                            },
                            contentDescription = device.type,
                            tint = when (device.type) {
                                "ROBOT_CAR" -> MaterialTheme.colorScheme.primary
                                "CLIMATE_NODE" -> MaterialTheme.colorScheme.secondary
                                "SECURITY_CAM" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = device.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${device.typeLabel} • ${device.id}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Delete node
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Node",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tech details block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Network specs
                Column {
                    Text(
                        text = "IP Address: ${device.ipAddress}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "MAC: ${device.macAddress}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Protocol badge
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = device.connectionType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
