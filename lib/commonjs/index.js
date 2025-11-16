"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Events = void 0;
exports.addBLEDeviceListener = addBLEDeviceListener;
exports.addConnectionStatusListener = addConnectionStatusListener;
exports.addRfidTagListener = addRfidTagListener;
exports.clearTags = clearTags;
exports.connectDevice = connectDevice;
exports.default = void 0;
exports.disconnect = disconnect;
exports.getConnectionStatus = getConnectionStatus;
exports.getPower = getPower;
exports.isConnected = isConnected;
exports.removeAllListeners = removeAllListeners;
exports.scanBLE = scanBLE;
exports.setPower = setPower;
exports.startScan = startScan;
exports.startScanWithTag = startScanWithTag;
exports.stopScan = stopScan;
exports.stopScanBLE = stopScanBLE;
var _reactNative = require("react-native");
const LINKING_ERROR = `The package 'react-native-uhf-tag' doesn't seem to be linked. Make sure: \n\n` + _reactNative.Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';

// Use NativeModules directly
const UhfTagModule = _reactNative.NativeModules.UhfTag ? _reactNative.NativeModules.UhfTag : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});

// Event emitter for native events
const eventEmitter = new _reactNative.NativeEventEmitter(_reactNative.NativeModules.UhfTag);

// ==================== Types ====================

// ==================== Event Names ====================

const Events = exports.Events = {
  BLE_DEVICE_FOUND: 'onBLEDeviceFound',
  RFID_TAG_READ: 'onRfidTagRead',
  CONNECTION_STATUS: 'onConnectionStatusChanged'
};

// ==================== Bluetooth Operations ====================

/**
 * Start scanning for nearby Bluetooth devices
 * Listen to Events.BLE_DEVICE_FOUND to get discovered devices
 */
function scanBLE() {
  UhfTagModule.scanBLE();
}

/**
 * Stop scanning for Bluetooth devices
 */
function stopScanBLE() {
  UhfTagModule.stopScanBLE();
}

/**
 * Connect to a Bluetooth device by its address
 * @param address - The Bluetooth device MAC address
 * @returns Promise resolving to connected device info
 */
function connectDevice(address) {
  return UhfTagModule.connectAddress(address);
}

/**
 * Disconnect from the currently connected device
 */
function disconnect() {
  UhfTagModule.disconnect();
}

/**
 * Get current connection status
 * @returns Promise with connection status string
 */
function getConnectionStatus() {
  return UhfTagModule.getConnectionStatus();
}

/**
 * Check if device is currently connected
 * @returns Promise with boolean connection state
 */
function isConnected() {
  return UhfTagModule.isConnected();
}

// ==================== RFID Operations ====================

/**
 * Start scanning for RFID tags
 * Listen to Events.RFID_TAG_READ to receive tag data
 */
function startScan() {
  UhfTagModule.startScanRFID();
}

/**
 * Start scanning for a specific RFID tag
 * Only tags matching the provided EPC will trigger events
 * @param rfidTag - The EPC tag to search for
 */
function startScanWithTag(rfidTag) {
  UhfTagModule.startScanRFIDWithTag(rfidTag);
}

/**
 * Stop RFID tag scanning
 */
function stopScan() {
  UhfTagModule.stopScanRFID();
}

/**
 * Clear all stored tag data
 * @returns Promise resolving to true if successful
 */
function clearTags() {
  return UhfTagModule.clearData();
}

// ==================== Power Management ====================

/**
 * Set the RFID reader power level
 * @param power - Power level (typically 5-30)
 * @returns Promise resolving to true if successful
 */
function setPower(power) {
  return UhfTagModule.setPower(power);
}

/**
 * Get the current RFID reader power level
 * @returns Promise with current power level
 */
function getPower() {
  return UhfTagModule.getPower();
}

// ==================== Event Listeners ====================

/**
 * Add listener for BLE device discovery
 * @param listener - Callback function receiving discovered devices
 * @returns Subscription object for cleanup
 */
function addBLEDeviceListener(listener) {
  return eventEmitter.addListener(Events.BLE_DEVICE_FOUND, listener);
}

/**
 * Add listener for RFID tag reads
 * @param listener - Callback function receiving tag data
 * @returns Subscription object for cleanup
 */
function addRfidTagListener(listener) {
  return eventEmitter.addListener(Events.RFID_TAG_READ, listener);
}

/**
 * Add listener for connection status changes
 * @param listener - Callback function receiving status updates
 * @returns Subscription object for cleanup
 */
function addConnectionStatusListener(listener) {
  return eventEmitter.addListener(Events.CONNECTION_STATUS, listener);
}

/**
 * Remove all event listeners
 */
function removeAllListeners() {
  eventEmitter.removeAllListeners(Events.BLE_DEVICE_FOUND);
  eventEmitter.removeAllListeners(Events.RFID_TAG_READ);
  eventEmitter.removeAllListeners(Events.CONNECTION_STATUS);
}

// ==================== Default Export ====================
var _default = exports.default = {
  // Bluetooth
  scanBLE,
  stopScanBLE,
  connectDevice,
  disconnect,
  getConnectionStatus,
  isConnected,
  // RFID
  startScan,
  startScanWithTag,
  stopScan,
  clearTags,
  // Power
  setPower,
  getPower,
  // Events
  addBLEDeviceListener,
  addRfidTagListener,
  addConnectionStatusListener,
  removeAllListeners,
  Events
};
//# sourceMappingURL=index.js.map