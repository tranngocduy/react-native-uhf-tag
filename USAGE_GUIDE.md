# 📖 Hướng Dẫn Sử Dụng React Native UHF Tag

Package đã được publish lên npm với tên: **`react-native-uhf-tag`**

## 🚀 Cài Đặt

### 1. Cài đặt package

```bash
npm install react-native-uhf-tag
# hoặc
yarn add react-native-uhf-tag
```

### 2. Cài đặt cho iOS (nếu cần)

```bash
cd ios && pod install && cd ..
```

### 3. Rebuild ứng dụng

```bash
# Android
npx react-native run-android

# iOS
npx react-native run-ios
```

## 📱 Cấu Hình Android

### AndroidManifest.xml

Thêm các quyền cần thiết vào `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Bluetooth permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    
    <!-- Android 12+ Bluetooth permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    
    <!-- Location permissions (required for BLE scanning on Android < 12) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <application ...>
        ...
    </application>
</manifest>
```

### MainApplication.java/kt

Package được tự động link nếu dùng React Native >= 0.60. Nếu cần thêm thủ công:

```java
import com.uhftag.UhfTagPackage; // Thêm import

@Override
protected List<ReactPackage> getPackages() {
  List<ReactPackage> packages = new PackageList(this).getPackages();
  packages.add(new UhfTagPackage()); // Thêm package
  return packages;
}
```

## 💻 Sử Dụng Trong Code

### Import

```typescript
import UhfTag, { 
  addBLEDeviceListener, 
  addRfidTagListener,
  addConnectionStatusListener,
  Events 
} from 'react-native-uhf-tag';
```

### 1️⃣ Xin Quyền (Android)

```typescript
import { PermissionsAndroid, Platform } from 'react-native';

const requestPermissions = async () => {
  if (Platform.OS === 'android') {
    if (Platform.Version >= 31) {
      // Android 12+
      const granted = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      ]);
      
      return Object.values(granted).every(
        status => status === PermissionsAndroid.RESULTS.GRANTED
      );
    } else {
      // Android < 12
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
      );
      return granted === PermissionsAndroid.RESULTS.GRANTED;
    }
  }
  return true;
};
```

### 2️⃣ Quét Bluetooth Devices

```typescript
import React, { useEffect, useState } from 'react';
import { View, Text, Button, FlatList } from 'react-native';
import UhfTag, { addBLEDeviceListener } from 'react-native-uhf-tag';

function BluetoothScanner() {
  const [devices, setDevices] = useState([]);
  const [scanning, setScanning] = useState(false);

  useEffect(() => {
    // Lắng nghe thiết bị BLE được tìm thấy
    const subscription = addBLEDeviceListener((device) => {
      console.log('Found:', device.name, device.address);
      
      setDevices(prev => {
        // Tránh trùng lặp
        if (prev.find(d => d.address === device.address)) {
          return prev;
        }
        return [...prev, device];
      });
    });

    return () => {
      subscription.remove();
      UhfTag.stopScanBLE();
    };
  }, []);

  const startScan = async () => {
    const hasPermission = await requestPermissions();
    if (!hasPermission) {
      alert('Cần cấp quyền để quét Bluetooth');
      return;
    }
    
    setDevices([]);
    setScanning(true);
    UhfTag.scanBLE();
    
    // Tự động dừng sau 10 giây
    setTimeout(() => {
      UhfTag.stopScanBLE();
      setScanning(false);
    }, 10000);
  };

  const stopScan = () => {
    UhfTag.stopScanBLE();
    setScanning(false);
  };

  return (
    <View>
      <Button 
        title={scanning ? "Đang quét..." : "Quét Bluetooth"} 
        onPress={scanning ? stopScan : startScan}
      />
      
      <FlatList
        data={devices}
        keyExtractor={item => item.address}
        renderItem={({ item }) => (
          <View style={{ padding: 10, borderBottomWidth: 1 }}>
            <Text style={{ fontWeight: 'bold' }}>{item.name}</Text>
            <Text>{item.address}</Text>
            <Text>RSSI: {item.rssi}</Text>
          </View>
        )}
      />
    </View>
  );
}
```

### 3️⃣ Kết Nối Với Thiết Bị

```typescript
const connectToDevice = async (address) => {
  try {
    const deviceInfo = await UhfTag.connectDevice(address);
    console.log('Kết nối thành công:', deviceInfo);
    
    // Kiểm tra kết nối
    const isConnected = await UhfTag.isConnected();
    console.log('Trạng thái kết nối:', isConnected);
    
    // Cài đặt công suất đọc (5-30)
    await UhfTag.setPower(25);
    
    alert(`Đã kết nối với ${deviceInfo}`);
  } catch (error) {
    console.error('Lỗi kết nối:', error);
    alert('Không thể kết nối với thiết bị');
  }
};

// Ngắt kết nối
const disconnect = () => {
  UhfTag.disconnect();
  console.log('Đã ngắt kết nối');
};
```

### 4️⃣ Đọc Thẻ RFID

```typescript
function RFIDReader() {
  const [tags, setTags] = useState([]);
  const [isReading, setIsReading] = useState(false);

  useEffect(() => {
    // Lắng nghe thẻ RFID được đọc
    const subscription = addRfidTagListener((tag) => {
      console.log('Đọc thẻ:', tag.epc, 'RSSI:', tag.rssi);
      
      setTags(prev => {
        // Thêm thẻ mới vào đầu danh sách
        const exists = prev.find(t => t.epc === tag.epc);
        if (exists) {
          // Cập nhật thẻ đã có
          return prev.map(t => t.epc === tag.epc ? tag : t);
        }
        return [tag, ...prev];
      });
    });

    return () => {
      subscription.remove();
      UhfTag.stopScan();
    };
  }, []);

  const startReading = () => {
    setIsReading(true);
    UhfTag.startScan();
  };

  const stopReading = () => {
    setIsReading(false);
    UhfTag.stopScan();
  };

  const clearAllTags = async () => {
    const success = await UhfTag.clearTags();
    if (success) {
      setTags([]);
      console.log('Đã xóa tất cả thẻ');
    }
  };

  return (
    <View>
      <View style={{ flexDirection: 'row', padding: 10 }}>
        <Button 
          title={isReading ? "Dừng Đọc" : "Bắt Đầu Đọc"} 
          onPress={isReading ? stopReading : startReading}
        />
        <Button title="Xóa Tất Cả" onPress={clearAllTags} />
      </View>

      <Text style={{ padding: 10 }}>
        Tổng số thẻ: {tags.length}
      </Text>

      <FlatList
        data={tags}
        keyExtractor={(item, index) => `${item.epc}-${index}`}
        renderItem={({ item }) => (
          <View style={{ padding: 10, borderBottomWidth: 1 }}>
            <Text style={{ fontFamily: 'monospace' }}>{item.epc}</Text>
            <Text>RSSI: {item.rssi} dBm</Text>
            <Text>
              Thời gian: {new Date(item.timestamp).toLocaleTimeString()}
            </Text>
          </View>
        )}
      />
    </View>
  );
}
```

### 5️⃣ Tìm Kiếm Thẻ Cụ Thể

```typescript
const searchSpecificTag = (epc) => {
  // Chỉ đọc thẻ có EPC khớp với giá trị này
  UhfTag.startScanWithTag(epc);
  console.log('Đang tìm thẻ:', epc);
};

// Ví dụ
searchSpecificTag('E2003412751414110824BE70');
```

### 6️⃣ Quản Lý Công Suất

```typescript
// Đặt công suất (5-30)
const setPowerLevel = async (power) => {
  try {
    const success = await UhfTag.setPower(power);
    if (success) {
      console.log('Đã đặt công suất:', power);
    } else {
      console.log('Không thể đặt công suất');
    }
  } catch (error) {
    console.error('Lỗi:', error);
  }
};

// Lấy công suất hiện tại
const getCurrentPower = async () => {
  try {
    const power = await UhfTag.getPower();
    console.log('Công suất hiện tại:', power);
    return power;
  } catch (error) {
    console.error('Lỗi:', error);
  }
};
```

### 7️⃣ Theo Dõi Trạng Thái Kết Nối

```typescript
useEffect(() => {
  const subscription = addConnectionStatusListener((status) => {
    console.log('Trạng thái kết nối:', status.status);
    
    switch (status.status) {
      case 'connected':
        console.log('Đã kết nối:', status.deviceName);
        break;
      case 'connecting':
        console.log('Đang kết nối...');
        break;
      case 'disconnected':
        console.log('Đã ngắt kết nối');
        break;
      case 'error':
        console.error('Lỗi:', status.message);
        break;
    }
  });

  return () => subscription.remove();
}, []);
```

## 📋 Ví Dụ Hoàn Chỉnh

```typescript
import React, { useEffect, useState } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  Button,
  FlatList,
  StyleSheet,
  PermissionsAndroid,
  Platform,
  Alert,
} from 'react-native';
import UhfTag, {
  addBLEDeviceListener,
  addRfidTagListener,
  addConnectionStatusListener,
} from 'react-native-uhf-tag';

export default function App() {
  const [devices, setDevices] = useState([]);
  const [tags, setTags] = useState([]);
  const [scanning, setScanning] = useState(false);
  const [reading, setReading] = useState(false);
  const [connected, setConnected] = useState(false);
  const [connectedDevice, setConnectedDevice] = useState('');

  useEffect(() => {
    requestPermissions();

    const bleSubscription = addBLEDeviceListener((device) => {
      setDevices((prev) => {
        if (prev.find((d) => d.address === device.address)) return prev;
        return [...prev, device];
      });
    });

    const rfidSubscription = addRfidTagListener((tag) => {
      setTags((prev) => {
        const exists = prev.find((t) => t.epc === tag.epc);
        if (exists) {
          return prev.map((t) => (t.epc === tag.epc ? tag : t));
        }
        return [tag, ...prev];
      });
    });

    const statusSubscription = addConnectionStatusListener((status) => {
      if (status.status === 'connected') {
        setConnected(true);
        setConnectedDevice(status.deviceName || '');
      } else if (status.status === 'disconnected') {
        setConnected(false);
        setConnectedDevice('');
      }
    });

    return () => {
      bleSubscription.remove();
      rfidSubscription.remove();
      statusSubscription.remove();
      UhfTag.stopScanBLE();
      UhfTag.stopScan();
    };
  }, []);

  const requestPermissions = async () => {
    if (Platform.OS === 'android') {
      const permissions =
        Platform.Version >= 31
          ? [
              PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
              PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
              PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
            ]
          : [PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION];

      await PermissionsAndroid.requestMultiple(permissions);
    }
  };

  const handleScanBLE = () => {
    if (scanning) {
      UhfTag.stopScanBLE();
      setScanning(false);
    } else {
      setDevices([]);
      UhfTag.scanBLE();
      setScanning(true);

      setTimeout(() => {
        UhfTag.stopScanBLE();
        setScanning(false);
      }, 10000);
    }
  };

  const handleConnect = async (address) => {
    try {
      const deviceInfo = await UhfTag.connectDevice(address);
      await UhfTag.setPower(25);
      Alert.alert('Thành công', `Đã kết nối với ${deviceInfo}`);
    } catch (error) {
      Alert.alert('Lỗi', 'Không thể kết nối với thiết bị');
    }
  };

  const handleDisconnect = () => {
    UhfTag.disconnect();
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
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>UHF RFID Reader</Text>
        {connected && (
          <Text style={styles.status}>Đã kết nối: {connectedDevice}</Text>
        )}
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>
          Bluetooth Devices ({devices.length})
        </Text>
        <Button
          title={scanning ? 'Dừng Quét BLE' : 'Quét BLE'}
          onPress={handleScanBLE}
        />
        {connected && (
          <Button title="Ngắt Kết Nối" onPress={handleDisconnect} color="red" />
        )}
        <FlatList
          data={devices}
          keyExtractor={(item) => item.address}
          style={styles.list}
          renderItem={({ item }) => (
            <View style={styles.item}>
              <Text style={styles.itemTitle}>{item.name}</Text>
              <Text>{item.address}</Text>
              <Button
                title="Kết Nối"
                onPress={() => handleConnect(item.address)}
              />
            </View>
          )}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>RFID Tags ({tags.length})</Text>
        <Button
          title={reading ? 'Dừng Đọc RFID' : 'Đọc RFID'}
          onPress={handleScanRFID}
          disabled={!connected}
        />
        <FlatList
          data={tags}
          keyExtractor={(item, index) => `${item.epc}-${index}`}
          style={styles.list}
          renderItem={({ item }) => (
            <View style={styles.item}>
              <Text style={styles.epc}>{item.epc}</Text>
              <Text>RSSI: {item.rssi} dBm</Text>
            </View>
          )}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 16,
    backgroundColor: '#2196F3',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: 'white',
  },
  status: {
    color: 'white',
    marginTop: 4,
  },
  section: {
    flex: 1,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  list: {
    marginTop: 8,
  },
  item: {
    backgroundColor: 'white',
    padding: 12,
    marginBottom: 8,
    borderRadius: 8,
  },
  itemTitle: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  epc: {
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    fontSize: 14,
  },
});
```

## 🎯 API Reference

### Methods

| Method | Tham số | Trả về | Mô tả |
|--------|---------|--------|-------|
| `scanBLE()` | - | `void` | Bắt đầu quét BLE devices |
| `stopScanBLE()` | - | `void` | Dừng quét BLE |
| `connectDevice()` | `address: string` | `Promise<string>` | Kết nối với thiết bị |
| `disconnect()` | - | `void` | Ngắt kết nối |
| `isConnected()` | - | `Promise<boolean>` | Kiểm tra kết nối |
| `getConnectionStatus()` | - | `Promise<string>` | Lấy trạng thái kết nối |
| `startScan()` | - | `void` | Bắt đầu đọc RFID |
| `startScanWithTag()` | `epc: string` | `void` | Đọc thẻ cụ thể |
| `stopScan()` | - | `void` | Dừng đọc RFID |
| `clearTags()` | - | `Promise<boolean>` | Xóa dữ liệu thẻ |
| `setPower()` | `power: number` | `Promise<boolean>` | Đặt công suất (5-30) |
| `getPower()` | - | `Promise<number>` | Lấy công suất hiện tại |

### Events

| Event | Callback | Mô tả |
|-------|----------|-------|
| `addBLEDeviceListener` | `(device: BluetoothDevice) => void` | Phát hiện BLE device |
| `addRfidTagListener` | `(tag: RfidTag) => void` | Đọc được thẻ RFID |
| `addConnectionStatusListener` | `(status: ConnectionStatus) => void` | Thay đổi trạng thái kết nối |

## 🔧 Troubleshooting

### Lỗi "Module not found"
```bash
# Xóa cache và rebuild
cd android && ./gradlew clean && cd ..
npx react-native run-android
```

### Không quét được Bluetooth
- Kiểm tra quyền đã được cấp
- Bật Bluetooth trong Settings
- Bật Location services (Android < 12)

### Không đọc được thẻ RFID
- Đảm bảo đã kết nối với reader
- Kiểm tra công suất đã được set
- Thẻ RFID phải ở trong phạm vi

## 📞 Hỗ Trợ

GitHub: https://github.com/tranngocduy/react-native-uhf-tag
