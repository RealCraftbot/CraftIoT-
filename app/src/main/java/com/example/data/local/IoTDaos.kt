package com.example.data.local

import androidx.room.*
import com.example.data.model.IoTDevice
import com.example.data.model.SensorLog
import com.example.data.model.AutomationRule
import com.example.data.model.FirmwareRelease
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<IoTDevice>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDeviceById(id: String): IoTDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: IoTDevice)

    @Delete
    suspend fun deleteDevice(device: IoTDevice)

    @Query("UPDATE devices SET status = :status WHERE id = :id")
    suspend fun updateDeviceStatus(id: String, status: String)

    @Query("UPDATE devices SET sensorValue1 = :val1, sensorValue2 = :val2, lastActive = :timestamp WHERE id = :id")
    suspend fun updateDeviceTelemetry(id: String, val1: Float, val2: Float, timestamp: Long)

    @Query("UPDATE devices SET stateFlag1 = :flag WHERE id = :id")
    suspend fun updateDeviceControlState(id: String, flag: Boolean)
}

@Dao
interface SensorLogDao {
    @Query("SELECT * FROM sensor_logs WHERE deviceId = :deviceId AND metric = :metric ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsForDevice(deviceId: String, metric: String, limit: Int = 50): Flow<List<SensorLog>>

    @Query("SELECT * FROM sensor_logs ORDER BY timestamp DESC LIMIT 200")
    fun getLatestLogs(): Flow<List<SensorLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SensorLog)

    @Query("DELETE FROM sensor_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeLogs(cutoffTimestamp: Long)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automation_rules ORDER BY name ASC")
    fun getAllRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules WHERE isActive = 1")
    fun getActiveRules(): Flow<List<AutomationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: AutomationRule)

    @Delete
    suspend fun deleteRule(rule: AutomationRule)
}

@Dao
interface FirmwareDao {
    @Query("SELECT * FROM firmwares ORDER BY version DESC")
    fun getAllFirmwares(): Flow<List<FirmwareRelease>>

    @Query("SELECT * FROM firmwares WHERE deviceType = :deviceType ORDER BY version DESC")
    fun getFirmwaresForDeviceType(deviceType: String): Flow<List<FirmwareRelease>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirmware(firmware: FirmwareRelease)

    @Query("UPDATE firmwares SET isDownloaded = :isDownloaded WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean)
}
