package com.example.data.repository

import com.example.data.local.DeviceDao
import com.example.data.local.SensorLogDao
import com.example.data.local.AutomationDao
import com.example.data.local.FirmwareDao
import com.example.data.model.IoTDevice
import com.example.data.model.SensorLog
import com.example.data.model.AutomationRule
import com.example.data.model.FirmwareRelease
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class IoTRepository(
    private val deviceDao: DeviceDao,
    private val sensorDao: SensorLogDao,
    private val automationDao: AutomationDao,
    private val firmwareDao: FirmwareDao
) {
    val allDevices: Flow<List<IoTDevice>> = deviceDao.getAllDevices()
    val allRules: Flow<List<AutomationRule>> = automationDao.getAllRules()
    val activeRules: Flow<List<AutomationRule>> = automationDao.getActiveRules()
    val allFirmwares: Flow<List<FirmwareRelease>> = firmwareDao.getAllFirmwares()
    val latestSensorLogs: Flow<List<SensorLog>> = sensorDao.getLatestLogs()

    fun getLogsForDevice(deviceId: String, metric: String): Flow<List<SensorLog>> {
        return sensorDao.getLogsForDevice(deviceId, metric)
    }

    suspend fun insertOrUpdateDevice(device: IoTDevice) {
        deviceDao.insertOrUpdateDevice(device)
    }

    suspend fun deleteDevice(device: IoTDevice) {
        deviceDao.deleteDevice(device)
    }

    suspend fun updateDeviceControlState(id: String, flag: Boolean) {
        deviceDao.updateDeviceControlState(id, flag)
        // Also log this control transition
        val device = deviceDao.getDeviceById(id)
        if (device != null) {
            val logVal = if (flag) 1.0f else 0.0f
            insertSensorLog(SensorLog(deviceId = id, metric = "control_switch", value = logVal))
        }
    }

    suspend fun insertSensorLog(log: SensorLog) {
        sensorDao.insertLog(log)
        // Check automations reactively when a new log is received
        checkAutomationsForMetric(log)
    }

    suspend fun insertRule(rule: AutomationRule) {
        automationDao.insertOrUpdateRule(rule)
    }

    suspend fun deleteRule(rule: AutomationRule) {
        automationDao.deleteRule(rule)
    }

    suspend fun insertFirmware(firmware: FirmwareRelease) {
        firmwareDao.insertFirmware(firmware)
    }

    suspend fun updateFirmwareDownloadStatus(id: String, isDownloaded: Boolean) {
        firmwareDao.updateDownloadStatus(id, isDownloaded)
    }

    private suspend fun checkAutomationsForMetric(log: SensorLog) {
        val rules = automationDao.getActiveRules().first()
        for (rule in rules) {
            if (rule.deviceId == log.deviceId && rule.metric == log.metric) {
                val isTriggered = when (rule.operator) {
                    "GREATER_THAN" -> log.value > rule.thresholdValue
                    "LESS_THAN" -> log.value < rule.thresholdValue
                    else -> false
                }
                if (isTriggered) {
                    // Trigger simulated action!
                    val targetDevice = deviceDao.getDeviceById(rule.actionDeviceId)
                    if (targetDevice != null) {
                        when (rule.actionType) {
                            "TURN_ON" -> {
                                deviceDao.updateDeviceControlState(rule.actionDeviceId, true)
                            }
                            "TURN_OFF" -> {
                                deviceDao.updateDeviceControlState(rule.actionDeviceId, false)
                            }
                            "SET_SPEED" -> {
                                deviceDao.updateDeviceTelemetry(
                                    rule.actionDeviceId,
                                    val1 = rule.actionValue, // Update speed or value
                                    val2 = targetDevice.sensorValue2,
                                    timestamp = System.currentTimeMillis()
                                )
                            }
                            "ALERT" -> {
                                // Insert alert log
                                sensorDao.insertLog(
                                    SensorLog(
                                        deviceId = rule.actionDeviceId,
                                        metric = "alert",
                                        value = rule.actionValue,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Prepopulate initial devices so first boot is visually rich and interesting
    suspend fun prepopulateDatabase() {
        val devices = deviceDao.getAllDevices().first()
        if (devices.isEmpty()) {
            // 1. Robot Car
            deviceDao.insertOrUpdateDevice(
                IoTDevice(
                    id = "ESP32_CAR_01",
                    name = "Rover Robot Car",
                    type = "ROBOT_CAR",
                    status = "ONLINE",
                    ipAddress = "192.168.4.1",
                    macAddress = "30:AE:A4:07:0F:0C",
                    connectionType = "WI_FI",
                    sensorValue1 = 45.0f, // Current Speed (km/h)
                    sensorValue2 = 12.4f, // Motor voltage (V)
                    stateFlag1 = false    // Motor ON/OFF state
                )
            )

            // 2. Climate Node
            deviceDao.insertOrUpdateDevice(
                IoTDevice(
                    id = "ESP32_CLIMATE_02",
                    name = "Greenhouse Climate",
                    type = "CLIMATE_NODE",
                    status = "ONLINE",
                    ipAddress = "192.168.1.15",
                    macAddress = "24:0A:C4:F3:11:80",
                    connectionType = "MQTT",
                    sensorValue1 = 28.4f, // Temperature (°C)
                    sensorValue2 = 62.1f, // Humidity (%)
                    stateFlag1 = true     // Irrigation Water Valve state
                )
            )

            // 3. Drone Camera
            deviceDao.insertOrUpdateDevice(
                IoTDevice(
                    id = "ESP32_DRONE_03",
                    name = "Copter Camera Drone",
                    type = "SECURITY_CAM",
                    status = "PROVISIONING",
                    ipAddress = "192.168.1.42",
                    macAddress = "50:02:91:DE:3C:A4",
                    connectionType = "BLE",
                    sensorValue1 = 88.0f, // Battery Level (%)
                    sensorValue2 = 1.2f,  // Stream bandwidth (Mbps)
                    stateFlag1 = false    // Camera recording state
                )
            )

            // 4. Smart Agri Sensor
            deviceDao.insertOrUpdateDevice(
                IoTDevice(
                    id = "ESP32_AGRI_04",
                    name = "Soil Hydroponics System",
                    type = "SMART_AGRI",
                    status = "ONLINE",
                    ipAddress = "192.168.1.18",
                    macAddress = "3C:71:BF:C0:5E:20",
                    connectionType = "MQTT",
                    sensorValue1 = 6.4f,  // pH Level
                    sensorValue2 = 340.0f,// Soil moisture (cb)
                    stateFlag1 = false    // Water pump state
                )
            )

            // Prepopulate some historical telemetry logs for charts
            val now = System.currentTimeMillis()
            for (i in 0..15) {
                val tOffset = now - (15 - i) * 60000L
                sensorDao.insertLog(
                    SensorLog(
                        deviceId = "ESP32_CAR_01",
                        metric = "speed",
                        value = 30.0f + (Math.sin(i.toDouble() / 2.0) * 15.0f).toFloat(),
                        timestamp = tOffset
                    )
                )
                sensorDao.insertLog(
                    SensorLog(
                        deviceId = "ESP32_CLIMATE_02",
                        metric = "temp",
                        value = 26.0f + (i * 0.15f) + (Math.cos(i.toDouble()) * 0.5f).toFloat(),
                        timestamp = tOffset
                    )
                )
            }

            // Prepopulate an automation rule: "If Greenhouse Temp > 30.0 °C, turn on Water Valve (ESP32_CLIMATE_02)"
            automationDao.insertOrUpdateRule(
                AutomationRule(
                    id = 1L,
                    name = "Greenhouse Auto Cool",
                    deviceId = "ESP32_CLIMATE_02",
                    metric = "temp",
                    operator = "GREATER_THAN",
                    thresholdValue = 30.0f,
                    actionDeviceId = "ESP32_CLIMATE_02",
                    actionType = "TURN_ON",
                    actionValue = 1.0f,
                    isActive = true
                )
            )

            // Prepopulate a firmware OTA update
            firmwareDao.insertFirmware(
                FirmwareRelease(
                    id = "FW_ESP32_ROVER_130",
                    version = "1.3.0",
                    deviceType = "ROBOT_CAR",
                    releaseNotes = "Added ultra-sonic sensor obstacle avoidance logic & improved motor driver calibration for smoother cornering.",
                    fileSizeMb = 1.85f,
                    releaseDate = now - 86400000L,
                    isDownloaded = false
                )
            )
            firmwareDao.insertFirmware(
                FirmwareRelease(
                    id = "FW_ESP32_CLIM_210",
                    version = "2.1.0",
                    deviceType = "CLIMATE_NODE",
                    releaseNotes = "Optimized deep-sleep power budget. Increases battery longevity up to 240 days under MQTT logging.",
                    fileSizeMb = 2.41f,
                    releaseDate = now - 259200000L,
                    isDownloaded = true
                )
            )
        }
    }
}
