import CoreBluetooth
import UIKit

class BLEForegroundService: NSObject, CBCentralManagerDelegate {
    private var centralManager: CBCentralManager!
    private var knownDeviceId: String?
    private var detectedDevices: Set<String> = []
    private let scanPeriod: TimeInterval = 5.0
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
        startBleScan()
    }
    
    func stopService() {
        UserDefaults.standard.set(false, forKey: "serviceRunning")
        stopBleScan()
    }
    
    private func startBleScan() {
        if !isScanning {
            detectedDevices.removeAll()
            centralManager.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
            isScanning = true
            print("BLE scan started")
            DispatchQueue.main.asyncAfter(deadline: .now() + scanPeriod) {
                self.stopBleScan()
                self.updateDeviceStatus()
                self.startBleScan()
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
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        let deviceAddress = peripheral.identifier.uuidString
        print("Device found: \(deviceAddress)")
        detectedDevices.insert(deviceAddress)
    }
}
