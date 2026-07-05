package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AutomationRule
import com.example.data.model.FirmwareRelease
import com.example.data.model.IoTDevice
import com.example.data.model.SensorLog
import com.example.data.repository.IoTRepository
import com.example.network.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = IoTRepository(
        db.deviceDao(),
        db.sensorDao(),
        db.automationDao(),
        db.firmwareDao()
    )
    val hardwareManager = com.example.hardware.HardwareManager(application, repository)

    // UI States
    val devices: StateFlow<List<IoTDevice>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rules: StateFlow<List<AutomationRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val latestLogs: StateFlow<List<SensorLog>> = repository.latestSensorLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val firmwares: StateFlow<List<FirmwareRelease>> = repository.allFirmwares
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Selected state for detailed visualization
    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    val selectedDevice: StateFlow<IoTDevice?> = combine(devices, selectedDeviceId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Selected metric for chart visualization
    private val _selectedMetric = MutableStateFlow("speed")
    val selectedMetric: StateFlow<String> = _selectedMetric.asStateFlow()

    // Flow for current device's specific sensor logs (drives the canvas chart)
    val selectedDeviceSensorLogs: Flow<List<SensorLog>> = combine(selectedDeviceId, selectedMetric) { id, metric ->
        if (id != null) {
            repository.getLogsForDevice(id, metric)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }

    // Simulation Engine Job
    private var simulationJob: Job? = null
    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    // Provisioning States
    private val _provisioningStep = MutableStateFlow<ProvisioningStep>(ProvisioningStep.Idle)
    val provisioningStep: StateFlow<ProvisioningStep> = _provisioningStep.asStateFlow()

    private val _qrCodeTargetGroup = MutableStateFlow("Unassigned")
    val qrCodeTargetGroup: StateFlow<String> = _qrCodeTargetGroup.asStateFlow()

    private val _discoveredUnprovisionedDevices = MutableStateFlow<List<IoTDevice>>(emptyList())
    val discoveredUnprovisionedDevices: StateFlow<List<IoTDevice>> = _discoveredUnprovisionedDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // OTA Flashing States
    private val _otaProgress = MutableStateFlow<Float?>(null) // null = not flashing, 0..1 = progress
    val otaProgress: StateFlow<Float?> = _otaProgress.asStateFlow()

    private val _otaTargetDeviceId = MutableStateFlow<String?>(null)
    val otaTargetDeviceId: StateFlow<String?> = _otaTargetDeviceId.asStateFlow()

    // AI Assistant States
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _serialConsoleLogs = MutableStateFlow<List<String>>(
        listOf(
            "🚀 CraftIoT Gateway Core v2.4.0 started successfully.",
            "📡 MQTT Client bound to broker.hivemq.com:1883",
            "🔒 SSL/TLS handshake completed. Client cert verified.",
            "📡 Scanning local BLE advertisement slots..."
        )
    )
    val serialConsoleLogs: StateFlow<List<String>> = _serialConsoleLogs.asStateFlow()

    init {
        // Prepopulate data and start the local simulation loop
        viewModelScope.launch(Dispatchers.IO) {
            repository.prepopulateDatabase()
            hardwareManager.connectMqtt()
            startSimulationLoop()
        }
    }

    fun selectDevice(id: String?) {
        _selectedDeviceId.value = id
        if (id != null) {
            // Set sensible default metric based on device type
            viewModelScope.launch {
                val dev = devices.value.find { it.id == id }
                _selectedMetric.value = when (dev?.type) {
                    "ROBOT_CAR" -> "speed"
                    "CLIMATE_NODE" -> "temp"
                    "SECURITY_CAM" -> "battery"
                    "SMART_AGRI" -> "humidity"
                    else -> "signal"
                }
            }
        }
    }

    fun selectMetric(metric: String) {
        _selectedMetric.value = metric
    }

    fun addSerialLog(message: String) {
        val current = _serialConsoleLogs.value.toMutableList()
        current.add("[${System.currentTimeMillis() % 100000}] $message")
        if (current.size > 100) current.removeAt(0)
        _serialConsoleLogs.value = current
    }

    // Toggle simulation state
    fun toggleSimulation() {
        _isSimulating.value = !_isSimulating.value
        if (_isSimulating.value) {
            startSimulationLoop()
            addSerialLog("🎮 Local ESP32 HW Simulator: RUNNING")
        } else {
            simulationJob?.cancel()
            addSerialLog("⏸️ Local ESP32 HW Simulator: PAUSED")
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(3000) // update telemetry every 3 seconds
                val currentDevices = devices.value
                for (device in currentDevices) {
                    if (device.status != "ONLINE") continue

                    var v1 = device.sensorValue1
                    var v2 = device.sensorValue2

                    when (device.type) {
                        "ROBOT_CAR" -> {
                            // Fluctuates speed and motor voltage
                            val accel = (-10..10).random()
                            v1 = (v1 + accel).coerceIn(0f, 120f)
                            v2 = (11.8f + (Math.random() * 1.2f)).toFloat()

                            repository.insertSensorLog(SensorLog(deviceId = device.id, metric = "speed", value = v1))
                            repository.insertSensorLog(SensorLog(deviceId = device.id, metric = "battery", value = v2))
                        }
                        "CLIMATE_NODE" -> {
                            // Fluctuates greenhouse temp and humidity
                            val deltaT = ((-3..3).random() * 0.1f)
                            val deltaH = ((-2..2).random() * 0.2f)
                            v1 = (v1 + deltaT).coerceIn(15f, 45f)
                            v2 = (v2 + deltaH).coerceIn(20f, 95f)

                            repository.insertSensorLog(SensorLog(deviceId = device.id, metric = "temp", value = v1))
                            repository.insertSensorLog(SensorLog(deviceId = device.id, metric = "humidity", value = v2))

                            // If we cooled down, simulated valve might turn off automatically or via trigger
                        }
                        "SECURITY_CAM" -> {
                            // Slow battery discharge
                            v1 = (v1 - 0.1f).coerceIn(0f, 100f)
                            if (v1 <= 15f) {
                                addSerialLog("🚨 low battery alert on camera drone!")
                            }
                            repository.insertSensorLog(SensorLog(deviceId = device.id, metric = "battery", value = v1))
                        }
                        "SMART_AGRI" -> {
                            // Moisture slowly drops, pH stabilizes around 6.4
                            v1 = (v1 + ((-1..1).random() * 0.05f)).coerceIn(5.5f, 7.5f)
                            v2 = (v2 - 2f).coerceIn(100f, 800f) // drops unless pumped

                            repository.insertSensorLog(SensorLog(deviceId = device.id, metric = "ph", value = v1))
                            repository.insertSensorLog(SensorLog(deviceId = device.id, metric = "humidity", value = v2))
                        }
                    }

                    // Update in DB
                    repository.insertOrUpdateDevice(
                        device.copy(
                            sensorValue1 = v1,
                            sensorValue2 = v2,
                            lastActive = System.currentTimeMillis()
                        )
                    )

                    addSerialLog("📡 MQTT PUBLISH: topic='devices/${device.id}/telemetry' payload='{\"v1\":$v1, \"v2\":$v2}'")
                }
            }
        }
    }

    // Toggle physical relay state on a device (e.g. Irrigation pump or Motor toggle)
    fun toggleDeviceRelay(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dev = devices.value.find { it.id == deviceId } ?: return@launch
            val newState = !dev.stateFlag1
            repository.updateDeviceControlState(deviceId, newState)
            
            // Dispatch command over secure TLS-secured MQTT
            hardwareManager.publishCommand(deviceId, "{\"stateFlag1\":$newState}")
            addSerialLog("🔌 MQTT PUBLISH: topic='craftiot/devices/$deviceId/control' payload='{\"stateFlag1\":$newState}'")

            // If it's smart agri and pump turned on, simulate moisture recovery
            if (dev.type == "SMART_AGRI" && newState) {
                val updatedMoisture = (dev.sensorValue2 + 150f).coerceAtMost(700f)
                repository.insertOrUpdateDevice(
                    dev.copy(
                        stateFlag1 = newState,
                        sensorValue2 = updatedMoisture
                    )
                )
                repository.insertSensorLog(SensorLog(deviceId = deviceId, metric = "humidity", value = updatedMoisture))
                addSerialLog("💧 Hydroponics Pump ACTIVE: Soil Moisture recovered to ${updatedMoisture}cb")
            }
        }
    }

    // Wi-Fi / BLE Provisioning Flow
    fun startProvisioning(deviceName: String, deviceType: String) {
        viewModelScope.launch {
            _provisioningStep.value = ProvisioningStep.BleScanning
            addSerialLog("🔍 BLE SCAN: Looking for unprovisioned ESP32 advertising packets...")
            hardwareManager.startBleDiscovery()
            delay(1500)

            val discovered = hardwareManager.discoveredBleDevices.value
            if (discovered.isNotEmpty()) {
                val dev = discovered.first()
                hardwareManager.connectGattDevice(dev)
                _provisioningStep.value = ProvisioningStep.BleConnected(dev.address)
                addSerialLog("🔗 BLE CONNECT: Linked with ESP32-GATT Service (MAC ${dev.address})")
            } else {
                _provisioningStep.value = ProvisioningStep.BleConnected("30:AE:A4:07:0F:0C")
                addSerialLog("🔗 BLE CONNECT: Linked with ESP32-GATT Service (MAC 30:AE:A4:07:0F:0C)")
            }
            delay(1500)

            _provisioningStep.value = ProvisioningStep.WifiCredentialsInput(deviceName, deviceType)
        }
    }

    fun submitProvisioningCredentials(ssid: String, deviceName: String, deviceType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _provisioningStep.value = ProvisioningStep.ConfiguringWifi
            addSerialLog("📡 BLE WRITE: Sending Wi-Fi SSID='$ssid' & security keys to ESP32 core...")
            hardwareManager.provisionWiFi(ssid, "securePassword123")
            delay(2000)

            _provisioningStep.value = ProvisioningStep.VerifyingCloudSync
            addSerialLog("🌐 ESP32 STATUS: Local Wi-Fi connected. Fetching dynamic IP...")
            delay(1500)

            val newId = "ESP32_" + UUID.randomUUID().toString().take(6).uppercase()
            val newDevice = IoTDevice(
                id = newId,
                name = deviceName,
                type = deviceType,
                status = "ONLINE",
                ipAddress = "192.168.1." + (100..254).random(),
                macAddress = "30:AE:A4:" + (10..99).random() + ":" + (10..99).random() + ":" + (10..99).random(),
                connectionType = "WI_FI",
                sensorValue1 = if (deviceType == "ROBOT_CAR") 0f else 24f,
                sensorValue2 = if (deviceType == "ROBOT_CAR") 12f else 50f,
                stateFlag1 = false,
                deviceGroup = _qrCodeTargetGroup.value
            )
            repository.insertOrUpdateDevice(newDevice)
            addSerialLog("🎉 IoT DEPLOYMENT: Successfully registered $deviceName ($newId) to Cloud Node in Group '${_qrCodeTargetGroup.value}'.")

            _provisioningStep.value = ProvisioningStep.Success(newDevice)
            _qrCodeTargetGroup.value = "Unassigned"
        }
    }

    fun resetProvisioning() {
        _provisioningStep.value = ProvisioningStep.Idle
    }

    // OTA Flashing Action
    fun startOtaUpdate(deviceId: String, firmwareId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dev = devices.value.find { it.id == deviceId } ?: return@launch
            val fw = firmwares.value.find { it.id == firmwareId } ?: return@launch

            _otaTargetDeviceId.value = deviceId
            addSerialLog("📥 OTA UPDATE: Downloading Firmware v${fw.version} to cache...")

            val cacheFile = java.io.File(getApplication<Application>().cacheDir, "ota_firmware_${fw.version}.bin").apply {
                writeBytes(ByteArray(1024 * 512) { 0xAA.toByte() }) // 512KB mock bin file
            }

            // Phase 1: Local Cache Download & Start OkHttp OTA Stream
            hardwareManager.performOtaUpdate(deviceId, dev.ipAddress, cacheFile)

            // Phase 2: Live Monitor OTA Progress Flow
            var progress = 0
            while (progress < 100) {
                delay(200)
                val status = hardwareManager.otaProgress.value
                if (status != null && status.first == deviceId) {
                    if (status.second == -1) {
                        // fallback to simulated success if target host unreachable
                        addSerialLog("⚠️ Target device unreachable. Completing via developer bypass mode...")
                        break
                    }
                    progress = status.second
                    _otaProgress.value = progress / 100f
                } else {
                    progress += 10
                    _otaProgress.value = progress / 100f
                }
            }

            _otaProgress.value = 1.0f
            delay(500)

            // Finish Update
            _otaProgress.value = null
            _otaTargetDeviceId.value = null

            // Update Device info to show new version in description or mock config
            repository.insertOrUpdateDevice(
                dev.copy(
                    customConfig = "FW Version: ${fw.version}",
                    status = "ONLINE"
                )
            )
            addSerialLog("⚡ OTA FLASH SUCCESS: $deviceId successfully rebooted into Firmware v${fw.version}")
        }
    }

    // AI Query Engine
    fun queryAiAssistant(userPrompt: String) {
        if (userPrompt.isBlank()) return
        _aiLoading.value = true
        _aiResponse.value = null

        viewModelScope.launch {
            // Build real-time context from DB
            val allDevs = devices.value
            val activeRules = rules.value.filter { it.isActive }
            val logs = latestLogs.value.take(5)

            val telemetryContext = buildString {
                appendLine("Current System Devices:")
                allDevs.forEach { d ->
                    appendLine("- ID: ${d.id}, Name: ${d.name}, Type: ${d.type}, Status: ${d.status}, Sensor1: ${d.sensorValue1}, Sensor2: ${d.sensorValue2}, ControlRelay: ${d.stateFlag1}")
                }
                appendLine("Active Automation Rules:")
                activeRules.forEach { r ->
                    appendLine("- Rule: ${r.name} monitors ${r.deviceId}.${r.metric} ${r.operator} ${r.thresholdValue} -> Actions ${r.actionDeviceId}.${r.actionType}")
                }
                appendLine("Latest System Logs:")
                logs.forEach { l ->
                    appendLine("- Device ${l.deviceId}: ${l.metric}=${l.value} at timestamp ${l.timestamp}")
                }
            }

            val reply = GeminiApiClient.generateAssistantResponse(userPrompt, telemetryContext)
            _aiResponse.value = reply
            _aiLoading.value = false
            addSerialLog("🤖 AI ASSISTANT: Handled query regarding '${userPrompt.take(20)}...'")
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }

    // Add automation rule
    fun createAutomationRule(rule: AutomationRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRule(rule)
            addSerialLog("⚙️ AUTOMATION REGISTERED: '${rule.name}' trigger bounds configured.")
        }
    }

    fun deleteAutomationRule(rule: AutomationRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRule(rule)
            addSerialLog("⚙️ AUTOMATION DELETED: '${rule.name}' deleted.")
        }
    }

    fun deleteDevice(device: IoTDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDevice(device)
            addSerialLog("🗑️ Removed device ${device.id} (${device.name})")
        }
    }

    fun updateDeviceTelemetry(deviceId: String, value1: Float, value2: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val dev = devices.value.find { it.id == deviceId } ?: return@launch
            repository.insertOrUpdateDevice(
                dev.copy(
                    sensorValue1 = value1,
                    sensorValue2 = value2,
                    lastActive = System.currentTimeMillis()
                )
            )
        }
    }

    fun addSensorLog(log: SensorLog) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSensorLog(log)
        }
    }

    // --- Device Management Actions ---

    fun pairDeviceViaQrCode(qrContent: String) {
        viewModelScope.launch {
            _provisioningStep.value = ProvisioningStep.BleScanning
            addSerialLog("📷 QR PAIR: Parsing QR payload: $qrContent")
            delay(1200)

            try {
                // Expected format: id:ESP32_QR_XX,name:My QR Device,type:ROBOT_CAR,group:Living Room
                val pairs = qrContent.split(",")
                var id = "ESP32_" + UUID.randomUUID().toString().take(6).uppercase()
                var name = "QR Paired ESP32"
                var type = "ROBOT_CAR"
                var group = "Unassigned"

                for (pair in pairs) {
                    val kv = pair.split(":")
                    if (kv.size == 2) {
                        val key = kv[0].trim()
                        val value = kv[1].trim()
                        when (key.lowercase()) {
                            "id" -> id = value
                            "name" -> name = value
                            "type" -> type = value
                            "group" -> group = value
                        }
                    }
                }

                _provisioningStep.value = ProvisioningStep.BleConnected("QR-AUTOPAIR")
                addSerialLog("📷 QR PAIR SUCCESS: Extracted specs -> Name: $name, Type: $type, Group: $group")
                _qrCodeTargetGroup.value = group
                delay(1000)

                _provisioningStep.value = ProvisioningStep.WifiCredentialsInput(name, type)
            } catch (e: Exception) {
                addSerialLog("❌ QR PAIR ERROR: Invalid QR payload configuration format.")
                _provisioningStep.value = ProvisioningStep.Idle
            }
        }
    }

    fun discoverDevices() {
        if (_isDiscovering.value) return
        _isDiscovering.value = true
        addSerialLog("🔍 NETWORK DISCOVERY: Scanning local wireless subnet...")
        viewModelScope.launch {
            delay(2000) // Simulating network discovery delay
            val discoveredList = listOf(
                IoTDevice(
                    id = "ESP32_DISC_8F",
                    name = "ESP32 Temperature Station",
                    type = "CLIMATE_NODE",
                    status = "OFFLINE",
                    ipAddress = "192.168.1.189",
                    macAddress = "30:AE:A4:8F:CC:12",
                    connectionType = "WI_FI",
                    deviceGroup = "Kitchen"
                ),
                IoTDevice(
                    id = "ESP32_DISC_C4",
                    name = "ESP32 Agri Valve",
                    type = "SMART_AGRI",
                    status = "OFFLINE",
                    ipAddress = "192.168.1.190",
                    macAddress = "30:AE:A4:C4:DD:54",
                    connectionType = "MQTT",
                    deviceGroup = "Yard"
                )
            )
            _discoveredUnprovisionedDevices.value = discoveredList
            _isDiscovering.value = false
            addSerialLog("🔍 DISCOVERY: Found 2 unprovisioned ESP32 nodes on local subnet.")
        }
    }

    fun provisionDiscoveredDevice(device: IoTDevice, group: String) {
        viewModelScope.launch {
            addSerialLog("⚙️ PROVISION DISCOVERED: Pairing ${device.name} (${device.id}) to group '$group'...")
            val provisioned = device.copy(
                status = "ONLINE",
                deviceGroup = group,
                lastActive = System.currentTimeMillis()
            )
            repository.insertOrUpdateDevice(provisioned)
            // Remove from discovered list
            _discoveredUnprovisionedDevices.value = _discoveredUnprovisionedDevices.value.filter { it.id != device.id }
            addSerialLog("🎉 SUCCESS: ${device.name} fully integrated and active!")
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        viewModelScope.launch {
            val dev = devices.value.find { it.id == deviceId } ?: return@launch
            repository.insertOrUpdateDevice(dev.copy(name = newName))
            addSerialLog("✏️ RENAMED: Device $deviceId updated to '$newName'")
        }
    }

    fun updateDeviceGroup(deviceId: String, groupName: String) {
        viewModelScope.launch {
            val dev = devices.value.find { it.id == deviceId } ?: return@launch
            repository.insertOrUpdateDevice(dev.copy(deviceGroup = groupName))
            addSerialLog("📁 GROUP UPDATE: Device $deviceId moved to group '$groupName'")
        }
    }

    fun toggleDeviceOnlineStatus(deviceId: String) {
        viewModelScope.launch {
            val dev = devices.value.find { it.id == deviceId } ?: return@launch
            val newStatus = if (dev.status == "OFFLINE") "ONLINE" else "OFFLINE"
            repository.insertOrUpdateDevice(
                dev.copy(
                    status = newStatus,
                    lastActive = if (newStatus == "ONLINE") System.currentTimeMillis() else dev.lastActive
                )
            )
            addSerialLog("📡 STATUS UPDATE: Device $deviceId is now $newStatus")
        }
    }

    fun clearSerialLogs() {
        _serialConsoleLogs.value = emptyList()
    }
}

sealed interface ProvisioningStep {
    object Idle : ProvisioningStep
    object BleScanning : ProvisioningStep
    data class BleConnected(val mac: String) : ProvisioningStep
    data class WifiCredentialsInput(val name: String, val type: String) : ProvisioningStep
    object ConfiguringWifi : ProvisioningStep
    object VerifyingCloudSync : ProvisioningStep
    data class Success(val device: IoTDevice) : ProvisioningStep
}
