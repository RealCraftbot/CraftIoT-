package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.IoTDevice
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.ProvisioningStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeviceManagementTest {

    private lateinit var application: Application
    private lateinit var viewModel: DashboardViewModel
    private lateinit var db: AppDatabase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        
        // Retrieve the exact same database singleton instance used by the application ViewModel
        db = AppDatabase.getDatabase(application)

        // Clear existing devices and prepopulate synchronously to ensure complete isolation
        runBlocking {
            val deviceDao = db.deviceDao()
            try {
                val existing = deviceDao.getAllDevices().first()
                existing.forEach { deviceDao.deleteDevice(it) }
            } catch (e: Exception) {
                // Fail-safe
            }

            val repository = com.example.data.repository.IoTRepository(
                db.deviceDao(),
                db.sensorDao(),
                db.automationDao(),
                db.firmwareDao()
            )
            repository.prepopulateDatabase()
        }
        
        viewModel = DashboardViewModel(application)
        
        // Pause the hardware simulator background loop to prevent telemetry write races during unit testing
        viewModel.toggleSimulation()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialDeviceStateIsLoaded() = runTest(testDispatcher) {
        var attempts = 0
        while (viewModel.devices.value.isEmpty() && attempts < 50) {
            delay(100)
            attempts++
        }
        
        val currentDevices = viewModel.devices.value
        assertNotNull(currentDevices)
        assertTrue(currentDevices.isNotEmpty())
    }

    @Test
    fun testRenameDevice() = runTest(testDispatcher) {
        var attempts = 0
        while (viewModel.devices.value.isEmpty() && attempts < 50) {
            delay(100)
            attempts++
        }
        
        val currentDevices = viewModel.devices.value
        val targetDevice = currentDevices.first()
        val originalName = targetDevice.name
        val newName = "Surgically Renamed Rover"
        
        viewModel.renameDevice(targetDevice.id, newName)
        advanceUntilIdle() // Synchronously execute the queued rename coroutine block
        
        attempts = 0
        while (viewModel.devices.value.find { it.id == targetDevice.id }?.name != newName && attempts < 50) {
            delay(100)
            attempts++
        }
        
        val updatedDevices = viewModel.devices.value
        val updatedDevice = updatedDevices.find { it.id == targetDevice.id }
        assertNotNull(updatedDevice)
        assertEquals(newName, updatedDevice?.name)
    }

    @Test
    fun testUpdateDeviceGroup() = runTest(testDispatcher) {
        var attempts = 0
        while (viewModel.devices.value.isEmpty() && attempts < 50) {
            delay(100)
            attempts++
        }
        
        val currentDevices = viewModel.devices.value
        val targetDevice = currentDevices.first()
        val newGroup = "Experimental Lab Suite"
        
        viewModel.updateDeviceGroup(targetDevice.id, newGroup)
        advanceUntilIdle() // Synchronously execute the queued group update coroutine block
        
        attempts = 0
        while (viewModel.devices.value.find { it.id == targetDevice.id }?.deviceGroup != newGroup && attempts < 50) {
            delay(100)
            attempts++
        }
        
        val updatedDevices = viewModel.devices.value
        val updatedDevice = updatedDevices.find { it.id == targetDevice.id }
        assertNotNull(updatedDevice)
        assertEquals(newGroup, updatedDevice?.deviceGroup)
    }

    @Test
    fun testToggleDeviceOnlineStatus() = runTest(testDispatcher) {
        var attempts = 0
        while (viewModel.devices.value.isEmpty() && attempts < 50) {
            delay(100)
            attempts++
        }
        
        val currentDevices = viewModel.devices.value
        val targetDevice = currentDevices.first()
        val originalOfflineState = targetDevice.isOffline
        
        viewModel.toggleDeviceOnlineStatus(targetDevice.id)
        advanceUntilIdle() // Synchronously execute the queued status toggle coroutine block
        
        attempts = 0
        while (viewModel.devices.value.find { it.id == targetDevice.id }?.isOffline == originalOfflineState && attempts < 50) {
            delay(100)
            attempts++
        }
        
        val updatedDevices = viewModel.devices.value
        val updatedDevice = updatedDevices.find { it.id == targetDevice.id }
        assertNotNull(updatedDevice)
        assertNotEquals(originalOfflineState, updatedDevice?.isOffline)
    }

    @Test
    fun testPairDeviceViaQrCode() = runTest(testDispatcher) {
        val qrPayload = "id:ESP32_QR_TEST,name:QR Greenhouse Node,type:CLIMATE_NODE,group:Greenhouse"
        viewModel.pairDeviceViaQrCode(qrPayload)
        
        // Execute the first part of the coroutine up to the first delay
        runCurrent()
        assertEquals(ProvisioningStep.BleScanning, viewModel.provisioningStep.value)
        
        // Advance virtual clock past the BLE scan delay of 1200ms
        advanceTimeBy(1300)
        runCurrent()
        assertTrue(viewModel.provisioningStep.value is ProvisioningStep.BleConnected)
        
        // Advance virtual clock past the BLE connected state delay of 1000ms
        advanceTimeBy(1100)
        runCurrent()
        assertTrue(viewModel.provisioningStep.value is ProvisioningStep.WifiCredentialsInput)
        
        val inputState = viewModel.provisioningStep.value as ProvisioningStep.WifiCredentialsInput
        assertEquals("QR Greenhouse Node", inputState.name)
        assertEquals("CLIMATE_NODE", inputState.type)
    }

    @Test
    fun testNetworkSubnetDiscovery() = runTest(testDispatcher) {
        assertFalse(viewModel.isDiscovering.value)
        assertEquals(0, viewModel.discoveredUnprovisionedDevices.value.size)
        
        viewModel.discoverDevices()
        
        // Verify active discovery phase before the delay ends
        runCurrent()
        assertTrue(viewModel.isDiscovering.value)
        
        // Advance virtual clock past the network subnet discovery delay of 2000ms
        advanceTimeBy(2100)
        runCurrent()
        
        assertFalse(viewModel.isDiscovering.value)
        assertEquals(2, viewModel.discoveredUnprovisionedDevices.value.size)
    }
}
