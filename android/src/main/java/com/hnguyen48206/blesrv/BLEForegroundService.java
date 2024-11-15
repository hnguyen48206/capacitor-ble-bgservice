package com.hnguyen48206.blesrv;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiresApi(api = Build.VERSION_CODES.S)
public class BLEForegroundService extends Service {
    String[] runtimeList = {
            BLUETOOTH_SCAN,
            BLUETOOTH_CONNECT,
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION,
            BLUETOOTH,
            BLUETOOTH_ADMIN
    };
    private static final Boolean DEBUG = true;
    private static final String TAG = "BLEForegroundService";
    private static final String CHANNEL_ID = "BLEForegroundServiceChannel";
    private static final String PREFS_NAME = "CapacitorStorage";
    private static final String SERVICE_RUNNING_KEY = "serviceRunning";
    private static final String DEVICE_LIST_KEY = "MacBluetoothsConnected";
    private static final String BLE_CONFIG_KEY = "BLEConfigs";

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private String devicelistStr;
    private String bleconfigsStr;
    private JSONArray listOfDevices;
    private final Handler handler = new Handler();
    private long SCAN_PERIOD = 0;
    private long DELAY_PERIOD = 0;
    private final Set<String> detectedDevices = new HashSet<>();
    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();
    List<ScanFilter> filters = new ArrayList<>();

    private ExecutorService executorService;
    private Context context;

    private boolean isTesting = false;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        executorService = Executors.newSingleThreadExecutor();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Service started");
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                devicelistStr = prefs.getString(DEVICE_LIST_KEY, "");
//                Log.d(TAG, "devicelistStr: " + devicelistStr);
                bleconfigsStr = prefs.getString(BLE_CONFIG_KEY, "");
                if (DEBUG) {
                    setDebugDefault();
                }
                convertDevicelistStrBackToObjectAndBuildFilters();
                convertBLEconfigsStrBackToObject();
                if (listOfDevices != null) {
                    startForeground(1, getNotification("BLE Service", "Scanning for Devices."));
                    startBleScan();
                    saveServiceState(true);
                } else {
                    Log.d(TAG, "No known device ID found in storage. Service will not start scanning.");
                    stopSelf();
                }
            }
        });

        return START_STICKY;
    }

    private void setDebugDefault() {
        if (devicelistStr.isEmpty())
            devicelistStr = "[{\"mac\":\"78:02:B7:08:14:51\", \"vehicleID\":\"ABC\",\"status\":\"on\"}]";
        if (bleconfigsStr.isEmpty()) {
            SCAN_PERIOD = 10000;
            DELAY_PERIOD = 5000;
        }
    }

    private void convertDevicelistStrBackToObjectAndBuildFilters() {
        try {
            filters.clear();
            listOfDevices = new JSONArray(devicelistStr);
            for (int i = 0; i < listOfDevices.length(); i++) {
                JSONObject jsonObject = listOfDevices.getJSONObject(i);
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setDeviceAddress(jsonObject.getString("mac"))
                        .build();
                filters.add(scanFilter);
//                System.out.println(jsonObject.toString(2)); // Pretty print with 2 spaces indentation
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void convertBLEconfigsStrBackToObject() {
        if (bleconfigsStr != "")
            try {
                JSONObject jsonObject = new JSONObject(bleconfigsStr);
                SCAN_PERIOD = jsonObject.getLong("scan_period");
                DELAY_PERIOD = jsonObject.getLong("scan_delay");
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service stopped");
        stopBleScan();
        saveServiceState(false);
        executorService.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBleScan() {
        if (!isScanning) {
            handler.post(scanRunnable);
        }
    }

    private void stopBleScan() {
        if (isScanning) {
            if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(scanCallback);
                isScanning = false;
                handler.removeCallbacks(scanRunnable);
                Log.d(TAG, "BLE scan stopped");
            }
        }
    }


    private final Runnable scanRunnable = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            if (checkActivateStatus() && checkPermissions()) {
                if (!isScanning) {
                    detectedDevices.clear();
                    Log.d(TAG, "Starting BLE scan");
//                    bluetoothLeScanner.startScan(scanCallback);
                    bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
                    isScanning = true;
                    handler.postDelayed(scanRunnable, SCAN_PERIOD);
                } else {
                    bluetoothLeScanner.stopScan(scanCallback);
                    isScanning = false;
                    Log.d(TAG, "Stopping BLE scan");
                    updateDeviceStatus();
                    handler.postDelayed(scanRunnable, DELAY_PERIOD);
                }
            } else {
                stopBleScan();
                stopSelf();
            }
        }
    };


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceAddress = result.getDevice().getAddress();
//            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "Device found: " + deviceAddress + "/" + result.getDevice().getName());
//            }
            detectedDevices.add(deviceAddress);

            //test connecting to device name SCANTOOL
            // if(deviceAddress.equals("78:02:B7:08:14:51") && !isTesting)
            //     connectToGATTServer(result.getDevice());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "BLE scan failed with error code: " + errorCode);
        }
    };

    private void updateDeviceStatus() {
        try {
            JSONArray newListOfDevices = new JSONArray();

            for (int i = 0; i < listOfDevices.length(); i++) {
                JSONObject device = listOfDevices.getJSONObject(i);
                String mac = device.getString("mac");
                if (detectedDevices.contains(mac)) {
                    Log.d(TAG, "Known device is online: " + mac);
                    device.put("status", "on");
                } else {
                    Log.d(TAG, "Known device is offline: " + mac);
                    device.put("status", "off");
                }
                newListOfDevices.put(device);
            }

            listOfDevices = newListOfDevices;
            //save back to storage
            saveDevicesState();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private Notification getNotification(String title, String body) {
        Intent notificationIntent = new Intent(this, getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setAutoCancel(false)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void saveServiceState(boolean isRunning) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SERVICE_RUNNING_KEY, isRunning);
        editor.apply();
    }

    private void saveDevicesState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String devicesList = listOfDevices.toString();
        editor.putString(DEVICE_LIST_KEY, devicesList);
        editor.apply();
        if (DEBUG)
            sendNotification(getNotification("Scan Result", devicesList));
    }

    private boolean checkActivateStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean check = prefs.getBoolean(SERVICE_RUNNING_KEY, false);
        Log.d(TAG, "Activate Status " + check);
        return check;
    }

    public boolean checkPermissions() {
        int result = 0;
        for (String s : runtimeList) {
            result = ContextCompat.checkSelfPermission(this, s);
        }
        return result == 0;
    }

    private void sendNotification(Notification notification) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.notify(1234, notification);
    }

    private void connectToGATTServer(BluetoothDevice device) {
        Log.d(TAG, "FOUND SCAN TOOL DONGLE 426: " + device.getAddress() + " / " + ActivityCompat.checkSelfPermission(this, BLUETOOTH_CONNECT));
        isTesting = true;
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "CONNECTING SCAN TOOL DONGLE 426: " + device.getAddress());

            BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // successfully connected to the GATT Server
                        Log.d(TAG, "Connected to : " + device.getAddress());
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        // disconnected from the GATT Server
                        Log.d(TAG, "Disconnected from : " + device.getAddress());
                    }
                }
            };

            BluetoothGatt bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        }
    }
}