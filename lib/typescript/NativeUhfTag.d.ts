import type { TurboModule } from 'react-native';
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
export interface Spec extends TurboModule {
    scanBLE(): void;
    stopScanBLE(): void;
    connectAddress(address: string): Promise<string>;
    disconnect(): void;
    startScanRFID(): void;
    startScanRFIDWithTag(rfidTag: string): void;
    stopScanRFID(): void;
    clearData(): Promise<boolean>;
    setPower(power: number): Promise<boolean>;
    getPower(): Promise<number>;
    getConnectionStatus(): Promise<string>;
    isConnected(): Promise<boolean>;
    addListener(eventName: string): void;
    removeListeners(count: number): void;
}
declare const _default: Spec;
export default _default;
