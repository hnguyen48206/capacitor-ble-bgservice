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
        
    override public func load() {
        super.load()
        print("BLEServPlugin loaded")
    }
    
    
    @objc public func startService(_ call: CAPPluginCall) {
        print("startService called")
        NotificationCenter.default.post(name: Notification.Name("hnguyen48206_startble"), object: nil)
        call.resolve(true)
    }
    
    @objc public func stopService(_ call: CAPPluginCall) {
        print("stopService called")
        NotificationCenter.default.post(name: Notification.Name("hnguyen48206_stopble"), object: nil)
        call.resolve(true)
    }
}
