import Foundation
import Capacitor

@available(iOS 14.0, *)
@objc(BLEServPlugin)
public class BLEServPlugin: CAPPlugin, CAPBridgedPlugin {
 
    public let identifier = "BLEServPlugin"
    public let jsName = "BLEServ"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startService", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopService", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = BLEServ()
    
    private var centralManager: CBCentralManager!
    
    override public func load() {
        super.load()
        logger.log("BLEServPlugin loaded")
    }
    
    
    @objc public func startService(_ call: CAPPluginCall) {
        logger.log("startService called")
        NotificationCenter.default.post(name: Notification.Name("hnguyen48206_startble"), object: nil)
        call.resolve()
    }
    
    @objc public func stopService(_ call: CAPPluginCall) {
        logger.log("stopService called")
        NotificationCenter.default.post(name: Notification.Name("hnguyen48206_stopble"), object: nil)
        call.resolve()
    }
}
