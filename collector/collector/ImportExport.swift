//
//  ImportExport.swift
//  collector
//
//  JSON export (preview + Save to Files + Copy) and import (file / paste →
//  validate → merge or replace). Scope is whole-library or a single collection.
//

import SwiftUI
import UIKit
import UniformTypeIdentifiers

enum DataMode { case exporting, importing }

/// FileDocument wrapper used by `.fileExporter`.
struct JSONDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }
    var data: Data

    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

struct ImportExportSheet: View {
    let scopeCollectionID: String?
    let onClose: () -> Void

    @EnvironmentObject private var store: CollectorStore
    @Environment(\.palette) private var p

    @State private var mode: DataMode
    @State private var paste = ""
    @State private var parsed: ParsedImport?
    @State private var parseError = ""
    @State private var copied = false
    @State private var showExporter = false
    @State private var showImporter = false

    init(scopeCollectionID: String?, initialMode: DataMode = .exporting, onClose: @escaping () -> Void) {
        self.scopeCollectionID = scopeCollectionID
        self.onClose = onClose
        _mode = State(initialValue: initialMode)
    }

    private var scopeCollection: ItemCollection? { scopeCollectionID.flatMap { store.collection($0) } }
    private var jsonData: Data { store.exportJSONData(scopeCollectionID: scopeCollectionID) }
    private var jsonString: String { String(data: jsonData, encoding: .utf8) ?? "" }
    private var filename: String {
        if let c = scopeCollection {
            return "collector-\(c.name.lowercased().replacingOccurrences(of: " ", with: "-")).json"
        }
        return "collector-library.json"
    }

    var body: some View {
        SheetScaffold(title: scopeCollection?.name ?? "Library data", onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                if scopeCollection == nil {
                    HStack(spacing: 3) {
                        tabButton("Export", .exporting)
                        tabButton("Import", .importing)
                    }
                    .padding(3)
                    .background(Capsule().fill(p.surface2))
                    .overlay(Capsule().strokeBorder(p.line, lineWidth: 0.5))
                    .padding(.bottom, 16)
                }

                if mode == .exporting { exportBody } else { importBody }
            }
        }
        .presentationDetents([.large])
        .fileExporter(
            isPresented: $showExporter,
            document: JSONDocument(data: jsonData),
            contentType: .json,
            defaultFilename: filename
        ) { _ in }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: [.json]) { result in
            if case .success(let url) = result { loadFile(url) }
        }
    }

    // MARK: - Export

    private var exportBody: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(scopeCollection.map { "Export “\($0.name)” (\($0.items.count) items)" }
                    ?? "Export your whole library (\(store.collections.count) collections)")
                .font(.ui(14)).foregroundColor(p.muted).lineSpacing(2)
                .padding(.bottom, 12)

            ScrollView {
                Text(jsonString)
                    .font(.mono(11)).foregroundColor(p.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .textSelection(.enabled)
            }
            .frame(maxHeight: 180)
            .padding(14)
            .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface2))
            .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))

            Text("\(filename) · \(String(format: "%.1f", Double(jsonData.count) / 1024)) KB")
                .font(.mono(10.5)).foregroundColor(p.faint)
                .padding(.vertical, 8)

            PrimaryButton(title: "Save to Files", icon: "export") { showExporter = true }
                .padding(.top, 8)
            GhostButton(title: copied ? "Copied ✓" : "Copy JSON", icon: "doc", expands: true) {
                UIPasteboard.general.string = jsonString
                copied = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { copied = false }
            }
            .padding(.top, 10)
        }
    }

    // MARK: - Import

    private var importBody: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Restore from a Collector JSON file, or paste its contents below.")
                .font(.ui(14)).foregroundColor(p.muted).lineSpacing(2)
                .padding(.bottom, 14)

            GhostButton(title: "Choose JSON file…", icon: "import", expands: true) { showImporter = true }

            Text("OR PASTE")
                .font(.mono(10.5)).foregroundColor(p.faint)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)

            PlainTextField(placeholder: "{ \"app\": \"Collector\", \"collections\": [ … ] }", text: $paste, mono: true, multiline: true)
                .onChange(of: paste) { tryParse($0) }

            if !parseError.isEmpty {
                Text(parseError)
                    .font(.ui(13.5)).foregroundColor(p.danger)
                    .padding(.horizontal, 13).padding(.vertical, 11)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.danger.opacity(0.12)))
                    .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.danger.opacity(0.35), lineWidth: 0.5))
                    .padding(.top, 12)
            }

            if let parsed {
                let itemCount = parsed.collections.reduce(0) { $0 + $1.items.count }
                let trashCount = parsed.trash.count
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 8) {
                        Icon(name: "check", size: 17, weight: .bold)
                        Text("Valid file").font(.ui(15, .semibold))
                    }
                    .foregroundColor(p.accent)
                    Text("\(parsed.collections.count) collections · \(itemCount) items"
                         + (trashCount > 0 ? " · \(trashCount) in trash" : ""))
                        .font(.mono(11.5)).foregroundColor(p.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 15).padding(.vertical, 13)
                .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.accentSoft))
                .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.accentLine, lineWidth: 0.5))
                .padding(.top, 14)

                PrimaryButton(title: "Merge into library", icon: "import") {
                    store.importData(collections: parsed.collections, trash: parsed.trash, strategy: .merge)
                    onClose()
                }
                .padding(.top, 14)
                GhostButton(title: "Replace entire library", icon: "trash", danger: true, expands: true) {
                    store.importData(collections: parsed.collections, trash: parsed.trash, strategy: .replace)
                    onClose()
                }
                .padding(.top, 10)
            }
        }
    }

    // MARK: - Helpers

    private func tabButton(_ title: String, _ m: DataMode) -> some View {
        Button { mode = m } label: {
            Text(title).font(.ui(14, .semibold))
                .foregroundColor(mode == m ? p.text : p.faint)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 9)
                .background(Capsule().fill(mode == m ? p.surface : Color.clear))
        }
        .buttonStyle(.plain)
    }

    private func tryParse(_ text: String) {
        parseError = ""
        parsed = nil
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return }
        if let result = CollectorStore.parseImport(t), !result.collections.isEmpty {
            parsed = result
        } else {
            parseError = "Not a valid Collector JSON file."
        }
    }

    private func loadFile(_ url: URL) {
        let access = url.startAccessingSecurityScopedResource()
        defer { if access { url.stopAccessingSecurityScopedResource() } }
        if let text = try? String(contentsOf: url, encoding: .utf8) {
            paste = text
            tryParse(text)
        }
    }
}
