import Foundation
import Capacitor

import CoreBluetooth
import BackgroundTasks
import UIKit
import os.log
import UserNotifications
import CoreLocation

struct BLEDevice: Codable {
  let mac: String
  let deviceName: String
  let vehicleID: String
  let status: String
  let isAutoConnect: Bool
}

struct BLEConfig: Codable {
  let scan_period: Int
  let scan_delay: Int
  let isTesting: Bool
  let connect_delay: Int
}

struct VehicleIsMoving: Codable {
  let Vehicle_IsMoving: Bool
}
enum BluetoothCommand: String, CaseIterable {
  case numQueue = "NUM_QUEUE\r"
  case readAll = "READ_ALL\r"
}


@available(iOS 14.0, *)
@objc(BLEServPlugin)
public class BLEServPlugin: CAPPlugin, CAPBridgedPlugin, CBCentralManagerDelegate, CLLocationManagerDelegate {    
    public let identifier = "BLEServPlugin"
    public let jsName = "BLEServ"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startService", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopService", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = BLEServ()
    
    private let locationManager = CLLocationManager()
    var blSettingStatus: Bool = true
    var locationSettingAlwaysStatus: Bool = true
    var centralManager: CBCentralManager!
    var targetPeripheral: CBPeripheral?
    let logger: Logger = Logger(subsystem: "com.hnguyen48206.blesrv.ios", category: "background")
    var count = 0;
    private lazy var timer = BackgroundTimer(delegate: nil)
  
    var MacBluetoothsConnectedStr: String?
    var BLEConfigsStr: String?
    var Vehicle_IsMovingStr: String?
  
    var listOfSavedDevice = [BLEDevice]()
    var BLEConfigs = BLEConfig(scan_period:10000, scan_delay:50000, isTesting: true, connect_delay:900000)
    var Vehicle_IsMoving =  VehicleIsMoving(Vehicle_IsMoving: true)
    var SCAN_PERIOD: TimeInterval = 10.0
    var SCAN_DELAY: TimeInterval = 50.0
    var CONNECT_DELAY: TimeInterval = 900
    var targetDevice: CBPeripheral?
    var isFG = true
    let listOfBLEServ: [CBUUID] = [CBUUID(string: "0x180D"), CBUUID(string: "0x5533")] //HeartRate
    var listOfLatestSans = [String]()
    let df = DateFormatter()
  
    private var detectedDevices: Set<String> = []
    private var isScanning = false

    override public func load() {
        super.load()
        // centralManager = CBCentralManager(delegate: self, queue: nil)
        // knownDeviceId = "78:02:B7:08:14:51"
        logger.log("BLEServPlugin loaded")
    }
    
    
    @objc public func startService(_ call: CAPPluginCall) {
        logger.log("startService called")
        // UserDefaults.standard.set(true, forKey: "serviceRunning")
        // scheduleAppRefresh()
        call.resolve()
    }
    
    @objc public func stopService(_ call: CAPPluginCall) {
        logger.log("stopService called")
        // taskUnregister()
        // UserDefaults.standard.set(false, forKey: "serviceRunning")
        // stopBleScan()
        call.resolve()
    }
    
//     public func handleAppRefresh(task: BGAppRefreshTask) {
//         logger.log("handleAppRefresh method called")
//         task.expirationHandler = {
//             // Clean up if the task expires
//             task.setTaskCompleted(success: true)
//             self.logger.log("handleAppRefresh task expired")
//         }
//         startBleScan(task: task)
//     }
    
//     public func scheduleAppRefresh() {
//         let request = BGAppRefreshTaskRequest(identifier: "com.hnguyen48206.blesrv")
//         request.earliestBeginDate = Date(timeIntervalSinceNow: delayPeriod)
//         do {
//             try BGTaskScheduler.shared.submit(request)
//             logger.log("App refresh task scheduled")
//         } catch {
//             logger.error("Could not schedule app refresh: \(error.localizedDescription)")
//         }
//     }
    
//     func taskUnregister() {
//         BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: "com.hnguyen48206.blesrv")
//         logger.log("App refresh task unregistered")
//     }
    
    
//     private func startBleScan(task: BGAppRefreshTask) {
//         logger.log("startBleScan called")
//         if !isScanning {
//             detectedDevices.removeAll()
//             centralManager.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
//             isScanning = true
//             logger.log("BLE scan started")
// //            DispatchQueue.main.asyncAfter(deadline: .now() + scanPeriod) {
//                 self.stopBleScan()
//                 self.updateDeviceStatus()
//                 task.setTaskCompleted(success: true)
//                 self.logger.log("BLE scan completed")
//                 self.scheduleAppRefresh()
// //            }
//         } else {
//             task.setTaskCompleted(success: true)
//             logger.log("BLE scan already in progress")
//             scheduleAppRefresh()
//         }
//     }
    
//     private func stopBleScan() {
//         if isScanning {
//             centralManager.stopScan()
//             isScanning = false
//             logger.log("BLE scan stopped")
//         }
//     }
    
//     private func updateDeviceStatus() {
//         if detectedDevices.contains(knownDeviceId!) {
//             logger.log("Known device is online")
//             sendDeviceStatus("online")
//         } else {
//             logger.log("Known device is offline")
//             sendDeviceStatus("offline")
//         }
//     }
    
//     private func sendDeviceStatus(_ status: String) {
//         logger.log("Sending device status: \(status)")
//     }
    
//     public func centralManagerDidUpdateState(_ central: CBCentralManager) {
//         if central.state == .poweredOn {
//             logger.log("Bluetooth is powered on")
//             guard let call = CAPPluginCall(callbackId: "startService", options: [:], success: { _,_  in }, error: { _ in }) else { return }
//             startService(call)
//         } else {
//             logger.log("Bluetooth is not available")
//         }
//     }
    
//     public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String: Any], rssi RSSI: NSNumber) {
//         let deviceAddress = peripheral.identifier.uuidString
//         logger.log("Device found: \(deviceAddress)")
//         detectedDevices.insert(deviceAddress)
//     }
}
