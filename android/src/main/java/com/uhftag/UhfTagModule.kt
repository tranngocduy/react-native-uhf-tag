package com.uhftag

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback
import com.rscja.deviceapi.interfaces.KeyEventCallback
import com.rscja.deviceapi.interfaces.ScanBTCallback
import com.uhftag.utils.CheckUtils
import com.uhftag.utils.SoundUtils
import java.util.concurrent.ConcurrentHashMap

@ReactModule(name = UhfTagModule.NAME)
class UhfTagModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "UhfTag"
        private const val TAG = "UhfTagModule"
        private const val ACCESS_FINE_LOCATION_PERMISSION_REQUEST = 100
        
        // Event names
        const val EVENT_BLE_DEVICE_FOUND = "onBLEDeviceFound"
        const val EVENT_RFID_TAG_READ = "onRfidTagRead"
        const val EVENT_CONNECTION_STATUS = "onConnectionStatusChanged"
    }

    private val uhfReader: RFIDWithUHFBLE = RFIDWithUHFBLE.getInstance()
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceList = ConcurrentHashMap<String, BluetoothDeviceInfo>()
    private val tagList = mutableListOf<UHFTAGInfo>()
    
    @Volatile
    private var isScanning = false
    
    @Volatile
    private var isExiting = false
    
    private var searchRfidTag: String? = null
    private var connectedDeviceName: String = ""
    private var isSupportRssi: Boolean = false
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scanThread: Thread? = null

    init {
        initializeModule()
    }

    private fun initializeModule() {
        try {
            uhfReader.init(reactContext)
            SoundUtils.initSound(reactContext)
            isExiting = false
            
            // Setup key event callback for hardware trigger button
            uhfReader.setKeyEventCallback(object : KeyEventCallback {
                override fun onKeyDown(keycode: Int) {
                    if (!isExiting && uhfReader.connectStatus == ConnectionStatus.CONNECTED) {
                        handleKeyPress(keycode)
                    }
                }

                override fun onKeyUp(keycode: Int) {
                    stopScanRFID()
                }
            })
            
            Log.d(TAG, "UHF Tag module initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing module: ${e.message}", e)
        }
    }

    private fun handleKeyPress(keycode: Int) {
        when (keycode) {
            3 -> startScanRFID()
            1 -> {
                if (isScanning) {
                    stopScanRFID()
                } else {
                    searchRfidTag?.let { rfid ->
                        if (rfid.isNotEmpty()) {
                            startScanRFIDWithTag(rfid)
                        } else {
                            startScanRFID()
                        }
                    } ?: startScanRFID()
                }
            }
            else -> {
                if (isScanning) {
                    stopScanRFID()
                    SystemClock.sleep(100)
                }
                inventorySingleTag()
            }
        }
    }

    override fun getName(): String = NAME

    override fun getConstants(): Map<String, Any> {
        return mapOf(
            "EVENT_BLE_DEVICE_FOUND" to EVENT_BLE_DEVICE_FOUND,
            "EVENT_RFID_TAG_READ" to EVENT_RFID_TAG_READ,
            "EVENT_CONNECTION_STATUS" to EVENT_CONNECTION_STATUS
        )
    }

    override fun invalidate() {
        super.invalidate()
        cleanup()
    }

    private fun cleanup() {
        try {
            isExiting = true
            stopScanRFID()
            uhfReader.disconnect()
            tagList.clear()
            deviceList.clear()
            SoundUtils.releaseSound()
            Log.d(TAG, "Module cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }

    // ==================== Bluetooth Operations ====================

    @ReactMethod
    fun scanBLE() {
        if (!checkLocationPermission()) {
            sendEvent(EVENT_CONNECTION_STATUS, createMap().apply {
                putString("status", "error")
                putString("message", "Location permission required")
            })
            return
        }

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not available")
            sendEvent(EVENT_CONNECTION_STATUS, createMap().apply {
                putString("status", "error")
                putString("message", "Bluetooth not available")
            })
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            sendEvent(EVENT_CONNECTION_STATUS, createMap().apply {
                putString("status", "error")
                putString("message", "Please enable Bluetooth")
            })
            return
        }

        deviceList.clear()
        uhfReader.startScanBTDevices(btScanCallback)
        Log.d(TAG, "Started BLE scanning")
    }

    @ReactMethod
    fun stopScanBLE() {
        uhfReader.stopScanBTDevices()
        Log.d(TAG, "Stopped BLE scanning")
    }

    @ReactMethod
    fun connectAddress(address: String, promise: Promise) {
        try {
            uhfReader.connect(address, connectionStatusCallback)
            
            // Wait for connection result
            mainHandler.postDelayed({
                if (connectedDeviceName.isNotEmpty()) {
                    promise.resolve(connectedDeviceName)
                } else {
                    promise.reject("CONNECTION_FAILED", "Failed to connect to device")
                }
            }, 3000)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}", e)
            promise.reject("CONNECTION_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun disconnect() {
        try {
            uhfReader.disconnect()
            connectedDeviceName = ""
            sendEvent(EVENT_CONNECTION_STATUS, createMap().apply {
                putString("status", "disconnected")
            })
            Log.d(TAG, "Disconnected from device")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
    }

    @ReactMethod
    fun getConnectionStatus(promise: Promise) {
        try {
            val status = when (uhfReader.connectStatus) {
                ConnectionStatus.CONNECTED -> "connected"
                ConnectionStatus.CONNECTING -> "connecting"
                ConnectionStatus.DISCONNECTED -> "disconnected"
                else -> "unknown"
            }
            promise.resolve(status)
        } catch (e: Exception) {
            promise.reject("STATUS_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun isConnected(promise: Promise) {
        try {
            val connected = uhfReader.connectStatus == ConnectionStatus.CONNECTED
            promise.resolve(connected)
        } catch (e: Exception) {
            promise.reject("STATUS_ERROR", e.message, e)
        }
    }

    // ==================== RFID Operations ====================

    @ReactMethod
    fun startScanRFID() {
        if (isScanning) {
            Log.w(TAG, "Already scanning")
            return
        }
        
        searchRfidTag = null
        isScanning = true
        scanThread = Thread(RfidScanRunnable()).apply { start() }
        Log.d(TAG, "Started RFID scanning")
    }

    @ReactMethod
    fun startScanRFIDWithTag(rfidTag: String) {
        if (isScanning) {
            Log.w(TAG, "Already scanning")
            return
        }
        
        searchRfidTag = rfidTag
        isScanning = true
        scanThread = Thread(RfidScanRunnable()).apply { start() }
        Log.d(TAG, "Started RFID scanning for tag: $rfidTag")
    }

    @ReactMethod
    fun stopScanRFID() {
        if (!isScanning) return
        
        isScanning = false
        scanThread?.interrupt()
        scanThread = null
        searchRfidTag = null
        Log.d(TAG, "Stopped RFID scanning")
    }

    @ReactMethod
    fun clearData(promise: Promise) {
        try {
            tagList.clear()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CLEAR_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun setPower(power: Int, promise: Promise) {
        try {
            val success = uhfReader.setPower(power)
            if (success) {
                Log.d(TAG, "Set power to $power successfully")
                promise.resolve(true)
            } else {
                Log.e(TAG, "Failed to set power")
                promise.resolve(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting power: ${e.message}", e)
            promise.reject("POWER_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getPower(promise: Promise) {
        try {
            val power = uhfReader.power
            promise.resolve(power)
        } catch (e: Exception) {
            promise.reject("POWER_ERROR", e.message, e)
        }
    }

    // ==================== Private Helper Methods ====================

    private fun checkLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activity = reactContext.currentActivity ?: return false
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    ACCESS_FINE_LOCATION_PERMISSION_REQUEST
                )
                return false
            }
        }
        return true
    }

    private fun inventorySingleTag() {
        val tagInfo = uhfReader.inventorySingleTag()
        tagInfo?.let { processTag(listOf(it)) }
    }

    private fun readTags(): List<UHFTAGInfo>? {
        return if (isSupportRssi) {
            uhfReader.readTagFromBufferList_EpcTidUser()
        } else {
            uhfReader.readTagFromBufferList()
        }
    }

    private fun processTag(tags: List<UHFTAGInfo>) {
        for (tag in tags) {
            val exists = booleanArrayOf(false)
            val index = CheckUtils.getInsertIndex(tagList, tag, exists)
            
            if (!exists[0]) {
                tagList.add(index, tag)
                
                mainHandler.post {
                    sendEvent(EVENT_RFID_TAG_READ, createMap().apply {
                        putString("epc", tag.epc)
                        putString("rssi", tag.rssi)
                        putDouble("timestamp", System.currentTimeMillis().toDouble())
                    })
                }
                
                SoundUtils.playSound(1)
            }
        }
    }

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun createMap(): WritableMap = Arguments.createMap()

    // ==================== Callbacks ====================

    private val btScanCallback = ScanBTCallback { device, rssi, _ ->
        try {
            val deviceInfo = BluetoothDeviceInfo(device.address, device.name ?: "Unknown")
            
            if (!deviceList.containsKey(device.address)) {
                deviceList[device.address] = deviceInfo
                
                mainHandler.post {
                    sendEvent(EVENT_BLE_DEVICE_FOUND, createMap().apply {
                        putString("name", deviceInfo.name)
                        putString("address", deviceInfo.address)
                        putString("rssi", rssi.toString())
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in BT scan callback: ${e.message}", e)
        }
    }

    private val connectionStatusCallback = ConnectionStatusCallback<Any> { status, device ->
        mainHandler.post {
            if (device is BluetoothDevice) {
                when (status) {
                    ConnectionStatus.CONNECTED -> {
                        connectedDeviceName = "${device.name}(${device.address})"
                        sendEvent(EVENT_CONNECTION_STATUS, createMap().apply {
                            putString("status", "connected")
                            putString("deviceName", device.name)
                            putString("deviceAddress", device.address)
                        })
                        Log.d(TAG, "Connected to: $connectedDeviceName")
                    }
                    ConnectionStatus.DISCONNECTED -> {
                        connectedDeviceName = ""
                        sendEvent(EVENT_CONNECTION_STATUS, createMap().apply {
                            putString("status", "disconnected")
                        })
                        Log.d(TAG, "Disconnected from device")
                    }
                    ConnectionStatus.CONNECTING -> {
                        sendEvent(EVENT_CONNECTION_STATUS, createMap().apply {
                            putString("status", "connecting")
                        })
                        Log.d(TAG, "Connecting to device...")
                    }
                    else -> {}
                }
            }
        }
    }

    // ==================== Inner Classes ====================

    private inner class RfidScanRunnable : Runnable {
        override fun run() {
            try {
                if (!uhfReader.startInventoryTag()) {
                    Log.e(TAG, "Failed to start inventory")
                    isScanning = false
                    return
                }

                while (isScanning && !Thread.currentThread().isInterrupted) {
                    val tags = readTags()
                    
                    if (tags.isNullOrEmpty()) {
                        SystemClock.sleep(1)
                        continue
                    }

                    // Filter by search tag if specified
                    val filteredTags = searchRfidTag?.let { searchTag ->
                        tags.filter { it.epc == searchTag }
                    } ?: tags

                    if (filteredTags.isNotEmpty()) {
                        processTag(filteredTags)
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Scan thread interrupted")
            } catch (e: Exception) {
                Log.e(TAG, "Error during RFID scan: ${e.message}", e)
            } finally {
                stopInventory()
            }
        }

        private fun stopInventory() {
            try {
                val result = uhfReader.stopInventory()
                if (!result) {
                    Log.w(TAG, "Failed to stop inventory")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping inventory: ${e.message}", e)
            }
        }
    }

    private data class BluetoothDeviceInfo(
        val address: String,
        val name: String
    )

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for events
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for events
    }
}
