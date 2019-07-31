package com.example.aklesoft.heartrate_monitor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.aklesoft.heartrate_monitor.Constants.CHARACTERISTIC_ECHO_STRING;
import static com.example.aklesoft.heartrate_monitor.Constants.SERVICE_STRING;
import static com.example.aklesoft.heartrate_monitor.MainSettingsActivity.TAG;


public class BluetoothUtils {

    // Characteristics

    public static List<BluetoothGattCharacteristic> findCharacteristics(BluetoothGatt bluetoothGatt) {
        List<BluetoothGattCharacteristic> matchingCharacteristics = new ArrayList<>();

        List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
        BluetoothGattService service = BluetoothUtils.findService(serviceList);
        Log.e(TAG, "service -> " + service);
        if (service == null) {
            return matchingCharacteristics;
        }

        Log.e(TAG, "service.getCharacteristics() -> " + service.getCharacteristics());
        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            Log.e(TAG, "characteristic -> " + characteristic);
            if (isMatchingCharacteristic(characteristic)) {
                Log.e(TAG, "add characteristic -> " + characteristic);
                if( !matchingCharacteristics.contains(matchingCharacteristics)) {
                    matchingCharacteristics.add(characteristic);
                }
            }
        }

        return matchingCharacteristics;
    }

    @Nullable
    public static BluetoothGattCharacteristic findEchoCharacteristic(BluetoothGatt bluetoothGatt) {
        return findCharacteristic(bluetoothGatt, CHARACTERISTIC_ECHO_STRING);
    }

    @Nullable
    private static BluetoothGattCharacteristic findCharacteristic(BluetoothGatt bluetoothGatt, String uuidString) {
        List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
        Log.e(TAG, "findCharacteristic -> " + serviceList.toString());
        BluetoothGattService service = BluetoothUtils.findService(serviceList);
        if (service == null) {
            return null;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            if (characteristicMatches(characteristic, uuidString)) {
                return characteristic;
            }
        }

        return null;
    }

    public static boolean isEchoCharacteristic(BluetoothGattCharacteristic characteristic) {
        return characteristicMatches(characteristic, CHARACTERISTIC_ECHO_STRING);
    }

    private static boolean characteristicMatches(BluetoothGattCharacteristic characteristic, String uuidString) {
        if (characteristic == null) {
            return false;
        }
        UUID uuid = characteristic.getUuid();
        return uuidMatches(uuid.toString(), uuidString);
    }

    private static boolean isMatchingCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            return false;
        }
        Log.e(TAG, "characteristic.getUuid() -> " + characteristic.getUuid());
        UUID uuid = characteristic.getUuid();
        return matchesCharacteristicUuidString(uuid.toString());
    }

    private static boolean matchesCharacteristicUuidString(String characteristicIdString) {
        return uuidMatches(characteristicIdString, CHARACTERISTIC_ECHO_STRING);
    }

    public static boolean requiresResponse(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
    }

    public static boolean requiresConfirmation(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;
    }

    // Service

    private static boolean matchesServiceUuidString(String serviceIdString) {
        Log.e(TAG, "matchesServiceUuidString -> " + serviceIdString);
        return uuidMatches(serviceIdString, SERVICE_STRING);
    }

    @Nullable
    private static BluetoothGattService findService(List<BluetoothGattService> serviceList) {
        for (BluetoothGattService service : serviceList) {
            String serviceIdString = service.getUuid().toString();
            Log.e(TAG, "findService -> " + serviceIdString);

            if (matchesServiceUuidString(serviceIdString)) {
                return service;
            }
        }
        return null;
    }

    // String matching

    // If manually filtering, substring to match:
    // 0000XXXX-0000-0000-0000-000000000000
    private static boolean uuidMatches(String uuidString, String... matches) {

        for (String match : matches) {
            if (uuidString.equalsIgnoreCase(match)) {
                Log.e(TAG, "uuidMatches -> " + uuidString);
                Log.e(TAG, "uuidMatches -> " + match);
                return true;
            }
        }

        return false;
    }
}
