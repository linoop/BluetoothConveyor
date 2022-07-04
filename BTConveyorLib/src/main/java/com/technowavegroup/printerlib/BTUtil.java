package com.technowavegroup.printerlib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BTUtil {
    public static final char CONVEYOR_STOP = 'a';
    public static final char CONVEYOR_FORWARD = 'b';
    public static final char CONVEYOR_REVERSE = 'c';
    public static final char BUZZ_ON = 'd';
    public static final char BUZZ_OFF = 'e';
    public static final char RED_ON = 'f';
    public static final char RED_OFF = 'g';
    public static final char GREEN_ON = 'h';
    public static final char GREEN_OFF = 'i';
    public static final char YELLOW_ON = 'j';
    public static final char YELLOW_OFF = 'k';
    public static final char YELLOW_BLINK = 'l';
    public static final char YELLOW_BLINK_OFF = 'm';
    public static final String S1_ON = "a";
    public static final String S1_OFF = "b";
    public static final String S2_ON = "c";
    public static final String S2_OFF = "d";
    public static final int BT_ENABLE_REQUEST_CODE = 100;
    public static final String DEVICE_UUID = "00001101-0000-1000-8000-00805f9b34fb";
    @SuppressLint("StaticFieldLeak")
    private static BTUtil btUtilInstance;
    private final Activity activity;
    private final List<BluetoothDevice> paredDevices = new ArrayList<>();
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BluetoothDevice bluetoothDevice = null;
    private final String defaultDeviceMac;

    private boolean isConnected = false;
    private int readBufferPosition;
    private volatile boolean stopWorker;

    private final BTListener btListener;

    /*public static synchronized BTUtil getInstance(Activity activity, PrintStatusListener printStatusListener) {
        if (btUtilInstance == null) {
            btUtilInstance = new BTUtil(activity, printStatusListener);
        }
        return btUtilInstance;
    }*/

    public BTUtil(Activity activity, BTListener btListener) {
        this.activity = activity;
        this.btListener = btListener;
        defaultDeviceMac = BTPrefManager.getInstance(activity).getDeviceMacAddress();
        findBTDevices();
    }

    public void findBTDevices() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                btListener.onDeviceError("Bluetooth adapter not found!");
            }
            assert bluetoothAdapter != null;
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(intent, BT_ENABLE_REQUEST_CODE);
            }
            Set<BluetoothDevice> availParedDevices = bluetoothAdapter.getBondedDevices();
            if (availParedDevices.size() > 0) {
                for (BluetoothDevice device : availParedDevices) {
                    ParcelUuid[] uuids = device.getUuids();
                    /*for (ParcelUuid uuid : uuids) {
                        if (uuid.getUuid().toString().equals(PRINTER_UUID)) {
                            paredDevices.add(device);
                            break;
                        }
                    }*/
                    paredDevices.add(device);
                    if (defaultDeviceMac.equals(device.getAddress())) {
                        bluetoothDevice = device;
                    }
                }
            } else {
                btListener.onDeviceError("No bluetooth device available!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void chooseBTDeviceDialog() {
        BTDeviceListDialog btDeviceListDialog = new BTDeviceListDialog(activity, paredDevices, device -> {
            bluetoothDevice = device;
            connectBTDevice();
        });
        btDeviceListDialog.show();
    }

    public void connectBTDevice() {
        /*if (defaultPrinterMac.equals("")) {
            printStatusListener.onDeviceConnected(false, "Failed to get default printer!", bluetoothDevice);
            return;
        }*/
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.showProgress("Please wait", bluetoothDevice != null ? "Connecting to " + bluetoothDevice.getName() : "Bluetooth device error");
        progressDialog.show();
        new Thread(() -> {
            try {
                if (bluetoothDevice != null) {
                    //Old code
                    UUID uuid = UUID.fromString(DEVICE_UUID);
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    /*
                     * New code
                     * */
                    /*connection = new BluetoothConnectionInsecure(bluetoothDevice.getAddress());
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    connection.open();*/
                    isConnected = true;
                    beginListenForData();
                    listenSensorsState();
                } else {
                    isConnected = false;
                }
                progressDialog.dismissProgress();
            } catch (Exception e) {
                e.printStackTrace();
                isConnected = false;
                progressDialog.dismissProgress();
            } finally {
                if (isConnected) {
                    btListener.onDeviceConnected(true, "Conveyor connected successfully", bluetoothDevice);
                    BTPrefManager.getInstance(activity).saveDeviceMacAddress(bluetoothDevice.getAddress());
                } else {
                    btListener.onDeviceConnected(false, "Failed to connect Conveyor!", bluetoothDevice);
                }
            }
        }).start();
    }

    public void sendCommand(char command) {
        if (bluetoothDevice != null && isConnected) {
            Thread commandThread = new Thread(() -> {
                try {
                    //Old codes
                    outputStream.write(String.valueOf(command).getBytes());
                    //connection.write(text.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    btListener.onDeviceStateChange(false, "Conveyor error - Unable to send command!");
                }
            });
            commandThread.setPriority(Thread.MAX_PRIORITY);
            commandThread.start();
        } else {
            btListener.onDeviceStateChange(false, "Conveyor error - Unable to send command!");
        }
    }

    public void beginListenForData() {
        // final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];
        Thread receiveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                try {
                    int bytesAvailable = inputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        inputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;
                                System.out.println("Client message " + data);
                                switch (data) {
                                    case S1_ON:
                                        firstSensorState = true;
                                        break;
                                    case S1_OFF:
                                        firstSensorState = false;
                                        break;
                                    case S2_ON:
                                        secondSensorState = true;
                                        break;
                                    case S2_OFF:
                                        secondSensorState = false;
                                        break;
                                    default:
                                        btListener.onMessageReceive(data);
                                        break;
                                }
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException ex) {
                    stopWorker = true;
                }
            }
        });
        receiveThread.setPriority(Thread.MAX_PRIORITY);
        receiveThread.start();
    }

    private boolean firstSensorState = false;
    private boolean secondSensorState = false;

    private void listenSensorsState() {
        new Thread(() -> {
            while (true) {
                try {
                    boolean temp1 = firstSensorState;
                    boolean temp2 = secondSensorState;
                    Thread.sleep(400);
                    if (temp1 != firstSensorState)
                        if (temp1) btListener.onSensorStateChange(1, false, false, true);
                        else btListener.onSensorStateChange(1, true, true, false);
                    if (temp2 != secondSensorState)
                        if (temp2) btListener.onSensorStateChange(2, false, false, true);
                        else btListener.onSensorStateChange(2, true, true, false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public void disconnectBTDevice() {
        if (bluetoothDevice != null && isConnected) {
            new Thread(() -> {
                try {
                    //Old code
                    outputStream.close();
                    inputStream.close();
                    bluetoothSocket.close();
                    //New code
                    /*connection.close();
                    Looper.myLooper().quit();*/
                    isConnected = false;
                    Log.d("Device connection", "Device disconnected successfully");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (isConnected) {
                        btListener.onDeviceDisconnected(false, "Failed to disconnect!!");
                        Log.d("Device connection", "Failed to disconnect!!");
                    } else {
                        btListener.onDeviceDisconnected(true, "Conveyor disconnected successfully");
                    }
                }
            }).start();
        } else {
            btListener.onDeviceDisconnected(false, "Conveyor not found");
        }
    }
}
