// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "BleSrv",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "BleSrv",
            targets: ["BLEServPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "BLEServPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BLEServPlugin"),
        .testTarget(
            name: "BLEServPluginTests",
            dependencies: ["BLEServPlugin"],
            path: "ios/Tests/BLEServPluginTests")
    ]
)