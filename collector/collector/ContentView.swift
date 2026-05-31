//
//  ContentView.swift
//  collector
//
//  App root: tab bar + per-tab navigation stacks + live theme injection.
//

import SwiftUI

// MARK: - Navigation values

struct CollectionRef: Hashable { let id: String }
struct ItemRef: Hashable { let collectionID: String; let itemID: String }

// MARK: - Root

struct RootView: View {
    @EnvironmentObject private var settings: AppSettings

    var body: some View {
        // `preferredColorScheme` is applied here so the child `ThemedRoot` reads
        // the resolved colorScheme and can build the matching palette.
        ThemedRoot()
            .preferredColorScheme(settings.colorScheme)
    }
}

private struct ThemedRoot: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = Palette.make(dark: scheme == .dark, accent: settings.accent)
        TabView {
            LibraryTab()
                .tabItem { Label("Library", systemImage: "square.stack.3d.up") }
            SearchTabContainer()
                .tabItem { Label("Search", systemImage: "magnifyingglass") }
            SettingsTabContainer()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
        .tint(palette.accent)
        .environment(\.palette, palette)
    }
}

// MARK: - Tab containers (each owns a NavigationStack)

private struct LibraryTab: View {
    var body: some View {
        NavigationStack {
            CollectionsHome()
                .navigationDestination(for: CollectionRef.self) { ref in
                    CollectionDetail(collectionID: ref.id)
                }
                .navigationDestination(for: ItemRef.self) { ref in
                    ItemDetail(collectionID: ref.collectionID, itemID: ref.itemID)
                }
        }
    }
}

private struct SearchTabContainer: View {
    var body: some View {
        NavigationStack {
            SearchTab()
                .navigationDestination(for: ItemRef.self) { ref in
                    ItemDetail(collectionID: ref.collectionID, itemID: ref.itemID)
                }
        }
    }
}

private struct SettingsTabContainer: View {
    var body: some View {
        NavigationStack {
            SettingsTab()
        }
    }
}

#Preview {
    RootView()
        .environmentObject(CollectorStore())
        .environmentObject(AppSettings())
}
