import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(BLEServPlugin)
public class BLEServPlugin: CAPPlugin, CAPBridgedPlugin {
    private var bleService: BLEForegroundService?
    public let identifier = "BLEServPlugin"
    public let jsName = "BLEServ"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startService", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopService", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = BLEServ()



    @objc func startService(_ call: CAPPluginCall) {
if bleService == nil {
bleService = BLEForegroundService()
}
bleService?.startService()
call.resolve()
}

@objc func stopService(_ call: CAPPluginCall) {
bleService?.stopService()
call.resolve()
}
}
