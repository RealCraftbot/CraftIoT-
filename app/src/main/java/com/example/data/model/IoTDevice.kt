package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class IoTDevice(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "ROBOT_CAR", "CLIMATE_NODE", "SECURITY_CAM", "SMART_AGRI"
    val status: String, // "ONLINE", "OFFLINE", "PROVISIONING"
    val ipAddress: String = "192.168.1.100",
    val macAddress: String = "00:00:00:00:00:00",
    val connectionType: String = "WI_FI", // "WI_FI", "BLE", "MQTT"
    val lastActive: Long = System.currentTimeMillis(),
    val sensorValue1: Float = 0.0f, // Temperature, Speed, or Battery
    val sensorValue2: Float = 0.0f, // Humidity, Distance, or Signal
    val stateFlag1: Boolean = false, // Motor, Relay, or Light Switch
    val customConfig: String = "" // Additional JSON or metadata
) {
    val typeLabel: String
        get() = when (type) {
            "ROBOT_CAR" -> "Robot Car Controller"
            "CLIMATE_NODE" -> "Smart Climate Station"
            "SECURITY_CAM" -> "HD Camera Stream"
            "SMART_AGRI" -> "Soil Hydroponics System"
            else -> "IoT Edge Device"
        }
}
