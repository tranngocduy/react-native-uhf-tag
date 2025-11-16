import type { EmitterSubscription } from 'react-native';
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
export declare const Events: {
    readonly BLE_DEVICE_FOUND: "onBLEDeviceFound";
    readonly RFID_TAG_READ: "onRfidTagRead";
    readonly CONNECTION_STATUS: "onConnectionStatusChanged";
};
/**
 * Start scanning for nearby Bluetooth devices
 * Listen to Events.BLE_DEVICE_FOUND to get discovered devices
 */
export declare function scanBLE(): void;
/**
 * Stop scanning for Bluetooth devices
 */
export declare function stopScanBLE(): void;
/**
 * Connect to a Bluetooth device by its address
 * @param address - The Bluetooth device MAC address
 * @returns Promise resolving to connected device info
 */
export declare function connectDevice(address: string): Promise<string>;
/**
 * Disconnect from the currently connected device
 */
export declare function disconnect(): void;
/**
 * Get current connection status
 * @returns Promise with connection status string
 */
export declare function getConnectionStatus(): Promise<string>;
/**
 * Check if device is currently connected
 * @returns Promise with boolean connection state
 */
export declare function isConnected(): Promise<boolean>;
/**
 * Start scanning for RFID tags
 * Listen to Events.RFID_TAG_READ to receive tag data
 */
export declare function startScan(): void;
/**
 * Start scanning for a specific RFID tag
 * Only tags matching the provided EPC will trigger events
 * @param rfidTag - The EPC tag to search for
 */
export declare function startScanWithTag(rfidTag: string): void;
/**
 * Stop RFID tag scanning
 */
export declare function stopScan(): void;
/**
 * Clear all stored tag data
 * @returns Promise resolving to true if successful
 */
export declare function clearTags(): Promise<boolean>;
/**
 * Set the RFID reader power level
 * @param power - Power level (typically 5-30)
 * @returns Promise resolving to true if successful
 */
export declare function setPower(power: number): Promise<boolean>;
/**
 * Get the current RFID reader power level
 * @returns Promise with current power level
 */
export declare function getPower(): Promise<number>;
/**
 * Add listener for BLE device discovery
 * @param listener - Callback function receiving discovered devices
 * @returns Subscription object for cleanup
 */
export declare function addBLEDeviceListener(listener: BLEDeviceListener): EmitterSubscription;
/**
 * Add listener for RFID tag reads
 * @param listener - Callback function receiving tag data
 * @returns Subscription object for cleanup
 */
export declare function addRfidTagListener(listener: RfidTagListener): EmitterSubscription;
/**
 * Add listener for connection status changes
 * @param listener - Callback function receiving status updates
 * @returns Subscription object for cleanup
 */
export declare function addConnectionStatusListener(listener: ConnectionStatusListener): EmitterSubscription;
/**
 * Remove all event listeners
 */
export declare function removeAllListeners(): void;
declare const _default: {
    scanBLE: typeof scanBLE;
    stopScanBLE: typeof stopScanBLE;
    connectDevice: typeof connectDevice;
    disconnect: typeof disconnect;
    getConnectionStatus: typeof getConnectionStatus;
    isConnected: typeof isConnected;
    startScan: typeof startScan;
    startScanWithTag: typeof startScanWithTag;
    stopScan: typeof stopScan;
    clearTags: typeof clearTags;
    setPower: typeof setPower;
    getPower: typeof getPower;
    addBLEDeviceListener: typeof addBLEDeviceListener;
    addRfidTagListener: typeof addRfidTagListener;
    addConnectionStatusListener: typeof addConnectionStatusListener;
    removeAllListeners: typeof removeAllListeners;
    Events: {
        readonly BLE_DEVICE_FOUND: "onBLEDeviceFound";
        readonly RFID_TAG_READ: "onRfidTagRead";
        readonly CONNECTION_STATUS: "onConnectionStatusChanged";
    };
};
export default _default;
