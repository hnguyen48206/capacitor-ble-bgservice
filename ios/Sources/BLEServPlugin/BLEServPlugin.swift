import Foundation
import Capacitor
import BackgroundTasks
import CoreBluetooth
import os.log
import BGTasks

@available(iOS 14.0, *)
@objc(BLEServPlugin)
public class BLEServPlugin: CAPPlugin, CAPBridgedPlugin, CBCentralManagerDelegate {
    let logger: Logger = Logger(subsystem: "com.hnguyen48206.blesrv", category: "background")
    
    public let identifier = "BLEServPlugin"
    public let jsName = "BLEServ"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startService", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopService", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = BLEServ()
    
    private var centralManager: CBCentralManager!
    private var knownDeviceId: String?
    private var detectedDevices: Set<String> = []
    private let scanPeriod: TimeInterval = 5.0
    private let delayPeriod: TimeInterval = 10.0
    private var isScanning = false
    
    override public func load() {
        super.load()
        centralManager = CBCentralManager(delegate: self, queue: nil)
        knownDeviceId = "78:02:B7:08:14:51"
        UserDefaults.standard.set(true, forKey: "serviceRunning")
        logger.log("BLEServPlugin loaded")
    }
    
    
    public func performBGTask(force: Bool, completionHandler: ((UIBackgroundFetchResult) -> Void)?) {
        let data = BGSyncRegistrationData(
            identifier: "com.hnguyen48206.blesrv",
            configuration: .init(strategy: .everyTime,
                                 requiresNetworkConnectivity: false)) { completion in
                                     //perform and call completion.
                                     self.startBleScan()
                                     DispatchQueue.main.asyncAfter(deadline: .now() + self.scanPeriod) {
                                         self.stopBleScan()
                                         self.updateDeviceStatus()
                                         self.logger.log("BLE scan completed")
                                         completion(true)
                                     }
                                 }
        BGFrameworkFactory.registrationController().registerSyncItem(data)
    }
    
    private func startBleScan() {
        print("startBleScan called")
        if !isScanning {
            detectedDevices.removeAll()
            centralManager.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
            isScanning = true
            logger.log("BLE scan started")
            
        } else {
            logger.log("BLE scan already in progress")
        }
    }
    
    @objc public func startService(_ call: CAPPluginCall) {
        logger.log("startService called")
        //        UserDefaults.standard.set(true, forKey: "serviceRunning")
        //        scheduleAppRefresh()
        call.resolve()
    }
    
    @objc public func stopService(_ call: CAPPluginCall) {
        logger.log("stopService called")
        //        taskUnregister()
        //        UserDefaults.standard.set(false, forKey: "serviceRunning")
        stopBleScan()
        call.resolve()
    }
    
  
    
    private func stopBleScan() {
        if isScanning {
            centralManager.stopScan()
            isScanning = false
            logger.log("BLE scan stopped")
        }
    }
    
    private func updateDeviceStatus() {
        if detectedDevices.contains(knownDeviceId!) {
            logger.log("Known device is online")
            sendDeviceStatus("online")
        } else {
            logger.log("Known device is offline")
            sendDeviceStatus("offline")
        }
    }
    
    private func sendDeviceStatus(_ status: String) {
        logger.log("Sending device status: \(status)")
    }
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            logger.log("Bluetooth is powered on")
            guard let call = CAPPluginCall(callbackId: "startService", options: [:], success: { _,_  in }, error: { _ in }) else { return }
            startService(call)
        } else {
            logger.log("Bluetooth is not available")
        }
    }
    
    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String: Any], rssi RSSI: NSNumber) {
        let deviceAddress = peripheral.identifier.uuidString
        logger.log("Device found: \(deviceAddress)")
        detectedDevices.insert(deviceAddress)
    }
}
