package com.hnguyen48206.blesrv;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.GATT_SERVER;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Arrays;
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
    private boolean isTesting = false;

    private static final String TAG = "BLEForegroundService";
    private static final String CHANNEL_ID = "BLEForegroundServiceChannel";
    private static final String PREFS_NAME = "CapacitorStorage";
    private static final String SERVICE_RUNNING_KEY = "serviceRunning";
    private static final String DEVICE_LIST_KEY = "MacBluetoothsConnected";
    private static final String BLE_CONFIG_KEY = "BLEConfigs";
    private static final String IS_MOVING_CONFIG = "Vehicle_IsMoving";

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private String devicelistStr;
    private String bleconfigsStr;
    private String isMovingStr;
    private JSONArray listOfDevices;
    private final Handler handler = new Handler();
    private long SCAN_PERIOD = 0;
    private long DELAY_PERIOD = 0;
    private long SCAN_MODE = 1111;
    private boolean IS_MOVING = false;
    private final Set<String> detectedDevices = new HashSet<>();
    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();
    List<ScanFilter> filters = new ArrayList<>();

    private ExecutorService executorService;
    private Context context;


    private BluetoothProfile mProfileProxy;
    private ScanResult autoConnectDevice;

    BluetoothAdapter classic_bluetoothAdapter;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    assert device != null;
                    String deviceAddress = device.getAddress(); // MAC address
                    detectedDevices.add(deviceAddress);
                    Log.d(TAG, "Classic BL Device found: " + deviceAddress + "/" + device.getName());
                }

            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        executorService = Executors.newSingleThreadExecutor();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        //classic Bluetooth
        classic_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        createNotificationChannel();
        permissionListFilter();

        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                Log.d(TAG, "onServiceConnected: " + profile);
                mProfileProxy = proxy;
            }

            @Override
            public void onServiceDisconnected(int profile) {
            }
        }, BluetoothProfile.HEADSET);
    }

    public void reloadSharePreferences() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        devicelistStr = prefs.getString(DEVICE_LIST_KEY, "");
//                Log.d(TAG, "devicelistStr: " + devicelistStr);
        bleconfigsStr = prefs.getString(BLE_CONFIG_KEY, "");
        isMovingStr = prefs.getString(IS_MOVING_CONFIG, "");

        if (DEBUG) {
            setDebugDefault();
        }

        convertDevicelistStrBackToObjectAndBuildFilters();
        convertBLEconfigsStrBackToObject();
        convertIsMovingStrBackToObject();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//         throw new NullPointerException();

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Service started");
                
                reloadSharePreferences();

                if (listOfDevices != null) {
                    startForeground(12345, getNotification("BLE Service", "Scanning for Devices."));
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
            devicelistStr = "[{\"mac\":\"78:02:B7:08:14:51\", \"deviceName\":\"K11\", \"vehicleID\":\"ABC\",\"status\":\"on\",\"isAutoConnect\":true}]";

        if (bleconfigsStr.isEmpty()) {
            SCAN_PERIOD = 15000;
            DELAY_PERIOD = 30000;
        }
        if(isMovingStr.isEmpty())
            IS_MOVING = true;
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

     private void convertIsMovingStrBackToObject() {
        if (isMovingStr != "")
            try {
                JSONObject jsonObject = new JSONObject(isMovingStr);
                IS_MOVING = jsonObject.getBoolean("Vehicle_IsMoving");
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }


    private void permissionListFilter()
    {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//            runtimeList = removeString(runtimeList, BLUETOOTH_ADMIN);
//            runtimeList = removeString(runtimeList, BLUETOOTH);
        }
        else
        {
            runtimeList = removeString(runtimeList, BLUETOOTH_ADMIN);
            runtimeList = removeString(runtimeList, BLUETOOTH);
        }
    }

    public static String[] removeString(String[] array, String stringToRemove) {
        return Arrays.stream(array)
                .filter(s -> !s.equals(stringToRemove))
                .toArray(String[]::new);
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

    private void startClassicScan()
    {
        Log.d(TAG, "Classic BL Scanner Status: " + classic_bluetoothAdapter.isEnabled());

        if (classic_bluetoothAdapter != null && classic_bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Classic BL Started");

                classic_bluetoothAdapter.startDiscovery();

                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                context.registerReceiver(receiver, filter);
            }
        }
    }
   private void stopClassicScan()
    {
        if (classic_bluetoothAdapter != null)
        {
            if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                classic_bluetoothAdapter.cancelDiscovery();
            }
        }
    }
    private void stopBleScan() {
        if (isScanning) {
            if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(scanCallback);
                stopClassicScan();
                isScanning = false;
                handler.removeCallbacks(scanRunnable);
                Log.d(TAG, "BLE scan stopped");
            }
        }
    }

    private void getCurrentConnectedList()
    {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            detectedDevices.clear();
            if (mProfileProxy != null) {
                List<BluetoothDevice> connectedDevices = mProfileProxy.getConnectedDevices();
                Log.d(TAG, "Current Headset List Connected Device: " + connectedDevices.toString());
                for (BluetoothDevice device : connectedDevices) {
                    String address = device.getAddress();
                    Log.d(TAG, "Current Connected Device: " + address);
                    detectedDevices.add(address);
                }
            } else {
                Log.d(TAG, "getCurrentConnectedList: null mProfileProxy");
            }
        }

//        List<BluetoothDevice> connectedDevices = mProfileProxy.getConnectedDevices();
//        Log.d(TAG, "Current Headset List Connected Device: " + connectedDevices.toString());

    }

    private final Runnable scanRunnable = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {

            if (checkActivateStatus() && checkPermissions()) {

                if (!isScanning) {
                    detectedDevices.clear();
                    reloadSharePreferences();
                    Log.d(TAG, "devicelistStr: " + devicelistStr);

                    if(IS_MOVING)
                    {
                    Log.d(TAG, "Starting BLE scan");
                    //current connected devices
                    getCurrentConnectedList();
                    //ble devices
                    bluetoothLeScanner.startScan(scanCallback);
//                    bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
                    //classic devices
                    startClassicScan();
                    isScanning = true;
                    handler.postDelayed(scanRunnable, SCAN_PERIOD);
                    }
                    else
                    Log.d(TAG, "WONT start BLE scan due to no moving status.");
                } else {
                    bluetoothLeScanner.stopScan(scanCallback);
                    stopClassicScan();
                    isScanning = false;
                    Log.d(TAG, "Stopping BLE scan");
                    updateDeviceStatus();
                    handler.postDelayed(scanRunnable, DELAY_PERIOD);
                }
            } else {
                stopBleScan();
                // stopSelf();
            }
        }
    };


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceAddress = result.getDevice().getAddress();
            if (ActivityCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "BLE Device found: " + deviceAddress + "/" + result.getDevice().getName());
            }
            detectedDevices.add(deviceAddress);

            // test connecting to device name SCANTOOL
            findDeviceToConnect(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "BLE scan failed with error code: " + errorCode);
        }
    };

    private void findDeviceToConnect(ScanResult result)
    {
        try{
            for (int i = 0; i < listOfDevices.length(); i++) {
                JSONObject device = listOfDevices.getJSONObject(i);
                String mac = device.getString("mac");
                boolean isAutoConnect = device.getBoolean("isAutoConnect");
                if (mac.equals(result.getDevice().getAddress()) && isAutoConnect) {
                    autoConnectDevice = result;
                    Log.e(TAG, "FOUND AUTO CONNECT DEVICE: ");
                    break;
                }
            }
        }
        catch(Throwable e)
        {
            Log.e(TAG, e.getMessage());
        }

    }
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

            //save back to storage
            saveDevicesState(newListOfDevices.toString());
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

    private void saveDevicesState(String newList) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DEVICE_LIST_KEY, newList);
        editor.apply();
        if (DEBUG)
            sendNotification(getNotification("Scan Result", newList));
        //auto connect device
        if(autoConnectDevice!=null)
        {
            connectToGATTServer(autoConnectDevice.getDevice());
        }
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
            Log.d(TAG, "permission: " + s + " status " + result);
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
//        isTesting = true;
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "CONNECTING SCAN TOOL DONGLE: " + device.getAddress());

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
