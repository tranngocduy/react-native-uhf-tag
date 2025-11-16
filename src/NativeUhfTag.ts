import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

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
  // Bluetooth operations
  scanBLE(): void;
  stopScanBLE(): void;
  connectAddress(address: string): Promise<string>;
  disconnect(): void;
  
  // RFID operations
  startScanRFID(): void;
  startScanRFIDWithTag(rfidTag: string): void;
  stopScanRFID(): void;
  clearData(): Promise<boolean>;
  
  // Power management
  setPower(power: number): Promise<boolean>;
  getPower(): Promise<number>;
  
  // Connection status
  getConnectionStatus(): Promise<string>;
  isConnected(): Promise<boolean>;
  
  // Add listener (for event emitter)
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('UhfTag');
