package com.example.hardware

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.IoTRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HardwareManagerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: IoTRepository
    private lateinit var hardwareManager: HardwareManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getDatabase(context)
        repository = IoTRepository(
            db.deviceDao(),
            db.sensorDao(),
            db.automationDao(),
            db.firmwareDao()
        )
        hardwareManager = HardwareManager(context, repository)
    }

    @Test
    fun testMqttDefaultState() {
        // Initially disconnected before active connect call
        assertFalse(hardwareManager.isMqttConnected.value)
        
        // Mobile gateway availability defaults to true on standard Robolectric configuration
        assertTrue(hardwareManager.isInternetAvailable.value)
    }

    @Test
    fun testBleDiscoveryInitiallyEmpty() {
        // Initially, no BLE hardware peripherals discovered
        assertTrue(hardwareManager.discoveredBleDevices.value.isEmpty())
        
        // Confirm GATT connections map is empty
        assertTrue(hardwareManager.activeGattConnections.value.isEmpty())
    }

    @Test
    fun testOtaStreamProgressDefaultState() {
        // No OTA progress active initially
        assertNull(hardwareManager.otaProgress.value)
    }
}
