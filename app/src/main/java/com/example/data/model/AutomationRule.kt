package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val deviceId: String, // Which device to monitor
    val metric: String, // "temp", "speed", "humidity", "battery"
    val operator: String, // "GREATER_THAN", "LESS_THAN"
    val thresholdValue: Float,
    val actionDeviceId: String, // Which device to control
    val actionType: String, // "TURN_ON", "TURN_OFF", "ALERT", "SET_SPEED"
    val actionValue: Float = 1.0f,
    val isActive: Boolean = true
)
