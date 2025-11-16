import { NativeModules, NativeEventEmitter, Platform } from 'react-native';
import type { EmitterSubscription } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-uhf-tag' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// Use NativeModules directly
const UhfTagModule = NativeModules.UhfTag
  ? NativeModules.UhfTag
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

// Event emitter for native events
const eventEmitter = new NativeEventEmitter(NativeModules.UhfTag);

// ==================== Types ====================

export interface BluetoothDevice {
  name: string;
  address: string;
  rssi: string;
}

export interface RfidTag {
  epc: string;
  rssi: string;
  timestamp: number;
}

export interface ConnectionStatus {
  status: 'connected' | 'connecting' | 'disconnected' | 'error';
  deviceName?: string;
  deviceAddress?: string;
  message?: string;
}

export type BLEDeviceListener = (device: BluetoothDevice) => void;
export type RfidTagListener = (tag: RfidTag) => void;
export type ConnectionStatusListener = (status: ConnectionStatus) => void;

// ==================== Event Names ====================

export const Events = {
  BLE_DEVICE_FOUND: 'onBLEDeviceFound',
  RFID_TAG_READ: 'onRfidTagRead',
  CONNECTION_STATUS: 'onConnectionStatusChanged',
} as const;

// ==================== Bluetooth Operations ====================

/**
 * Start scanning for nearby Bluetooth devices
 * Listen to Events.BLE_DEVICE_FOUND to get discovered devices
 */
export function scanBLE(): void {
  UhfTagModule.scanBLE();
}

/**
 * Stop scanning for Bluetooth devices
 */
export function stopScanBLE(): void {
  UhfTagModule.stopScanBLE();
}

/**
 * Connect to a Bluetooth device by its address
 * @param address - The Bluetooth device MAC address
 * @returns Promise resolving to connected device info
 */
export function connectDevice(address: string): Promise<string> {
  return UhfTagModule.connectAddress(address);
}

/**
 * Disconnect from the currently connected device
 */
export function disconnect(): void {
  UhfTagModule.disconnect();
}

/**
 * Get current connection status
 * @returns Promise with connection status string
 */
export function getConnectionStatus(): Promise<string> {
  return UhfTagModule.getConnectionStatus();
}

/**
 * Check if device is currently connected
 * @returns Promise with boolean connection state
 */
export function isConnected(): Promise<boolean> {
  return UhfTagModule.isConnected();
}

// ==================== RFID Operations ====================

/**
 * Start scanning for RFID tags
 * Listen to Events.RFID_TAG_READ to receive tag data
 */
export function startScan(): void {
  UhfTagModule.startScanRFID();
}

/**
 * Start scanning for a specific RFID tag
 * Only tags matching the provided EPC will trigger events
 * @param rfidTag - The EPC tag to search for
 */
export function startScanWithTag(rfidTag: string): void {
  UhfTagModule.startScanRFIDWithTag(rfidTag);
}

/**
 * Stop RFID tag scanning
 */
export function stopScan(): void {
  UhfTagModule.stopScanRFID();
}

/**
 * Clear all stored tag data
 * @returns Promise resolving to true if successful
 */
export function clearTags(): Promise<boolean> {
  return UhfTagModule.clearData();
}

// ==================== Power Management ====================

/**
 * Set the RFID reader power level
 * @param power - Power level (typically 5-30)
 * @returns Promise resolving to true if successful
 */
export function setPower(power: number): Promise<boolean> {
  return UhfTagModule.setPower(power);
}

/**
 * Get the current RFID reader power level
 * @returns Promise with current power level
 */
export function getPower(): Promise<number> {
  return UhfTagModule.getPower();
}

// ==================== Event Listeners ====================

/**
 * Add listener for BLE device discovery
 * @param listener - Callback function receiving discovered devices
 * @returns Subscription object for cleanup
 */
export function addBLEDeviceListener(
  listener: BLEDeviceListener
): EmitterSubscription {
  return eventEmitter.addListener(Events.BLE_DEVICE_FOUND, listener);
}

/**
 * Add listener for RFID tag reads
 * @param listener - Callback function receiving tag data
 * @returns Subscription object for cleanup
 */
export function addRfidTagListener(
  listener: RfidTagListener
): EmitterSubscription {
  return eventEmitter.addListener(Events.RFID_TAG_READ, listener);
}

/**
 * Add listener for connection status changes
 * @param listener - Callback function receiving status updates
 * @returns Subscription object for cleanup
 */
export function addConnectionStatusListener(
  listener: ConnectionStatusListener
): EmitterSubscription {
  return eventEmitter.addListener(Events.CONNECTION_STATUS, listener);
}

/**
 * Remove all event listeners
 */
export function removeAllListeners(): void {
  eventEmitter.removeAllListeners(Events.BLE_DEVICE_FOUND);
  eventEmitter.removeAllListeners(Events.RFID_TAG_READ);
  eventEmitter.removeAllListeners(Events.CONNECTION_STATUS);
}

// ==================== Default Export ====================

export default {
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
  Events,
};

