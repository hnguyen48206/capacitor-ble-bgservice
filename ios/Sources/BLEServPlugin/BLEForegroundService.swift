import CoreBluetooth
import UIKit
import BackgroundTasks

class BLEForegroundService: NSObject, CBCentralManagerDelegate {
    private var centralManager: CBCentralManager!
    private var knownDeviceId: String?
    private var detectedDevices: Set<String> = []
    private let scanPeriod: TimeInterval = 5.0
    private let delayPeriod: TimeInterval = 10.0
    //ios time in seconds
    private var isScanning = false

    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
        knownDeviceId = UserDefaults.standard.string(forKey: "mydevice")
    }

    func startService() {
        guard let knownDeviceId = knownDeviceId, !knownDeviceId.isEmpty else {
            print("No known device ID found in storage. Service will not start scanning.")
            return
        }
        UserDefaults.standard.set(true, forKey: "serviceRunning")
        // Register the background task
        taskRegister()
        // Schedule the first background task
        scheduleAppRefresh()
    }

    func stopService() {
        taskUnregister()
        UserDefaults.standard.set(false, forKey: "serviceRunning")
        stopBleScan()
    }

    func taskRegister() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.hnguyen48206.blesrv", using: nil
        ) { task in
            self.handleAppRefresh(task: task as! BGAppRefreshTask)
        }
    }
    func handleAppRefresh(task: BGAppRefreshTask) {
        scheduleAppRefresh()  // Schedule the next refresh
        task.expirationHandler = {
            // Clean up if the task expires
        }
        // Perform your background task here
        startBleScan(task)
    }
    func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "com.hnguyen48206.blesrv")
        request.earliestBeginDate = Date(timeIntervalSinceNow: delayPeriod)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Could not schedule app refresh: \(error)")
        }
    }
    func taskUnregister()
    {
         BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: "com.hnguyen48206.blesrv")
    }

    private func startBleScan(task: BGAppRefreshTask) {
        if !isScanning {
            detectedDevices.removeAll()
            centralManager.scanForPeripherals(
                withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
            isScanning = true
            print("BLE scan started")
            DispatchQueue.main.asyncAfter(deadline: .now() + scanPeriod) {
                self.stopBleScan()
                self.updateDeviceStatus()
                task.setTaskCompleted(success: true)
            }
        }
    }

    private func stopBleScan() {
        if isScanning {
            centralManager.stopScan()
            isScanning = false
            print("BLE scan stopped")
        }
    }

    private func updateDeviceStatus() {
        if detectedDevices.contains(knownDeviceId!) {
            print("Known device is online")
            sendDeviceStatus("online")
        } else {
            print("Known device is offline")
            sendDeviceStatus("offline")
        }
    }

    private func sendDeviceStatus(_ status: String) {
        // Implement your logic to send the device status to your backend server
        print("Sending device status: \(status)")
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            startService()
        } else {
            print("Bluetooth is not available")
        }
    }

    func centralManager(
        _ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any], rssi RSSI: NSNumber
    ) {
        let deviceAddress = peripheral.identifier.uuidString
        print("Device found: \(deviceAddress)")
        detectedDevices.insert(deviceAddress)
    }
}
