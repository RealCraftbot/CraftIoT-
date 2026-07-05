package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val discoveredUnprovisionedDevices by viewModel.discoveredUnprovisionedDevices.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDeviceForEdit by remember { mutableStateOf<IoTDevice?>(null) }

    // Grouping & Filtering State
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("All") }
    var showCustomGroupDialog by remember { mutableStateOf(false) }
    var customGroupInput by remember { mutableStateOf("") }
    val definedGroups = remember(devices) {
        val groups = devices.map { it.deviceGroup }.filter { it.isNotBlank() }.distinct().toMutableList()
        if (!groups.contains("Home")) groups.add("Home")
        if (!groups.contains("Office")) groups.add("Office")
        if (!groups.contains("Greenhouse")) groups.add("Greenhouse")
        groups
    }

    // Forms for provisioning
    var newDeviceName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ROBOT_CAR") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var provisioningTab by remember { mutableStateOf(0) } // 0: BLE + Wi-Fi, 1: QR Pairing
    var customQrInput by remember { mutableStateOf("") }

    // Active Devices Filtering
    val filteredDevices = remember(devices, searchQuery, selectedGroupFilter) {
        devices.filter { device ->
            val matchesSearch = device.name.contains(searchQuery, ignoreCase = true) || 
                                device.id.contains(searchQuery, ignoreCase = true)
            val matchesGroup = selectedGroupFilter == "All" || device.deviceGroup == selectedGroupFilter
            matchesSearch && matchesGroup
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("device_catalog_root")) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "IoT Device Hub",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Deploy, group, and manage your smart ESP32 nodes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.discoverDevices() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .size(44.dp)
                        ) {
                            if (isDiscovering) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, "Discover Devices", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Deploy Node Action Button
                        Button(
                            onClick = {
                                newDeviceName = ""
                                selectedType = "ROBOT_CAR"
                                wifiSsid = ""
                                wifiPassword = ""
                                customQrInput = ""
                                showAddDialog = true
                                viewModel.resetProvisioning()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("add_device_btn")
                        ) {
                            Icon(Icons.Default.Add, "Deploy")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deploy Node", fontWeight = FontWeight.Bold)
                        }

                        // Network Scan Action Button
                        OutlinedButton(
                            onClick = { viewModel.discoverDevices() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Wifi, "Scan Subnet")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Subnet")
                        }
                    }
                }
            }

            // 2. Search and Network Discovery Banners
            AnimatedVisibility(
                visible = discoveredUnprovisionedDevices.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, "Discovery", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                text = "Discovered (${discoveredUnprovisionedDevices.size}) Unprovisioned Nodes Nearby",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        discoveredUnprovisionedDevices.forEach { discDevice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(discDevice.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("IP: ${discDevice.ipAddress} | MAC: ${discDevice.macAddress}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                var showGroupPickerForDisc by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { showGroupPickerForDisc = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Pair & Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showGroupPickerForDisc) {
                                    AlertDialog(
                                        onDismissRequest = { showGroupPickerForDisc = false },
                                        title = { Text("Assign Group to Node") },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Select target deployment group for ${discDevice.name}:")
                                                definedGroups.forEach { grp ->
                                                    Button(
                                                        onClick = {
                                                            viewModel.provisionDiscoveredDevice(discDevice, grp)
                                                            showGroupPickerForDisc = false
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                                    ) {
                                                        Text(grp)
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Filtering & Search Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Node Name or ID...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // 4. Custom Folder / Group chips row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedGroupFilter == "All",
                        onClick = { selectedGroupFilter = "All" },
                        label = { Text("📁 All Nodes") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(definedGroups) { group ->
                    FilterChip(
                        selected = selectedGroupFilter == group,
                        onClick = { selectedGroupFilter = group },
                        label = { Text("📂 $group") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    InputChip(
                        selected = false,
                        onClick = { showCustomGroupDialog = true },
                        label = { Text("+ Custom Group") },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Add, "Add Group") }
                    )
                }
            }

            // 5. Device Catalog List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredDevices.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Nodes Found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Try shifting filters or clicking Deploy Node to connect equipment.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    items(filteredDevices) { device ->
                        EnhancedDeviceCard(
                            device = device,
                            onEdit = { selectedDeviceForEdit = device },
                            onSelect = { viewModel.selectDevice(device.id) },
                            onToggleOffline = { viewModel.toggleDeviceOnlineStatus(device.id) }
                        )
                    }
                }
            }
        }

        // --- Custom Group Creation Dialog ---
        if (showCustomGroupDialog) {
            AlertDialog(
                onDismissRequest = { showCustomGroupDialog = false },
                title = { Text("Create Custom Deployment Folder") },
                text = {
                    Column {
                        Text("Add a logical organizational group category:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customGroupInput,
                            onValueChange = { customGroupInput = it },
                            placeholder = { Text("e.g. Garden, Lab B, Server Rack") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customGroupInput.isNotBlank()) {
                                if (!definedGroups.contains(customGroupInput)) {
                                    selectedGroupFilter = customGroupInput
                                }
                                showCustomGroupDialog = false
                            }
                        }
                    ) {
                        Text("Add Folder")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomGroupDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Device Editing & Customization Sheet (Dialog) ---
        if (selectedDeviceForEdit != null) {
            val device = selectedDeviceForEdit!!
            var editNameInput by remember { mutableStateOf(device.name) }
            var editGroupInput by remember { mutableStateOf(device.deviceGroup) }
            var showGroupDropdown by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { selectedDeviceForEdit = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Settings, "Edit", tint = MaterialTheme.colorScheme.primary)
                        Text("Configure Device Specs", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Modifying unique Node configuration ID: ${device.id}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                        OutlinedTextField(
                            value = editNameInput,
                            onValueChange = { editNameInput = it },
                            label = { Text("Device Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Group Selector Dropdown / Manual entry
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editGroupInput,
                                onValueChange = { editGroupInput = it },
                                label = { Text("Deployment Group / Category") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showGroupDropdown = !showGroupDropdown }) {
                                        Icon(Icons.Default.Folder, "Select Group")
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = showGroupDropdown,
                                onDismissRequest = { showGroupDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                definedGroups.forEach { grp ->
                                    DropdownMenuItem(
                                        text = { Text(grp) },
                                        onClick = {
                                            editGroupInput = grp
                                            showGroupDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Simulated Status Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Online Status Relay", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (device.isOffline) "Status: OFFLINE (Simulated)" else "Status: ONLINE (Active)", fontSize = 11.sp)
                            }
                            Switch(
                                checked = !device.isOffline,
                                onCheckedChange = { 
                                    viewModel.toggleDeviceOnlineStatus(device.id)
                                    // Refresh status in dialog state if needed
                                    selectedDeviceForEdit = device.copy(status = if (device.isOffline) "ONLINE" else "OFFLINE")
                                }
                            )
                        }

                        // Diagnostic Technical Specs
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🔧 Telemetry Metadata", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text("• IPv4 Address: ${device.ipAddress}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("• MAC Node ID: ${device.macAddress}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("• Protocol Driver: ${device.connectionType}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("• Microcontroller Type: ESP32 Core Dual-LX6", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editNameInput.isNotBlank()) {
                                viewModel.renameDevice(device.id, editNameInput)
                                viewModel.updateDeviceGroup(device.id, editGroupInput)
                                selectedDeviceForEdit = null
                            }
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { 
                                viewModel.deleteDevice(device)
                                selectedDeviceForEdit = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, "Delete")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Node")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { selectedDeviceForEdit = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // --- Provisioning dialogue Dialog ---
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Settings, "Wizard", tint = MaterialTheme.colorScheme.primary)
                            Text("Smart Provisioning Wizard", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Tab Selector
                        if (provisioningStep is ProvisioningStep.Idle) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TabRow(selectedTabIndex = provisioningTab) {
                                Tab(
                                    selected = provisioningTab == 0,
                                    onClick = { provisioningTab = 0 },
                                    text = { Text("BLE & Wi-Fi", fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = provisioningTab == 1,
                                    onClick = { provisioningTab = 1 },
                                    text = { Text("QR Auto-Pair", fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when (provisioningStep) {
                            is ProvisioningStep.Idle -> {
                                if (provisioningTab == 0) {
                                    // BLE + Wi-Fi Flow
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Mount an unprovisioned ESP32 board over a secure Bluetooth Low Energy (BLE) link, then pass local router keys.", fontSize = 11.sp)
                                        
                                        OutlinedTextField(
                                            value = newDeviceName,
                                            onValueChange = { newDeviceName = it },
                                            label = { Text("Node Name Label") },
                                            placeholder = { Text("e.g. Workshop Air Con") },
                                            modifier = Modifier.fillMaxWidth().testTag("device_name_field"),
                                            singleLine = true
                                        )

                                        Text("MCU Node Template Core:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val types = listOf(
                                                "ROBOT_CAR" to "Rover",
                                                "CLIMATE_NODE" to "Climate",
                                                "SECURITY_CAM" to "Drone",
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
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.weight(1f).height(32.dp)
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
                                            Icon(Icons.Default.Bluetooth, "BLE Scan")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Scan Bluetooth Signal")
                                        }
                                    }
                                } else {
                                    // QR Auto-Pairing Tab
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Scan a physical ESP32 QR specification matrix. This immediately transfers board MAC address, template model, and target workspace folder.", fontSize = 11.sp)

                                        // Mock scanning card frame
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.Videocam, "Camera View", tint = Color.Green, modifier = Modifier.size(32.dp))
                                                    Text("SIMULATED LENS VIEWFINDER ACTIVE", color = Color.Green, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                    Text("Point camera at device pairing barcode", color = Color.Gray, fontSize = 9.sp)
                                                }
                                            }
                                        }

                                        Text("Select Pre-Configured Test QR Code:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        
                                        val mockQrs = listOf(
                                            "name:Smart Climate Station,type:CLIMATE_NODE,group:Greenhouse" to "Greenhouse Climate QR",
                                            "name:Yard Hydroponics,type:SMART_AGRI,group:Yard" to "Agri Valve QR",
                                            "name:Rover Robot Car,type:ROBOT_CAR,group:Lab" to "Rover Robot QR"
                                        )

                                        mockQrs.forEach { (payload, label) ->
                                            OutlinedButton(
                                                onClick = { viewModel.pairDeviceViaQrCode(payload) },
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, "QR Presets", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(label, fontSize = 11.sp)
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        OutlinedTextField(
                                            value = customQrInput,
                                            onValueChange = { customQrInput = it },
                                            label = { Text("Manual QR Raw Payload") },
                                            placeholder = { Text("id:ESP_1,name:MyNode,type:SMART_AGRI,group:Home") },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        )

                                        Button(
                                            onClick = {
                                                if (customQrInput.isNotBlank()) {
                                                    viewModel.pairDeviceViaQrCode(customQrInput)
                                                }
                                            },
                                            enabled = customQrInput.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Inject QR Code Content")
                                        }
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
                                    Text("Deployment Folder: ${dev.deviceGroup}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
fun EnhancedDeviceCard(
    device: IoTDevice,
    onEdit: () -> Unit,
    onSelect: () -> Unit,
    onToggleOffline: () -> Unit
) {
    val isOffline = device.isOffline
    val cardAlpha = if (isOffline) 0.65f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("device_card_${device.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isOffline) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isOffline) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon + Name Info + Status Light
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Unique Circular Icon Container
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOffline) {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                } else {
                                    when (device.type) {
                                        "ROBOT_CAR" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        "CLIMATE_NODE" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                        "SECURITY_CAM" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                        "SMART_AGRI" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    }
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
                            tint = if (isOffline) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                when (device.type) {
                                    "ROBOT_CAR" -> MaterialTheme.colorScheme.primary
                                    "CLIMATE_NODE" -> MaterialTheme.colorScheme.secondary
                                    "SECURITY_CAM" -> MaterialTheme.colorScheme.tertiary
                                    "SMART_AGRI" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Folder Indicator Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = device.deviceGroup,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = "${device.typeLabel} • ${device.id}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }

                // Options/Edit Trigger
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Status Beacon Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOffline) Color(0xFFD32F2F) else Color(0xFF388E3C)
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Specs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Info Details (MAC, IP, Connection Type)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Info, "IP Address", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                        Text(
                            text = "IPv4: ${device.ipAddress}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * cardAlpha)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Settings, "MAC Address", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                        Text(
                            text = "MAC: ${device.macAddress}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f * cardAlpha)
                        )
                    }
                }

                // Connection protocol / Reconnect badge
                if (isOffline) {
                    Button(
                        onClick = onToggleOffline,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, "Reconnect", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("OFFLINE (Wake)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = device.connectionType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
