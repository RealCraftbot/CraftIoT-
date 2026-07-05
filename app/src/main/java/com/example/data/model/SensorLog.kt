package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_logs")
data class SensorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val deviceId: String,
    val metric: String, // "temp", "humidity", "speed", "battery"
    val value: Float,
    val timestamp: Long = System.currentTimeMillis()
)
