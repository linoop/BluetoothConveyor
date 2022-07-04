package com.technowavegroup.printerlib;

import android.bluetooth.BluetoothDevice;

public interface BTListener {

    void onDeviceConnected(boolean isConnected, String statusMessage, BluetoothDevice bluetoothDevice);

    void onMessageReceive(String message);

    void onDeviceStateChange(boolean completed, String status);

    void onDeviceDisconnected(boolean isDisconnected, String statusMessage);

    void onDeviceError(String errorMessage);

    void onSensorStateChange(int portNumber, boolean state, boolean rising, boolean falling);
}
