//
//  SettingsTab.swift
//  collector
//
//  Appearance, library stats + import/export, templates, about.
//

import SwiftUI

struct SettingsTab: View {
    @EnvironmentObject private var store: CollectorStore
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.palette) private var p

    @State private var showData = false
    @State private var dataMode: DataMode = .exporting

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                LargeHeader(title: "Settings")

                section("Appearance") {
                    card {
                        row(last: false) {
                            Text("Theme").font(.ui(16)).foregroundColor(p.text)
                            Spacer()
                            Segmented(
                                value: settings.themeMode,
                                options: ThemeMode.allCases.map { SegOption(value: $0, icon: $0.icon) },
                                onChange: { settings.themeMode = $0 }
                            )
                        }
                        row(last: false) {
                            Text("Accent").font(.ui(16)).foregroundColor(p.text)
                            Spacer()
                            HStack(spacing: 10) {
                                ForEach(Accent.allCases) { a in
                                    Button { settings.accent = a } label: {
                                        Circle().fill(a.swatch)
                                            .frame(width: 26, height: 26)
                                            .overlay(Circle().strokeBorder(.white, lineWidth: settings.accent == a ? 2 : 0))
                                            .overlay(Circle().strokeBorder(a.swatch, lineWidth: settings.accent == a ? 0 : 0))
                                            .shadow(color: .black.opacity(0.2), radius: 1, y: 1)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        row(last: true) {
                            Text("Default view").font(.ui(16)).foregroundColor(p.text)
                            Spacer()
                            Segmented(
                                value: settings.defaultView,
                                options: [SegOption(value: .list, icon: "list"), SegOption(value: .grid, icon: "grid")],
                                onChange: { settings.defaultView = $0 }
                            )
                        }
                    }
                }

                section("Data") {
                    card {
                        HStack(spacing: 0) {
                            stat("\(store.collections.count)", "Collections", first: true)
                            stat("\(store.totalItems)", "Pieces", first: false)
                            stat(money(store.totalValue), "Value", first: false)
                        }
                        .padding(.vertical, 16)
                    }
                    HStack(spacing: 10) {
                        GhostButton(title: "Import", icon: "import", expands: true) { open(.importing) }
                        GhostButton(title: "Export", icon: "export", expands: true) { open(.exporting) }
                    }
                    .padding(.top, 10)
                    HStack(spacing: 6) {
                        Icon(name: "info", size: 14).foregroundColor(p.faint)
                        Text("Everything lives in a single JSON file on this device. Back it up by exporting.")
                            .font(.ui(12.5)).foregroundColor(p.faint).lineSpacing(2)
                    }
                    .padding(.top, 10)
                    .padding(.horizontal, 2)
                }

                section("Recycle Bin") {
                    NavigationLink {
                        RecycleBinView()
                    } label: {
                        card {
                            HStack(spacing: 12) {
                                Icon(name: "trash", size: 18).foregroundColor(p.muted)
                                Text(store.trashCount == 0 ? "Empty" : "\(store.trashCount) deleted")
                                    .font(.ui(16))
                                    .foregroundColor(store.trashCount == 0 ? p.faint : p.text)
                                Spacer()
                                Icon(name: "chevron-right", size: 16).foregroundColor(p.faint)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                        }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Recycle Bin")
                }

                section("Templates") {
                    card {
                        if store.templates.isEmpty {
                            HStack {
                                Text("No templates yet.").font(.ui(15)).foregroundColor(p.faint)
                                Spacer()
                            }
                            .padding(.horizontal, 16).padding(.vertical, 14)
                        } else {
                            ForEach(Array(store.templates.enumerated()), id: \.element.id) { i, t in
                                row(last: i == store.templates.count - 1) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(t.name).font(.ui(15.5)).foregroundColor(p.text)
                                        Text("\(t.fields.count) fields").font(.mono(10.5)).foregroundColor(p.faint)
                                    }
                                    Spacer()
                                    Button { store.deleteTemplate(t.id) } label: {
                                        Icon(name: "trash", size: 17).foregroundColor(p.faint)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                }

                section("About") {
                    card {
                        row(last: false) {
                            Text("Collector's Notebook").font(.ui(16)).foregroundColor(p.text)
                            Spacer()
                            Text("iOS · 1.0").font(.mono(13)).foregroundColor(p.faint)
                        }
                        row(last: true) {
                            Text("Storage").font(.ui(16)).foregroundColor(p.text)
                            Spacer()
                            Text("On device").font(.mono(13)).foregroundColor(p.faint)
                        }
                    }
                }
            }
            .padding(.bottom, 32)
        }
        .background(p.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showData) {
            ImportExportSheet(scopeCollectionID: nil, initialMode: dataMode, onClose: { showData = false })
        }
    }

    private func open(_ mode: DataMode) {
        dataMode = mode
        showData = true
    }

    // MARK: - Builders

    private func section<Content: View>(_ label: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            MonoLabel(text: label)
            content()
        }
        .padding(.horizontal, 20)
        .padding(.top, 18)
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) { content() }
            .background(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).fill(p.surface))
            .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
            .clipShape(RoundedRectangle(cornerRadius: Radius.r, style: .continuous))
    }

    private func row<Content: View>(last: Bool, @ViewBuilder content: () -> Content) -> some View {
        HStack(spacing: 12) { content() }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .overlay(alignment: .bottom) {
                if !last { Rectangle().fill(p.line).frame(height: 0.5) }
            }
    }

    private func stat(_ value: String, _ label: String, first: Bool) -> some View {
        VStack(spacing: 3) {
            Text(value).font(.serif(21)).foregroundColor(p.text)
            MonoLabel(text: label, size: 9.5)
        }
        .frame(maxWidth: .infinity)
        .overlay(alignment: .leading) {
            if !first { Rectangle().fill(p.line).frame(width: 0.5) }
        }
    }
}
