//
//  AppSettings.swift
//  collector
//
//  User preferences: theme, accent, default item view. Persisted to UserDefaults.
//

import SwiftUI
import Combine

enum ThemeMode: String, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }

    var icon: String {
        switch self {
        case .system: return "auto"
        case .light: return "sun"
        case .dark: return "moon"
        }
    }
}

enum Accent: String, CaseIterable, Identifiable {
    case amber, indigo, forest, plum
    var id: String { rawValue }
}

enum ItemLayout: String, CaseIterable, Identifiable {
    case list, grid
    var id: String { rawValue }
    var icon: String { self == .list ? "list" : "grid" }
}

final class AppSettings: ObservableObject {
    @Published var themeMode: ThemeMode { didSet { Self.defaults.set(themeMode.rawValue, forKey: Keys.theme) } }
    @Published var accent: Accent { didSet { Self.defaults.set(accent.rawValue, forKey: Keys.accent) } }
    @Published var defaultView: ItemLayout { didSet { Self.defaults.set(defaultView.rawValue, forKey: Keys.view) } }

    private static let defaults = UserDefaults.standard
    private enum Keys {
        static let theme = "collector.themeMode"
        static let accent = "collector.accent"
        static let view = "collector.defaultView"
    }

    init() {
        let d = Self.defaults
        // First assignment in init → property observers do not fire (no redundant writes).
        themeMode = ThemeMode(rawValue: d.string(forKey: Keys.theme) ?? "") ?? .system
        accent = Accent(rawValue: d.string(forKey: Keys.accent) ?? "") ?? .amber
        defaultView = ItemLayout(rawValue: d.string(forKey: Keys.view) ?? "") ?? .list
    }

    /// `nil` lets the system decide (used with `.preferredColorScheme`).
    var colorScheme: ColorScheme? {
        switch themeMode {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}
