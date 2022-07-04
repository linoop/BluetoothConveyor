package com.technowavegroup.printerlib;

import android.bluetooth.BluetoothDevice;

public interface BTSelectDeviceListener {
    void onBTDeviceSelected(BluetoothDevice device);
}
