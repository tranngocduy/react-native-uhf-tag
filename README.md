# React Native UHF Tag 📡

[![npm version](https://badge.fury.io/js/react-native-uhf-tag.svg)](https://badge.fury.io/js/react-native-uhf-tag)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Modern React Native library for UHF RFID tag reading, built with **Kotlin** and optimized for **Chainway R6** and compatible devices.

## ✨ Features

- 🚀 **Modern Architecture** - Written in Kotlin with coroutines
- 📱 **Type-Safe** - Full TypeScript support with type definitions
- 🔌 **Easy Integration** - Simple API with event-driven architecture
- 🎯 **Hardware Support** - Chainway R6 UHF RFID Reader
- 📡 **Bluetooth LE** - Scan and connect to BLE devices
- 🏷️ **RFID Operations** - Single and continuous tag reading
- 🔍 **Filtered Scanning** - Search for specific EPC tags
- ⚡ **Power Management** - Adjustable reading power (5-30)
- 🎧 **Event Listeners** - Real-time notifications
- 🔘 **Hardware Trigger** - Support for physical trigger button

## 📦 Installation

```bash
npm install react-native-uhf-tag
# or
yarn add react-native-uhf-tag
```

### iOS (if needed)

```bash
cd ios && pod install && cd ..
```

### Android Setup

Add permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Bluetooth permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    
    <!-- Android 12+ Bluetooth permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    
    <!-- Location permissions (required for BLE scanning) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
</manifest>
```

## 🚀 Quick Start

```typescript
import UhfTag, { 
  addBLEDeviceListener, 
  addRfidTagListener,
  addConnectionStatusListener 
} from 'react-native-uhf-tag';

// 1. Request permissions (Android)
const requestPermissions = async () => {
  if (Platform.OS === 'android') {
    await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    ]);
  }
};

// 2. Scan for Bluetooth devices
UhfTag.scanBLE();

const bleSubscription = addBLEDeviceListener((device) => {
  console.log('Found device:', device.name, device.address);
});

// 3. Connect to device
const connectDevice = async (address: string) => {
  try {
    const deviceInfo = await UhfTag.connectDevice(address);
    console.log('Connected:', deviceInfo);
    
    // Set power level
    await UhfTag.setPower(25);
  } catch (error) {
    console.error('Connection failed:', error);
  }
};

// 4. Start reading RFID tags
UhfTag.startScan();

const rfidSubscription = addRfidTagListener((tag) => {
  console.log('Tag read:', tag.epc, 'RSSI:', tag.rssi);
});

// 5. Stop scanning
UhfTag.stopScan();

// 6. Cleanup
bleSubscription.remove();
rfidSubscription.remove();
```

## 📖 API Reference

### Bluetooth Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `scanBLE()` | - | `void` | Start scanning for BLE devices |
| `stopScanBLE()` | - | `void` | Stop BLE scanning |
| `connectDevice()` | `address: string` | `Promise<string>` | Connect to device by MAC address |
| `disconnect()` | - | `void` | Disconnect from current device |
| `isConnected()` | - | `Promise<boolean>` | Check if device is connected |
| `getConnectionStatus()` | - | `Promise<string>` | Get connection status |

### RFID Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `startScan()` | - | `void` | Start continuous RFID scanning |
| `startScanWithTag()` | `epc: string` | `void` | Scan for specific tag by EPC |
| `stopScan()` | - | `void` | Stop RFID scanning |
| `clearTags()` | - | `Promise<boolean>` | Clear stored tag data |

### Power Management

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `setPower()` | `power: number` | `Promise<boolean>` | Set power level (5-30) |
| `getPower()` | - | `Promise<number>` | Get current power level |

### Event Listeners

| Listener | Callback | Description |
|----------|----------|-------------|
| `addBLEDeviceListener()` | `(device: BluetoothDevice) => void` | Called when BLE device found |
| `addRfidTagListener()` | `(tag: RfidTag) => void` | Called when RFID tag read |
| `addConnectionStatusListener()` | `(status: ConnectionStatus) => void` | Called on connection status change |
| `removeAllListeners()` | - | Remove all event listeners |

### TypeScript Interfaces

```typescript
interface BluetoothDevice {
  name: string;
  address: string;
  rssi: string;
}

interface RfidTag {
  epc: string;
  rssi: string;
  timestamp: number;
}

interface ConnectionStatus {
  status: 'connected' | 'connecting' | 'disconnected' | 'error';
  deviceName?: string;
  deviceAddress?: string;
  message?: string;
}
```

## 💡 Usage Examples

### Complete React Component

```typescript
import React, { useEffect, useState } from 'react';
import { View, Button, FlatList, Text, Alert } from 'react-native';
import UhfTag, { 
  addBLEDeviceListener, 
  addRfidTagListener 
} from 'react-native-uhf-tag';

export default function RFIDScanner() {
  const [devices, setDevices] = useState([]);
  const [tags, setTags] = useState([]);
  const [scanning, setScanning] = useState(false);
  const [reading, setReading] = useState(false);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    // Subscribe to BLE device discovery
    const bleSubscription = addBLEDeviceListener((device) => {
      setDevices(prev => {
        if (prev.find(d => d.address === device.address)) return prev;
        return [...prev, device];
      });
    });

    // Subscribe to RFID tag reads
    const rfidSubscription = addRfidTagListener((tag) => {
      setTags(prev => {
        const exists = prev.find(t => t.epc === tag.epc);
        if (exists) {
          return prev.map(t => t.epc === tag.epc ? tag : t);
        }
        return [tag, ...prev];
      });
    });

    return () => {
      bleSubscription.remove();
      rfidSubscription.remove();
      UhfTag.stopScanBLE();
      UhfTag.stopScan();
    };
  }, []);

  const handleScanBLE = () => {
    if (scanning) {
      UhfTag.stopScanBLE();
      setScanning(false);
    } else {
      setDevices([]);
      UhfTag.scanBLE();
      setScanning(true);
      
      // Auto stop after 10 seconds
      setTimeout(() => {
        UhfTag.stopScanBLE();
        setScanning(false);
      }, 10000);
    }
  };

  const handleConnect = async (address: string) => {
    try {
      await UhfTag.connectDevice(address);
      await UhfTag.setPower(25);
      setConnected(true);
      Alert.alert('Success', 'Device connected');
    } catch (error) {
      Alert.alert('Error', 'Failed to connect');
    }
  };

  const handleScanRFID = () => {
    if (reading) {
      UhfTag.stopScan();
      setReading(false);
    } else {
      setTags([]);
      UhfTag.startScan();
      setReading(true);
    }
  };

  return (
    <View style={{ flex: 1, padding: 20 }}>
      <Text style={{ fontSize: 20, fontWeight: 'bold', marginBottom: 10 }}>
        Bluetooth Devices
      </Text>
      
      <Button 
        title={scanning ? "Stop Scan" : "Scan BLE"} 
        onPress={handleScanBLE}
      />
      
      <FlatList
        data={devices}
        keyExtractor={item => item.address}
        renderItem={({ item }) => (
          <View style={{ padding: 10, borderBottomWidth: 1 }}>
            <Text>{item.name}</Text>
            <Text>{item.address}</Text>
            <Button 
              title="Connect" 
              onPress={() => handleConnect(item.address)}
            />
          </View>
        )}
      />

      <Text style={{ fontSize: 20, fontWeight: 'bold', marginTop: 20 }}>
        RFID Tags ({tags.length})
      </Text>
      
      <Button 
        title={reading ? "Stop Reading" : "Start Reading"}
        onPress={handleScanRFID}
        disabled={!connected}
      />

      <FlatList
        data={tags}
        keyExtractor={(item, index) => `${item.epc}-${index}`}
        renderItem={({ item }) => (
          <View style={{ padding: 10, borderBottomWidth: 1 }}>
            <Text style={{ fontFamily: 'monospace' }}>{item.epc}</Text>
            <Text>RSSI: {item.rssi} dBm</Text>
          </View>
        )}
      />
    </View>
  );
}
```

### Search for Specific Tag

```typescript
// Search for a specific EPC tag
const searchTag = 'E2003412751414110824BE70';
UhfTag.startScanWithTag(searchTag);

// Only tags matching this EPC will trigger events
addRfidTagListener((tag) => {
  if (tag.epc === searchTag) {
    console.log('Found target tag!');
  }
});
```

### Power Management

```typescript
// Set power level (higher = longer range, more battery)
await UhfTag.setPower(30); // Max power

// Get current power level
const currentPower = await UhfTag.getPower();
console.log('Current power:', currentPower);
```

## 🔧 Troubleshooting

### Module not found

```bash
cd android && ./gradlew clean && cd ..
npx react-native run-android
```

### Bluetooth not working

- Check permissions are granted
- Enable Bluetooth in device settings
- Enable Location services (Android < 12)

### RFID not reading

- Verify device is connected: `await UhfTag.isConnected()`
- Check power level is set: `await UhfTag.setPower(25)`
- Ensure tags are within reading range
- Try increasing power level

### Build errors

```bash
# Clean and rebuild
cd android
./gradlew clean
cd ..
npx react-native run-android
```

## 📝 Requirements

- React Native >= 0.70.0
- Android minSdkVersion 21
- iOS 11.0+ (not fully implemented yet)
- Kotlin 1.8.0+

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- Uses Chainway Device API SDK for RFID operations
- Built with React Native and Kotlin

## 📧 Support

For issues and questions:
- 🐛 [GitHub Issues](https://github.com/tranngocduy/react-native-uhf-tag/issues)
- 📖 [Documentation](https://github.com/tranngocduy/react-native-uhf-tag#readme)

## 🔗 Links

- [npm package](https://www.npmjs.com/package/react-native-uhf-tag)
- [GitHub Repository](https://github.com/tranngocduy/react-native-uhf-tag)
- [Change Log](https://github.com/tranngocduy/react-native-uhf-tag/releases)

---

Made with ❤️ for the React Native community
