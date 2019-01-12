package com.example.aklesoft.heartrate_monitor;

import java.util.UUID;

class Constants {

    public static String SERVICE_STRING = "0000180d-0000-1000-8000-00805f9b34fb";
    public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

    public static String CHARACTERISTIC_ECHO_STRING = "00002a37-0000-1000-8000-00805f9b34fb";
    public static UUID CHARACTERISTIC_ECHO_UUID = UUID.fromString(CHARACTERISTIC_ECHO_STRING);

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final long SCAN_PERIOD = 5000;

    public static String ACTION_BROADCAST_RECEIVER = "com.example.aklesoft.heartrate_monitor.RECEIVER";
    public static String ACTION_BROADCAST_RECEIVER_DATA = "com.example.aklesoft.heartrate_monitor.DATA";
}
