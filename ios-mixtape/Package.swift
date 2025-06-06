// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Mixtape",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "Mixtape",
            targets: ["Mixtape"])
    ],
    dependencies: [
        // Add package dependencies here if needed in the future
        // Example: .package(url: "https://github.com/apple/swift-algorithms", from: "1.0.0")
    ],
    targets: [
        .target(
            name: "Mixtape",
            dependencies: [
                // Add target dependencies here
            ]
        ),
        .testTarget(
            name: "MixtapeTests",
            dependencies: ["Mixtape"]
        )
    ]
) 