import Foundation

@objc public class BLEServ: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
     @objc public func startService(_ value: Any) -> Any {
        print(value)
        return value
    }
     @objc public func stopService(_ value: Any) -> Any {
        print(value)
        return value
    }
}
