//
//  collectorApp.swift
//  collector
//
//  Created by Frank Zhan Zhiquan on 31/5/26.
//

import SwiftUI

@main
struct collectorApp: App {
    @StateObject private var store = CollectorStore()
    @StateObject private var settings = AppSettings()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(settings)
        }
    }
}
