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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationRule
import com.example.data.model.FirmwareRelease
import com.example.ui.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val firmwares by viewModel.firmwares.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val otaProgress by viewModel.otaProgress.collectAsState()
    val otaTargetId by viewModel.otaTargetDeviceId.collectAsState()

    var showRuleDialog by remember { mutableStateOf(false) }

    // Automation Rule forms
    var ruleName by remember { mutableStateOf("") }
    var triggerDeviceId by remember { mutableStateOf("") }
    var triggerMetric by remember { mutableStateOf("temp") }
    var triggerVal by remember { mutableStateOf("30.0") }
    var actionDeviceId by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("TURN_ON") }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("marketplace_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Section Header
        item {
            Column {
                Text(
                    text = "Automations & OTAs",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Manage serverless rules, premium plugins, and wireless firmware flashes.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- SECTION 1: AUTOMATIONS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RULE-BASED TRIGGER ENGINE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                TextButton(
                    onClick = {
                        ruleName = ""
                        triggerDeviceId = devices.firstOrNull()?.id ?: ""
                        triggerMetric = "temp"
                        triggerVal = "30.0"
                        actionDeviceId = devices.firstOrNull()?.id ?: ""
                        actionType = "TURN_ON"
                        showRuleDialog = true
                    },
                    modifier = Modifier.testTag("add_rule_btn")
                ) {
                    Icon(Icons.Default.Add, "Add Trigger", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule", fontSize = 12.sp)
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Text("No active rules loaded. Click Add Rule to create one.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            items(rules) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("rule_card_${rule.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "IF ${rule.deviceId}.${rule.metric} ${if (rule.operator == "GREATER_THAN") ">" else "<"} ${rule.thresholdValue} -> THEN ${rule.actionDeviceId}.${rule.actionType}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        IconButton(onClick = { viewModel.deleteAutomationRule(rule) }) {
                            Icon(Icons.Default.Delete, "Delete Rule", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // --- SECTION 2: OTA FIRMWARE MANAGEMENT ---
        item {
            Text(
                text = "FIRMWARE OTA MANAGEMENT (OVER-THE-AIR)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (firmwares.isEmpty()) {
            item {
                Text("No compiled firmware binary binaries loaded.", fontSize = 12.sp)
            }
        } else {
            items(firmwares) { fw ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("firmware_card_${fw.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.SystemUpdateAlt, "OTA", tint = MaterialTheme.colorScheme.secondary)
                                Column {
                                    Text("v${fw.version} (${fw.deviceType})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${fw.fileSizeMb} MB • Cached locally", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }

                            // Active progress indicator
                            if (otaTargetId != null && otaProgress != null) {
                                CircularProgressIndicator(
                                    progress = { otaProgress!! },
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Button(
                                    onClick = {
                                        // Pick first matching device to simulate OTA
                                        val targetDev = devices.find { it.type == fw.deviceType }
                                        if (targetDev != null) {
                                            viewModel.startOtaUpdate(targetDev.id, fw.id)
                                        } else {
                                            viewModel.addSerialLog("⚠️ OTA FAILED: Cannot locate any connected physical hardware matching type '${fw.deviceType}'")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("flash_ota_btn_${fw.id}")
                                ) {
                                    Text("Flash OTA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = fw.releaseNotes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // --- SECTION 3: PREMIUM SUBSCRIPTIONS & MARKETPLACE ---
        item {
            Text(
                text = "CRAFTIOT LICENSE & PLUGINS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Premium Tier card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ENTERPRISE PRO PLATFORM",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("TRIAL UNLOCKED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Unlimited ESP32 cluster controller, secure PostgreSQL database replication syncs, and dedicated REST OTA compilation channels activated.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Marketplace items
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MarketPluginCard(
                    title = "RTMP Stream Mod",
                    desc = "Direct low-latency ESP32-CAM stream pipeline.",
                    price = "$4.99",
                    icon = Icons.Default.Videocam,
                    modifier = Modifier.weight(1f)
                )
                MarketPluginCard(
                    title = "GPS Node Sync",
                    desc = "NMEA tracking & geofence trigger routines.",
                    price = "$2.99",
                    icon = Icons.Default.Map,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Add automation rule dialog
    if (showRuleDialog) {
        AlertDialog(
            onDismissRequest = { showRuleDialog = false },
            title = { Text("Configure Automation Rule", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        label = { Text("Rule Display Name") },
                        placeholder = { Text("Auto Cooler, Speed Warning") },
                        modifier = Modifier.fillMaxWidth().testTag("rule_name_field"),
                        singleLine = true
                    )

                    Text("TRIGGER CLAUSE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    if (devices.isEmpty()) {
                        Text("No registered nodes to bind rule triggers.", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    } else {
                        // Trigger device selection
                        var expandedTrigger by remember { mutableStateOf(false) }
                        val tDev = devices.find { it.id == triggerDeviceId } ?: devices.first()
                        if (triggerDeviceId.isEmpty()) triggerDeviceId = tDev.id

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedTrigger = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Device: ${tDev.name}")
                            }
                            DropdownMenu(
                                expanded = expandedTrigger,
                                onDismissRequest = { expandedTrigger = false }
                            ) {
                                devices.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.name) },
                                        onClick = {
                                            triggerDeviceId = d.id
                                            expandedTrigger = false
                                        }
                                    )
                                }
                            }
                        }

                        // Trigger metric Selection
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val metrics = listOf("temp" to "Temp", "speed" to "Speed", "humidity" to "Humid", "battery" to "Battery")
                            metrics.forEach { (m, lbl) ->
                                FilterChip(
                                    selected = triggerMetric == m,
                                    onClick = { triggerMetric = m },
                                    label = { Text(lbl) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = triggerVal,
                            onValueChange = { triggerVal = it },
                            label = { Text("Trigger Threshold Bound Value") },
                            modifier = Modifier.fillMaxWidth().testTag("rule_threshold_field"),
                            singleLine = true
                        )

                        Text("CONSEQUENTIAL ACTION CLAUSE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                        // Action device selection
                        var expandedAction by remember { mutableStateOf(false) }
                        val aDev = devices.find { it.id == actionDeviceId } ?: devices.first()
                        if (actionDeviceId.isEmpty()) actionDeviceId = aDev.id

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedAction = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Action On: ${aDev.name}")
                            }
                            DropdownMenu(
                                expanded = expandedAction,
                                onDismissRequest = { expandedAction = false }
                            ) {
                                devices.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.name) },
                                        onClick = {
                                            actionDeviceId = d.id
                                            expandedAction = false
                                        }
                                    )
                                }
                            }
                        }

                        // Action type selection
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val actions = listOf("TURN_ON" to "ON", "TURN_OFF" to "OFF", "ALERT" to "ALERT")
                            actions.forEach { (a, lbl) ->
                                FilterChip(
                                    selected = actionType == a,
                                    onClick = { actionType = a },
                                    label = { Text(lbl) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleName.isNotBlank() && triggerDeviceId.isNotEmpty() && actionDeviceId.isNotEmpty()) {
                            viewModel.createAutomationRule(
                                AutomationRule(
                                    name = ruleName,
                                    deviceId = triggerDeviceId,
                                    metric = triggerMetric,
                                    operator = "GREATER_THAN",
                                    thresholdValue = triggerVal.toFloatOrNull() ?: 30.0f,
                                    actionDeviceId = actionDeviceId,
                                    actionType = actionType,
                                    isActive = true
                                )
                            )
                            showRuleDialog = false
                        }
                    },
                    enabled = ruleName.isNotBlank() && triggerDeviceId.isNotEmpty() && actionDeviceId.isNotEmpty(),
                    modifier = Modifier.testTag("rule_confirm_btn")
                ) {
                    Text("Apply Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRuleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MarketPluginCard(
    title: String,
    desc: String,
    price: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), minLines = 2, maxLines = 2)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(price, fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("BUY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
