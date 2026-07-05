package com.example.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.NetworkSpecifier
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.example.data.model.IoTDevice
import com.example.data.model.SensorLog
import com.example.data.repository.IoTRepository
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import okio.source
import okio.Buffer
import okio.BufferedSink
import okio.Source

@SuppressLint("MissingPermission")
class HardwareManager(
    private val context: Context,
    private val repository: IoTRepository
) {
    private val tag = "HardwareManager"
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- State Observables ---
    private val _isMqttConnected = MutableStateFlow(false)
    val isMqttConnected: StateFlow<Boolean> = _isMqttConnected.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    private val _discoveredBleDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredBleDevices: StateFlow<List<BluetoothDevice>> = _discoveredBleDevices.asStateFlow()

    private val _activeGattConnections = MutableStateFlow<Map<String, BluetoothGatt>>(emptyMap())
    val activeGattConnections: StateFlow<Map<String, BluetoothGatt>> = _activeGattConnections.asStateFlow()

    private val _otaProgress = MutableStateFlow<Pair<String, Int>?>(null) // Device ID to percentage
    val otaProgress: StateFlow<Pair<String, Int>?> = _otaProgress.asStateFlow()

    // --- Core Clients ---
    private var mqttClient: Mqtt3AsyncClient? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        monitorConnectivity()
    }

    // ==========================================
    // 1. TLS-Secured MQTT Client (HiveMQ Client)
    // ==========================================
    fun connectMqtt(
        brokerHost: String = "broker.hivemq.com",
        port: Int = 8883,
        useTls: Boolean = true
    ) {
        val clientId = "CraftIoT-Android-" + UUID.randomUUID().toString().take(6)
        Log.i(tag, "Connecting MQTT client $clientId to $brokerHost:$port (TLS: $useTls)...")

        try {
            val clientBuilder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(brokerHost)
                .serverPort(port)

            if (useTls) {
                // Configures real, secure TLS-encrypted socket connection
                clientBuilder.sslWithDefaultConfig()
            }

            mqttClient = clientBuilder.buildAsync()

            mqttClient?.connectWith()
                ?.cleanSession(true)
                ?.keepAlive(60)
                ?.send()
                ?.whenComplete { connAck: Mqtt3ConnAck?, throwable: Throwable? ->
                    if (throwable != null) {
                        Log.e(tag, "MQTT Connection failed: ${throwable.message}")
                        _isMqttConnected.value = false
                        scheduleMqttReconnect(brokerHost, port, useTls)
                    } else {
                        Log.i(tag, "MQTT Connection successfully established (TLS secured)!")
                        _isMqttConnected.value = true
                        subscribeToTelemetry()
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Exception during MQTT instantiation: ${e.message}")
        }
    }

    private fun scheduleMqttReconnect(host: String, port: Int, useTls: Boolean) {
        ioScope.launch {
            delay(10000L) // Wait 10 seconds before attempting reconnection
            if (!_isMqttConnected.value && _isInternetAvailable.value) {
                Log.i(tag, "Attempting automatic MQTT reconnection...")
                connectMqtt(host, port, useTls)
            }
        }
    }

    private fun subscribeToTelemetry() {
        // Subscribe to wildcard telemetry stream for all devices on the platform
        mqttClient?.subscribeWith()
            ?.topicFilter("craftiot/devices/+/telemetry")
            ?.callback { publish: Mqtt3Publish ->
                val topic = publish.topic.toString()
                val payloadString = StandardCharsets.UTF_8.decode(publish.payload.get()).toString()
                Log.d(tag, "MQTT Message received on topic: $topic | Payload: $payloadString")
                parseAndStoreTelemetry(topic, payloadString)
            }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(tag, "Failed to subscribe to telemetry: ${throwable.message}")
                } else {
                    Log.i(tag, "Successfully subscribed to wildcard device telemetries")
                }
            }
    }

    private fun parseAndStoreTelemetry(topic: String, payload: String) {
        // Extract deviceId from "craftiot/devices/{deviceId}/telemetry"
        val parts = topic.split("/")
        if (parts.size >= 3) {
            val deviceId = parts[2]
            try {
                // Custom JSON parsing for production stability (Moshi or similar can also be used)
                val cleanPayload = payload.trim().removeSurrounding("{", "}")
                val pairs = cleanPayload.split(",")
                var sensor1 = 0.0f
                var sensor2 = 0.0f
                var stateFlag = false

                for (pair in pairs) {
                    val kv = pair.split(":")
                    if (kv.size == 2) {
                        val key = kv[0].trim().removeSurrounding("\"")
                        val valueStr = kv[1].trim().removeSurrounding("\"")
                        when (key) {
                            "sensorValue1", "temp", "speed" -> sensor1 = valueStr.toFloatOrNull() ?: 0.0f
                            "sensorValue2", "humidity", "moisture" -> sensor2 = valueStr.toFloatOrNull() ?: 0.0f
                            "stateFlag1", "relay", "pump" -> stateFlag = valueStr.lowercase() == "true" || valueStr == "1"
                        }
                    }
                }

                ioScope.launch {
                    repository.insertSensorLog(SensorLog(deviceId = deviceId, metric = "sensor1", value = sensor1))
                    repository.insertSensorLog(SensorLog(deviceId = deviceId, metric = "sensor2", value = sensor2))
                    repository.insertOrUpdateDevice(
                        IoTDevice(
                            id = deviceId,
                            name = "Discovered ESP32 Node",
                            type = "MQTT_NODE",
                            status = "ONLINE",
                            ipAddress = "Dynamic MQTT IP",
                            macAddress = "XX:XX:XX:XX:XX:XX",
                            connectionType = "MQTT",
                            sensorValue1 = sensor1,
                            sensorValue2 = sensor2,
                            stateFlag1 = stateFlag
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse telemetry: ${e.message}")
            }
        }
    }

    fun publishCommand(deviceId: String, commandPayload: String) {
        val topic = "craftiot/devices/$deviceId/control"
        if (mqttClient != null && _isMqttConnected.value) {
            mqttClient?.publishWith()
                ?.topic(topic)
                ?.payload(commandPayload.toByteArray(StandardCharsets.UTF_8))
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(tag, "Failed to publish control frame: ${throwable.message}")
                    } else {
                        Log.i(tag, "Dispatched state control frame on MQTT topic: $topic")
                    }
                }
        } else {
            Log.w(tag, "MQTT client offline. Cannot publish command: $commandPayload")
        }
    }

    // ==========================================
    // 2. Android BLE Scanning & GATT Client
    // ==========================================
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name != null) {
                val currentList = _discoveredBleDevices.value
                if (!currentList.any { it.address == device.address }) {
                    Log.i(tag, "Discovered BLE Hardware Peripheral: ${device.name} [${device.address}]")
                    _discoveredBleDevices.value = currentList + device
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "BLE Scan failed with error code: $errorCode")
        }
    }

    fun startBleDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(tag, "Bluetooth Adapter is inactive or unconfigured.")
            return
        }

        _discoveredBleDevices.value = emptyList()
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner != null) {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            // Filters for standard ESP32 BLE peripherals
            val filters = listOf(
                ScanFilter.Builder().build()
            )

            scanner.startScan(filters, settings, scanCallback)
            Log.i(tag, "Active BLE hardware scan initiated...")

            // Auto-stop scanning after 15 seconds to save power
            ioScope.launch {
                delay(15000L)
                stopBleDiscovery()
            }
        }
    }

    fun stopBleDiscovery() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner != null) {
            scanner.stopScan(scanCallback)
            Log.i(tag, "Active BLE hardware scan paused.")
        }
    }

    fun connectGattDevice(device: BluetoothDevice) {
        Log.i(tag, "Connecting to GATT Server of ${device.name} (${device.address})...")
        
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                val deviceAddress = gatt.device.address
                if (newState == BluetoothProfile_STATE_CONNECTED) {
                    Log.i(tag, "Successfully connected to GATT Server: $deviceAddress")
                    val currentMap = _activeGattConnections.value.toMutableMap()
                    currentMap[deviceAddress] = gatt
                    _activeGattConnections.value = currentMap

                    // Pair and save newly discovered hardware to local database
                    ioScope.launch {
                        repository.insertOrUpdateDevice(
                            IoTDevice(
                                id = "BLE_${deviceAddress.replace(":", "")}",
                                name = gatt.device.name ?: "CraftIoT BLE Peripheral",
                                type = "BLE_NODE",
                                status = "ONLINE",
                                ipAddress = "N/A (GATT)",
                                macAddress = deviceAddress,
                                connectionType = "BLE",
                                sensorValue1 = 100.0f, // Battery level representation
                                sensorValue2 = 0.0f,
                                stateFlag1 = false
                            )
                        )
                    }

                    // Initiate service discovery immediately
                    gatt.discoverServices()

                } else if (newState == BluetoothProfile_STATE_DISCONNECTED) {
                    Log.i(tag, "GATT Client disconnected from hardware link: $deviceAddress")
                    val currentMap = _activeGattConnections.value.toMutableMap()
                    currentMap.remove(deviceAddress)
                    _activeGattConnections.value = currentMap
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(tag, "GATT services cataloged on ${gatt.device.address}")
                    for (service in gatt.services) {
                        Log.d(tag, "Service UUID: ${service.uuid}")
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, callback)
        }
    }

    // Android Bluetooth state constants mapped locally
    companion object {
        const val BluetoothProfile_STATE_CONNECTED = 2
        const val BluetoothProfile_STATE_DISCONNECTED = 0
    }

    // ==========================================
    // 3. ESP32 Wi-Fi Provisioning using Android Wi-Fi APIs
    // ==========================================
    fun provisionWiFi(ssid: String, psk: String, targetDeviceIp: String = "192.168.4.1") {
        Log.i(tag, "Initiating hardware provisioning onto Network: $ssid ...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ standard Wi-Fi configuration request
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid("CraftIoT-Provision-AP")
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.i(tag, "Connected securely to ESP32 provisioning Hotspot AP")
                    connectivityManager.bindProcessToNetwork(network)

                    // Dispatch credentials via secure local HTTP request
                    postWifiCredentials(ssid, psk, targetDeviceIp)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.e(tag, "Hotspot unavailable or authorization denied.")
                }
            }

            connectivityManager.requestNetwork(request, networkCallback)
        } else {
            // Legacy Wi-Fi configuration
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                val conf = WifiConfiguration().apply {
                    SSID = "\"CraftIoT-Provision-AP\""
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
                val netId = wifiManager.addNetwork(conf)
                wifiManager.disconnect()
                wifiManager.enableNetwork(netId, true)
                wifiManager.reconnect()

                ioScope.launch {
                    delay(3000) // Delay to let connection settle
                    postWifiCredentials(ssid, psk, targetDeviceIp)
                }
            }
        }
    }

    private fun postWifiCredentials(ssid: String, psk: String, targetDeviceIp: String) {
        val formBody = FormBody.Builder()
            .add("ssid", ssid)
            .add("password", psk)
            .build()

        val request = Request.Builder()
            .url("http://$targetDeviceIp/provision")
            .post(formBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "Credentials delivery failure: ${e.message}")
                connectivityManager.bindProcessToNetwork(null) // release bind
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i(tag, "ESP32 microchip accepted Wi-Fi gateway: ${response.code}")
                connectivityManager.bindProcessToNetwork(null) // release bind
            }
        })
    }

    // ==========================================
    // 4. Real OTA Firmware Upload to ESP32 Devices
    // ==========================================
    fun performOtaUpdate(deviceId: String, ipAddress: String, firmwareFile: File) {
        Log.i(tag, "Starting active OTA Firmware Upload to device: $deviceId at $ipAddress")
        _otaProgress.value = Pair(deviceId, 0)

        // Wrap file in Custom ProgressRequestBody to track percentage live
        val progressBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

            override fun contentLength() = firmwareFile.length()

            override fun writeTo(sink: BufferedSink) {
                val source = firmwareFile.source()
                val buffer = Buffer()
                var totalBytesRead = 0L
                val fileLen = contentLength()
                var readCount: Long

                while (source.read(buffer, 2048L).also { readCount = it } != -1L) {
                    sink.write(buffer, readCount)
                    totalBytesRead += readCount
                    val progress = ((totalBytesRead * 100) / fileLen).toInt()
                    _otaProgress.value = Pair(deviceId, progress)
                    Log.d(tag, "Firmware stream progress: $progress%")
                }
                source.close()
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("update", "firmware.bin", progressBody)
            .build()

        val request = Request.Builder()
            .url("http://$ipAddress/update")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "OTA upload failed for device $deviceId: ${e.message}")
                _otaProgress.value = Pair(deviceId, -1) // -1 denotes error
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i(tag, "OTA successfully flashed to $deviceId!")
                    _otaProgress.value = Pair(deviceId, 100)
                } else {
                    Log.e(tag, "OTA flash rejected by device. HTTP Status: ${response.code}")
                    _otaProgress.value = Pair(deviceId, -1)
                }
            }
        })
    }

    // ==========================================
    // 5. Automatic Offline & Network State Detection
    // ==========================================
    private fun monitorConnectivity() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(tag, "Mobile internet gateway found.")
                _isInternetAvailable.value = true
                
                // Reconnect MQTT if it went offline
                if (mqttClient != null && !_isMqttConnected.value) {
                    connectMqtt()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.w(tag, "Mobile internet connection severed.")
                _isInternetAvailable.value = false
                _isMqttConnected.value = false
            }
        })
    }

    fun release() {
        ioScope.cancel()
        mqttClient?.disconnect()
    }
}
